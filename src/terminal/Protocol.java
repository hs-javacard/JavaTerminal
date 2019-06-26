package terminal;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Util;
import javacard.security.*;
import javacardx.crypto.Cipher;

import javax.smartcardio.ResponseAPDU;
import java.util.Arrays;
import java.util.Calendar;

import static terminal.Communication.byteArrayToHex;

public class Protocol  implements ISO7816{

    Logger log;
    Bank bank;

    private short cardNumber;
    private short nonce = 0;
    private short old_nonce = 0;

    Communication comm;
    private RSAPublicKey public_key_terminal;
    private RSAPublicKey public_key_card;

    private RandomData random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
    private byte[] theKey = {};
    private AESKey sharedKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128,
            false);
    private Cipher aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);

    private byte[] ivdata = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    Calendar cal = Calendar.getInstance();

    static private final byte INIT_CLA = (byte) 0xd0;
    static private final byte CHANGEPIN_CLA = (byte) 0xd1;
    static private final byte WITHDR_CLA = (byte) 0xd3;
    static private final byte SOFTLIM_CLA = (byte) 0xd2;
    static private final byte DEPOSIT_CLA = (byte) 0xd4;

    public Protocol(CardThread ct, Logger logger, Bank bank){
        comm = new Communication(ct);
        log = logger;
        this.bank = bank;
    }

    public void init(){
        comm.init();

        //Generate new RSA key
        KeyPair keyPair;
        keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
        keyPair.genKeyPair();

        public_key_terminal = (RSAPublicKey) keyPair.getPublic();

    }

    //Initialization protocol
    public void initialization(short balance, short soft_limit, short hard_limit){
        System.out.println("[TERMINAL]: Initialization: Balance = " + balance + ", Soft Limit = " + soft_limit +
                ", Hard Limit = " + hard_limit);

        //Generate and log a new card number and pin
        short card_number = bank.generateCardNumber();
        cardNumber = card_number;
        short pin = getNewPin();
        log.logRequest(card_number, Logger.NEWCARDNUMBER, "INIT");

        System.out.println("[TERMINAL]: Card number = " + card_number + ", pin = " + pin);

        //Place the information in the buffer
        byte[] plain_text = new byte[26];
        Util.setShort(plain_text, (short) 0, card_number);
        Util.setShort(plain_text, (short) 2, balance);
        Util.setShort(plain_text, (short) 4, pin);
        Util.setShort(plain_text, (short) 6, soft_limit);
        Util.setShort(plain_text, (short) 8, hard_limit);
        byte[] secret = new byte[16];
        random.generateData(secret, (short) 0, (short) 16);
        Util.arrayCopy(secret,(short) 0,plain_text,(short) 10,(short) 16);
        ResponseAPDU response = comm.sendData(INIT_CLA, (byte) 0, (byte) 0, (byte) 0, plain_text,(byte) 0);

        //Retrieve the exponent and modulus of the card public key
        byte claCounter = response.getBytes()[0]; // just ignore the cla since this is initialization and the environment is trusted anyways
        short exp_size = Util.getShort(response.getBytes(), (short)(1));
        short mod_size = Util.getShort(response.getBytes(), (short)(3));
        byte[] exp = new byte[exp_size];
        Util.arrayCopy(response.getBytes(), (short)(5), exp, (short) 0, exp_size);
        byte[] mod = new byte[mod_size];
        Util.arrayCopy(response.getBytes(), (short)(5 + exp_size), mod, (short) 0, mod_size);
        //Save the public key of the card
        public_key_card = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
        public_key_card.setExponent(exp, (short) 0, exp_size);
        public_key_card.setModulus(mod, (short) 0, mod_size);

        bank.setCardInfo(card_number, exp, mod, secret);

        log.logRequest(card_number, Logger.NEWCARD, "INIT");

    }

    public boolean checkPin(byte cla, short pin, byte ins) {
        byte[] plain_text = new byte[4];
        Util.setShort(plain_text, (short) 0, nonce);
        Util.setShort(plain_text, (short) 2, pin);
        byte[] cipher = encrypt(theKey, plain_text, (short) plain_text.length);
        ResponseAPDU response = comm.sendData(cla, (byte) ins, (byte) 0, (byte) 0, cipher,(byte) 0);

        byte[] plain_response = decrypt(response.getBytes());

        byte claCounter = plain_response[0];

        if(claCounter != cla) {
            System.out.println("CLA changed!");
            return false;
        }

        short card_nonce = Util.getShort(plain_response, (short) (1));
        short status_code = (short) plain_response[3];

        if(status_code != 0 || !Verify_Nonce(card_nonce)){
            if (status_code == -2) { // lock the card
                bank.updateStateCard(cardNumber, new byte[]{1});
            }
            log.logRequest(cardNumber, Logger.PINATTEMPT_F, "AUTH");
            return false;
        }

        log.logRequest(cardNumber, Logger.PINATTEMPT_S, "AUTH");
        return true;
    }

    public class WithdrawResult {
        boolean success;
        short balance;
        public WithdrawResult (boolean success, short balance){
            this.success = success;
            this.balance = balance;
        }
    }

    public WithdrawResult withdraw(){
        byte[] buffer = new byte[2];
        Util.setShort(buffer, (short) 0,  nonce); // nonce

        byte[] cipher = encrypt(theKey, buffer, (short) 2);
        ResponseAPDU response = comm.sendData(WITHDR_CLA, (byte) 4, (byte) 0, (byte) 0, cipher,(byte) 0);
        byte[] rsa_cipher = decrypt(response.getData()); // the log
        byte[] plain = RSA_decrypt(public_key_card, rsa_cipher, (short) 128);
        byte clac = plain[0];
        if(clac != WITHDR_CLA) {
            return new WithdrawResult(false, (short) 0);
        }

        short nonce_card = Util.getShort(plain, (short) 1);
        if(nonce_card != nonce) {
            return new WithdrawResult(false, (short) 0);
        }
        short theBalance = Util.getShort(plain, (short) 3);

        log.logRequestSigned(cardNumber, Logger.CONFIRMATION, String.valueOf(theBalance), "Withdrawal");
        return new WithdrawResult(true, theBalance);

    }

    //Authentication protocol
    public boolean authentication(byte cla, short pin){

        if(Share_Sym_Key(cla)){
            return checkPin(cla, pin, (byte) 2);
        } else {
            return false;
        }
    }

    //Withdrawal protocol
    public int withdrawal_checklimits(int payment){
        System.out.println("[TERMINAL]: Payment = " + payment);
        System.out.println("[TERMINAL]: nonce send = " + nonce);

        //Generate & share symmetrical key
        if(Share_Sym_Key(WITHDR_CLA)){
            System.out.println("-------------------- T → C: (nPT ++ “Withdrawal / Payment” ++ “amount” ++ “TSpt”)symTC --------------------");

            short day = (short) cal.get(Calendar.DAY_OF_YEAR);
            short year = (short) cal.get(Calendar.YEAR);
            byte[] msg = new byte[8];
            Util.setShort(msg, (short) 0, nonce);
            Util.setShort(msg, (short) 2, (short) payment);
            Util.setShort(msg, (short) 4, day);
            Util.setShort(msg, (short) 6, year);

            //Encrypt nonce + payment amount + current day number
            byte[] cipher3 = encrypt(theKey, msg, (short) msg.length);
            ResponseAPDU response3 = comm.sendData(WITHDR_CLA, (byte) 2, (byte) 0, (byte) 0,cipher3,(byte) 0);
            byte[] plainresponse = decrypt(response3.getData());
            //Get nonce and status code

            byte clac = plainresponse[0];
            short nonce = Util.getShort(plainresponse, (short) 1);
            byte statuscode = plainresponse[3];

            System.out.println("[TERMINAL] status_code = " + statuscode);
            return statuscode;

        }else{
            System.out.println("share symkey failed in withrawal");
            return 0; // error
        }
    }

    //Deposit protocol
    public void deposit(int deposit){
        System.out.println("[TERMINAL]: Deposit = " + deposit);
        System.out.println("[TERMINAL]: nonce send = " + nonce);

        log.logRequest(cardNumber, Logger.NEWREQUEST, "Deposit");

        //Generate & share the symmetrical key
        if(Share_Sym_Key(DEPOSIT_CLA)) {
            System.out.println("-------------------- T → C: (nT ++ “Deposit” ++ “amount”)symTC --------------------");
            byte[] msg = new byte[4];
            Util.setShort(msg, (short) 0, nonce);
            Util.setShort(msg, (short) 2, (short) deposit);
            //Encrypt deposit amount
            byte[] cipher3 = encrypt(theKey, msg, (short) msg.length);
            //Send data
            ResponseAPDU response3 = comm.sendData(DEPOSIT_CLA, (byte) 2, (byte) 0, (byte) 0, cipher3, (byte) 0);

            byte[] dec = decrypt(response3.getData());
            dec = RSA_decrypt(public_key_card, dec, (short) 128);
            byte claC = dec[0];
            if (claC != DEPOSIT_CLA ){
                System.out.println("CLA changed during protocol");
            }
            short nonce = Util.getShort(dec, (short) 1);
            if (nonce != this.nonce) {
                System.out.println("Nonce changed during protocol");
            }
            short newBal = Util.getShort(dec, (short) 3);

            System.out.println("[TERMINAL] New Balance = "+ newBal);

            log.logRequestSigned(cardNumber, Logger.CONFIRMATION, String.valueOf(newBal), "Deposit");

        }
    }

    //Change soft limit protocol
    public void change_soft_limit(int soft_limit){
        System.out.println("[TERMINAL] New Soft Limit = " + soft_limit);
        log.logRequest(cardNumber, Logger.NEWREQUEST, "ChangeSL");
        byte[] plain_text = new byte[4];
        Util.setShort(plain_text, (short) 0, nonce);
        Util.setShort(plain_text, (short) 2, (short) soft_limit);

        //Encrypt nonce + new soft limit
        byte[] cipher = encrypt(theKey, plain_text, (short) plain_text.length);
        //Send encrypted data
        ResponseAPDU response = comm.sendData( SOFTLIM_CLA, (byte) 3, (byte) 0, (byte) cipher.length,cipher,(byte) 0);

        // Unpack encrypted data
        byte[] afterAES = decrypt(response.getData());

        byte[] afterRSA = RSA_decrypt(public_key_card, afterAES, (short) 8);



        //Get status code
        byte status_code = afterRSA[3];

        //Check status code
        if(check_status(status_code)){
            System.out.print("[TERMINAL] Change Soft Limit Success!!!");
            log.logRequest(cardNumber, Logger.CONFIRMATION, "ChangeSL");
        } else {
            System.out.print("[TERMINAL] Change Soft Limit Failed!!!");
        }
    }

    //Change pin protocol
    public void change_pin(int pin){
        System.out.println("[TERMINAL]: New PIN = " + pin);
        log.logRequest(cardNumber, Logger.NEWREQUEST, "ChangePIN");

        byte[] plain_text = new byte[6];
        Util.setShort(plain_text, (short) 0, nonce);

        Util.setShort(plain_text, (short) 2, (short) pin);

        byte[] cipher = encrypt(theKey,plain_text,(short) plain_text.length);
        //Send nonce + pin
        ResponseAPDU response = comm.sendData(CHANGEPIN_CLA, (byte) 3, (byte) 0, (byte) cipher.length,cipher,(byte) 0);

        log.logRequest(cardNumber, Logger.PINCHANGE, "ChangePIN");
    }

    //RSA encryption
    public byte[] RSA_encrypt(Key public_key, byte[] plain_text){
        byte[] cipher = new byte[128];
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);

        rsaCipher.init(public_key, Cipher.MODE_ENCRYPT);
        rsaCipher.doFinal(plain_text, (short) 0, (short) plain_text.length, cipher, (short) 0);

        return cipher;
    }

    //RSA decryption
    public byte[] RSA_decrypt(Key private_key, byte[] cipher, short ctlength){
        byte[] plain_text = new byte[128];
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);
        rsaCipher.init(private_key, Cipher.MODE_DECRYPT);

        rsaCipher.doFinal(cipher, (short) 0, (short) 128, plain_text, (short) 0);

        return plain_text;
    }

    //Symmetrical encryption
    private byte[] encrypt(byte[] theKey, byte[] buffer, short msgSize) {

        // figure out the size in blocks
        short blocks = (short) (msgSize / 16);
        if ((msgSize % 16) > 0)
            blocks++;

        short encSize = (short) (blocks * 16);
        short paddingSize = (short) (encSize - msgSize);


        byte[] msg = new byte[encSize+18];
        byte[] cipher = new byte[encSize + 18];

        Util.arrayCopy(buffer, (short) 0, msg, (short) 0, msgSize);
        Util.arrayFillNonAtomic(msg, msgSize, paddingSize, (byte) 3);

        // generate IV
        random.generateData(ivdata, (short) 0, (short) 16);

        sharedKey.setKey(theKey, (short) 0);

        aesCipher.init(sharedKey, Cipher.MODE_ENCRYPT, ivdata, (short) 0, (short) 16);
        aesCipher.doFinal(msg, (short) 0, encSize, cipher, (short) 2);

        Util.arrayCopy(ivdata, (short) 0, cipher, (short) (encSize + 2), (short) 16);
        Util.setShort(cipher, (short) 0, encSize); //msgSize);

        return cipher;
    }

    //Symmetrical decryption
    private byte[] decrypt(byte[] buffer){
        short len = Util.getShort(buffer, (short) 0);
        byte[] plain_text = new byte[len];
        short blocks = (short) (len / 16);
        if((len % 16) > 0){
            blocks++;
        }

        short encSize = (short) (blocks * 16);
        byte[] msg = new byte[encSize];
        Util.arrayCopy(buffer, (short) 2, msg, (short) 0, encSize);
        Util.arrayCopy(buffer, (short) (encSize + 2), ivdata, (short) 0, (short) 16);

        byte[] text = new byte[encSize];

        sharedKey.setKey(theKey, (short) 0);
        aesCipher.init(sharedKey, Cipher.MODE_DECRYPT, ivdata, (short) 0, (short) 16);
        aesCipher.doFinal(msg, (short) 0, encSize, text, (short) 0);
        Util.arrayCopy(text, (short) 0, plain_text, (short) 0, len);
        return plain_text;
    }

    //Check nonce
    public boolean Verify_Nonce(int n){
        if(n == nonce){
            return true;
        }else{
            System.out.print("[TERMINAL] ERROR: Nonce does not match");
            return false;
        }
    }

    /* Multiple protocols start with sharing the public key of the terminal and setting up the symmetric key.
    This part of the protocol is implemented in this function.
    The function also generates the nonce used during the protocol, as well as the symmetric key that will be used */
    public boolean Share_Sym_Key(byte CLA){
        nonce = generateNonce();
        theKey = generateSymKey();

        ResponseAPDU response = comm.sendData(CLA, (byte) 0, (byte) 0, (byte) 0, new byte[] {}, (byte) 0);

        short card_number = Util.getShort(response.getData(), (short) 128);
        cardNumber = card_number;
        // Get the public key of the card
        byte[] exp = bank.getCardInfo(card_number).get(0); // exp
        byte[] mod = bank.getCardInfo(card_number).get(1); // mod

        log.logRequest(card_number, Logger.NEWREQUEST, "AUTH");

        public_key_card = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
        public_key_card.setExponent(exp,(short) 0, (short) exp.length);
        public_key_card.setModulus(mod,(short)0, (short) mod.length);



        byte[] plain_cn = RSA_decrypt(public_key_card, response.getData(), (short) 128);
        if (plain_cn[0] != CLA) {
            System.out.println("Wrong CLA in share_sym_key");
            return false;
        }
        if (Util.getShort(plain_cn, (short) 1) != card_number){
            System.out.println("Wrong cardnumber in sign does not match");
            return false;
        }

        byte[] status = bank.getCardInfo(card_number).get(2);
        if (status[0] == 1) {
            System.out.println("Card is locked");
            log.logRequest(card_number, Logger.LOCKEDATTEMPT, "AUTH");
            return false;
        }
        cardNumber = card_number;



        System.out.println("--------------------  T → C: (nT ++ symTC ++ secC [missing! ++ pkT] )pkC --------------------");
        byte[] temp = new byte[255];
        //Get the exponent and modulus of the terminal key.
        short exponent_size = public_key_terminal.getExponent(temp, (short) 0);
        short modulus_size = (short) (public_key_terminal.getModulus(temp, exponent_size) -1);

        //Copy the nonce, exponent and modulus to the buffer.
        byte[] plain_text = new byte[16+16+2];
        Util.setShort(plain_text,(short) 0, nonce);

        Util.arrayCopy(theKey, (short) 0, plain_text,(short) 2, (short) 16);


        byte[] secret = bank.getCardInfo(card_number).get(3);
        Util.arrayCopy(secret, (short) 0, plain_text, (short) 18, (short) 16);

        byte[] cipher = RSA_encrypt(public_key_card, plain_text);

        //Send the public key of the terminal
        response = comm.sendData(CLA, (byte) 1, (byte) 0, (byte) 0, cipher,(byte) 0);


        //Decrypt card response
        byte[] plain = decrypt(response.getData());

        System.out.print("[TERMINAL]: ");
        printBytes(plain);

        //Retrieve the nonce and card number
        byte cla_counter = plain[0];
        short nonce = Util.getShort(plain, (short) 1);
        short status_code = plain[3]; // does secret match
        if (cla_counter != CLA) {
            System.out.println("CLA changed during protocol");
            return false;
        }
        if (nonce != this.nonce){
            System.out.println("Nonce changed during protocol");
            return false;
        }
        if (status_code != 1){ // Check secret
            System.out.println("Supplied secret does not match secret on card");
            return false;
        }


        return true;
    }

    /* Check the status code we received from the card for withdrawal*/
    public boolean check_status(short status){
        boolean result = false;
        switch (status){
            case 1:
                 result = true;
                break;
            case -1:
                System.out.println("[TERMINAL] ERROR: Not enough balance on the card for the transaction");
                break;
            case -2:
                System.out.println("[TERMINAL] ERROR: The payment exceeds the soft limit");
                break;
            case -3:
                System.out.println("[TERMINAL] ERROR: The daily limit has been exceeded");
                break;
            default:
                System.out.println("[TERMINAL] ERROR: unknown status code");
                break;
        }
        return  result;
    }


    public short getNewPin(){
        byte[] d = new byte[2];
        random.generateData(d, (short) 0, (short) 2);
        return Util.getShort(d, (short) 0);
    }
    public void printBytes(byte[] buffer){
        System.out.println(byteArrayToHex(buffer));
    }

    public short generateNonce(){
        old_nonce = nonce;
        short new_nonce = old_nonce;
        byte[] buff = new byte[2];
        random.generateData(buff, (short) 0, (short) buff.length);
        while(old_nonce == new_nonce){
            random.generateData(buff, (short) 0, (short) buff.length);
            new_nonce = Util.getShort(buff, (short) 0);
        }
        return new_nonce;
    }

    public byte[] generateSymKey(){
        byte[] key = new byte[16];
        random.generateData(key, (short) 0, (short) 16);
        return key;
    }

}


