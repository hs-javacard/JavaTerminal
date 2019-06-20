package terminal;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

import javax.smartcardio.CardException;
import javax.smartcardio.ResponseAPDU;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static terminal.Communication.byteArrayToHex;

public class Protocol  implements ISO7816{

    private short cardNumber;
    private short cardState;
    private short pin;
    private short nonce = 4;

    Communication comm;
    private RSAPublicKey public_key_terminal;
    private RSAPrivateKey private_key_terminal;
    private RSAPublicKey public_key_card;

    RandomData random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
    byte[] theKey = {0x2d, 0x2a, 0x2d, 0x42, 0x55, 0x49, 0x4c, 0x44, 0x41, 0x43, 0x4f, 0x44, 0x45, 0x2d, 0x2a, 0x2d};
    private AESKey sharedKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128,
            false);
    Cipher aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);

    private byte[] decryptArray;
    private byte[] aesWorkspace;
    byte[] ivdata = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public Protocol(CardThread ct){
        comm = new Communication(ct);
    }

    public void init(){
        comm.init();

        KeyPair keyPair;
        keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
        keyPair.genKeyPair();

        public_key_terminal = (RSAPublicKey) keyPair.getPublic();
        private_key_terminal = (RSAPrivateKey) keyPair.getPrivate();
    }

//    public void testt(){
//        byte[] buf = new byte[10];
//        Util.setShort(buf, (short) 0, (short) 1);// cardnr
//        Util.setShort(buf, (short) 2, (short) 2000);// balance
//        Util.setShort(buf, (short) 4, (short) 5);// pin
//        Util.setShort(buf, (short) 6, (short) 1000);// soft l
//        Util.setShort(buf, (short) 8, (short) 1500);// hard l
//
//        ResponseAPDU r = comm.sendData((byte) -1,(byte) 0,(byte) 0,(byte) 0,buf,(byte) 0);
//        int a = 0;
//
//
//        ResponseAPDU r2 = comm.sendData((byte) 4,(byte) 0,(byte) 0,(byte) 0,buf,(byte) 0);
//        int b = 0;
//
//    }

    public void authentication(APDU apdu){
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[OFFSET_INS];
        boolean authenticated = false;

        while (!authenticated)
            switch (ins) {
                case 0: // request card number
                    cardNumber = requestCardNumber(apdu);
                    break;
                case 1: // stateOfCard
                    verifyCardStatus(apdu, cardNumber);
                    break;
                case 2: // check pin
                    authenticated = checkPin(apdu);
                    break;
                default:
                    ISOException.throwIt(SW_INCORRECT_P1P2);
                    break;
        }
    }

    private void sendResponse(APDU apdu, short length) {
        apdu.setOutgoingAndSend((short) 0, length);
    }

    public void withdrawal(int payment){

        System.out.println("[TERMINAL]: Payment = " + payment);
        System.out.println("[TERMINAL]: nonce send = " + nonce);

        if(Share_Sym_Key()){
            System.out.println("-------------------- PHASE 3: T → C: (nPT ++ “Withdrawal / Payment” ++ “amount” ++ “TSpt”)symTC --------------------");

            short day_nr = 69;

            byte[] msg = new byte[6];
            Util.setShort(msg, (short) 0, nonce);
            Util.setShort(msg, (short) 2, (short) payment);
            Util.setShort(msg, (short) 4, day_nr);

            byte[] cipher3 = encrypt(theKey, msg, (short) msg.length);
            ResponseAPDU response3 = comm.sendData((byte) 2, (byte) 2, (byte) 0, (byte) 0,msg,(byte) 0);
            short received_nonce = Util.getShort(response3.getBytes(), (short)(OFFSET_CDATA));
            short status_code = Util.getShort(response3.getBytes(), (short)(OFFSET_CDATA + 2));
            System.out.println("[TERMINAL] status_code = " + status_code);
            if(check_status(status_code)){
                           /*
                        short new_balance = Util.getShort(response3.getBytes(), (short)(OFFSET_CDATA + 4));
                        System.out.println("[TERMINAL] New card balance = " + new_balance);
                        */
                //TODO: save signed message to logs
            }
        }
    }

    public void test(){
        ResponseAPDU response3 = comm.send((byte) 102, (byte) 2, (byte) 0, (byte) 0, (byte) 0);
    }

    public void deposit(int deposit){

        System.out.println("[TERMINAL]: Deposit = " + deposit);
        System.out.println("[TERMINAL]: nonce send = " + nonce);

        if(Share_Sym_Key()) {
            System.out.println("-------------------- PHASE 3: T → C: (nT ++ “Deposit” ++ “amount”)symTC --------------------");
            byte[] msg = new byte[4];
            Util.setShort(msg, (short) 0, nonce);
            Util.setShort(msg, (short) 2, (short) deposit);
            byte[] cipher3 = encrypt(theKey, msg, (short) msg.length);
            ResponseAPDU response3 = comm.sendData((byte) 3, (byte) 2, (byte) 0, (byte) 0, cipher3, (byte) 0);
                /*
                short balance_new = Util.getShort(response3.getBytes(), (short)(OFFSET_CDATA));
                System.out.print("[TERMINAL] new card balance = " + balance_new);
                */
            //TODO: Save signed message to logs
        }
    }

    public void change_soft_limit(int soft_limit){
        System.out.println("[TERMINAL] New Soft Limit = " + soft_limit);
        byte[] plain_text = new byte[4];
        Util.setShort(plain_text, (short) 0, nonce);
        Util.setShort(plain_text, (short) 2, (short) soft_limit);
        byte[] cipher = encrypt(theKey, plain_text, (short) plain_text.length);
        ResponseAPDU response = comm.sendData((byte) 1, (byte) 3, (byte) 0, (byte) cipher.length,cipher,(byte) 0);
        short status_code = Util.getShort(response.getBytes(), (short) OFFSET_CDATA);
        if(check_status(status_code)){
            System.out.print("[TERMINAL] Change Soft Limit Success!!!");
        }
        //TODO: Save signed message to logs
    }

    public void change_pin(int pin){
        System.out.println("[TERMINAL]: New PIN = " + pin);

        byte[] plain_text = new byte[6];
        plain_text[0] = (byte) (nonce >> 8);
        plain_text[1] = (byte) (nonce >> 0);
        plain_text[2] = (byte) (pin >> 24);
        plain_text[3] = (byte) (pin >> 16);
        plain_text[4] = (byte) (pin >> 8);
        plain_text[5] = (byte) (pin >> 0);

        byte[] cipher = encrypt(theKey,plain_text,(short) plain_text.length);
        ResponseAPDU response = comm.sendData((byte) 0, (byte) 2, (byte) 0, (byte) cipher.length,cipher,(byte) 0);
    }

    private boolean checkPin(APDU apdu) {
        return true;
    }

    private void requestPIN(APDU apdu) {
        pin = Util.getShort(apdu.getBuffer(), (short) 1);


    }

    private short requestCardNumber(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        cardNumber = Util.getShort(buffer, (short) 1);

        //TODO Terminal search for CN and related BAN in bank system
        // to combine it to find ID of the card, and records
        // request by the card in the logs.

        if(verifyCardStatus(apdu, cardNumber)){
            return cardNumber;
        }
        else {
            ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
        }

        return 0;
    }

    private boolean verifyCardStatus(APDU apdu, short cardNumber) {
        //TODO Terminal verifies if the card is not in
        // ‘Locked’ state (Blacklisted or Blocked)
        byte[] buffer = apdu.getBuffer();
        cardState = 0;

        if(cardNumber == 0) {
            cardState = Util.getShort(buffer, (short) 1);

            if (cardState == Short.parseShort("ready")) {
                //TODO Continue communication
                return true;
            } else {
                //TODO Abort communication, show abort message on terminal screen.
                // Terminal logs attempt of locked card.
                return false;
            }
        }
        return false;
    }

    public byte[] RSA_encrypt(RSAPublicKey public_key, byte[] plain_text){
        byte[] cipher = new byte[128];
        short length = (short) plain_text.length;
        byte[] temp_array = new byte[plain_text.length + 2];
        Util.setShort(temp_array,(short) 0, length);
        Util.arrayCopy(plain_text,(short) 0, temp_array, (short) 2, (short)plain_text.length);
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);

        rsaCipher.init(public_key, Cipher.MODE_ENCRYPT);
        rsaCipher.doFinal(temp_array, (short) 0, (short) temp_array.length, cipher, (short) 0);

        return cipher;
    }

    public byte[] RSA_decrypt(RSAPrivateKey private_key, byte[] cipher){
        byte[] plain_text = new byte[128];
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);
        rsaCipher.init(private_key, Cipher.MODE_DECRYPT);
        rsaCipher.doFinal(cipher, (short) 0, (short) cipher.length, plain_text, (short) 0);
        short length = Util.getShort(plain_text, (short) 0);
        byte[] result = new byte[length];
        Util.arrayCopy(plain_text, (short) 2, result, (short) 0 , length);
        return result;
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

        /*
        System.out.print("TERMINAL Cipher: ");
        printBytes(cipher);
        */

        Util.arrayCopy(ivdata, (short) 0, cipher, (short) (encSize + 2), (short) 16);
        Util.setShort(cipher, (short) 0, msgSize);

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

        aesCipher.init(sharedKey, Cipher.MODE_DECRYPT, ivdata, (short) 0, (short) 16);
        aesCipher.doFinal(msg, (short) 0, encSize, text, (short) 0);
        Util.arrayCopy(text, (short) 0, plain_text, (short) 0, len);
        return plain_text;
    }

    /*
    public byte[] IntListToBytes(List<Integer> list){

        int length = 0;
        for (Integer i:list) {
            length += BigInteger.valueOf(i).toByteArray().length;
        }

        byte[] result = new byte[length];

        int offset = 0;
        for (Integer i:list) {
            byte[] temp_array = BigInteger.valueOf(i).toByteArray();
            Util.arrayCopy(temp_array,(short) 0, result, (short) offset,(short) temp_array.length);
            offset += temp_array.length;
        }
        return result;
    }
    */

    //Check card number
    public boolean Verify_Card_Number(int cn){
        if(cn == 5){
            return true;
        }else{
            System.out.print("[TERMINAL] ERROR: Card Number does not match");
            return false;
        }
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

    /*Multiple protocols start with sharing the public key of the terminal and setting up the symmetric key.
    This part of the protocol is implemented in this function.*/
    public boolean Share_Sym_Key(){
        byte[] temp = new byte[255];
        //Get the exponent and modulus of the terminal key.
        short exponent_size = public_key_terminal.getExponent(temp, (short) 0);
        short modulus_size = public_key_terminal.getModulus(temp, exponent_size);

        //Copy the nonce, exponent and modulus to the buffer.
        byte[] plain_text = new byte[exponent_size + modulus_size + 6];
        Util.setShort(plain_text,(short) 0, nonce);
        Util.setShort(plain_text,(short) 2, exponent_size);
        Util.setShort(plain_text,(short) 4, modulus_size);
        Util.arrayCopy(temp, (short) 0, plain_text, (short) 6, exponent_size);
        Util.arrayCopy(temp, exponent_size, plain_text, (short) (6 + exponent_size), modulus_size);

        System.out.println("-------------------- PHASE 1: T → C: (nT ++ cn? ++ pkT) --------------------");
        //Send the public key of the terminal
        ResponseAPDU response = comm.sendData((byte) 3, (byte) 0, (byte) 0, (byte) 0,plain_text,(byte) 0);

        short response_length = Util.getShort(response.getBytes(), (short)(OFFSET_CDATA));
        byte[] res = Arrays.copyOfRange(response.getBytes(), (OFFSET_CDATA + 2),
                (OFFSET_CDATA + 2 + response_length));

        //Decrypt card response
        byte[] plain = RSA_decrypt(private_key_terminal, res);
        System.out.print("[TERMINAL]: ");
        printBytes(plain);

        //Retrieve the nonce and card number
        short card_nonce = Util.getShort(plain, (short) 0);
        short card_number = Util.getShort(plain, (short) 2);

        //Verify the nonce and card number
        if(Verify_Nonce(card_nonce) && Verify_Card_Number(card_number)){
            System.out.println("-------------------- PHASE 2: T → C: (nT ++ symkTC)pkC --------------------");

            System.out.print("[TERMINAL]: ");
            printBytes(theKey);

            byte[] msg = new byte[theKey.length + 2];
            Util.setShort(msg, (short)0, nonce);
            Util.arrayCopy(theKey,(short) 0, msg, (short) 2, (short) theKey.length);

            //TODO: change the line below this
            public_key_card = public_key_terminal;

            //Encrypt the key
            byte[] cipher2 = RSA_encrypt(public_key_card,msg);
            msg = new byte[cipher2.length + 2];
            Util.setShort(msg, (short) 0, (short) cipher2.length);
            Util.arrayCopy(cipher2, (short) 0, msg, (short) 2, (short) cipher2.length);

            //Send the key
            ResponseAPDU response2 = comm.sendData((byte) 3, (byte) 1, (byte) 0, (byte) 0,msg,(byte) 0);
            return true;
        }else{
            return false;
        }
    }

    /*Check the status code we received from the card*/
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

    public void printBytes(byte[] buffer){
        System.out.println(byteArrayToHex(buffer));
    }

}


/*TODO: for Anass
* Withdrawal
* Deposit
* Change pin
* */
