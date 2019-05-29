package terminal;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Protocol  implements ISO7816{

    private short cardNumber;
    private short cardState;
    private short pin;
    Communication comm;

    public void Protocol(){
        this.comm = new Communication();
    }

    public void init(){

    }

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

    public void withdrawal(){
        //TODO
    }

    public void deposit(){
        //TODO
    }

    public void change_soft_limit(/*APDU apdu, int nonce, int soft_limit*/){
        //TODO: 1: (nonceRT + publickeyRT + amount)publickeyCard

        try {
            KeyPair keyPair;
            keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
            keyPair.genKeyPair();
            RSAPublicKey pb = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey pk = (RSAPrivateKey) keyPair.getPrivate();

            List<Integer> data = new ArrayList<>(Arrays.asList(20, 40, 60));
            for(int i = 0; i < data.size(); i++){
                System.out.print("Data[" + i + "]: " + data.get(i) + '\t');
                if(i == data.size() - 1){
                    System.out.println(" ");
                }
            }

            byte[] test = EncryptMultiData(pb,data);
            List<Integer> data_2 = DecryptMultiData(pk, test);

            for(int i = 0; i < data_2.size(); i++){
                System.out.print("Data[" + i + "]: " + data_2.get(i) + '\t');
                if(i == data_2.size() - 1){
                    System.out.println(" ");
                }
            }

        } catch (CryptoException e) {
            short reason = e.getReason();
            ISOException.throwIt(reason);
        }





        int ins = 0;
        int pk_card = 5;

        //RSAPublicKey cardPub = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, true);
        switch (ins){
            case 0:

                break;
            default:
                ISOException.throwIt(SW_INCORRECT_P1P2);
                break;
        }


    }

    public void change_pin(){
        //TODO
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

    /*Encryption of a byte array*/
    public byte[] publicKeyEncrypt(RSAPublicKey public_key, byte[] plain_text){
        byte[] cipher = new byte[128];
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);
        rsaCipher.init(public_key, Cipher.MODE_ENCRYPT);
        rsaCipher.doFinal(plain_text, (short) 0, (short) plain_text.length, cipher, (short) 0);
        return cipher;
    }

    /*Decryption of a byte array*/
    public byte[] privateKeyDecrypt(RSAPrivateKey private_key, byte[] cipher){
        byte[] plain_text = new byte[128];
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);
        rsaCipher.init(private_key, Cipher.MODE_DECRYPT);
        rsaCipher.doFinal(cipher, (short) 0, (short) cipher.length, plain_text, (short) 0);
        return plain_text;
    }

    /*Encryption of a list of integers*/
    public byte[] EncryptMultiData(RSAPublicKey public_key, List<Integer> data){
        //List<Integer> data = new ArrayList<>();
        byte[] msg = new byte[100];
        int msg_offset = 0;
        for (int i: data){
            Util.setShort(msg,(short) msg_offset,(short)(BigInteger.valueOf(i).toByteArray().length));
            msg_offset += 2;
        }

        for (int i: data){
            Util.arrayCopy(BigInteger.valueOf(i).toByteArray(),(short)0 ,msg,(short) msg_offset,
                    (short)(BigInteger.valueOf(i).toByteArray().length));
            msg_offset += (BigInteger.valueOf(i).toByteArray().length);
        }

        byte[] encryptedMsg = publicKeyEncrypt(public_key,msg);
        return encryptedMsg;
    }

    /*Decryption of a list of integers*/
    public List<Integer> DecryptMultiData(RSAPrivateKey private_key, byte[] cipher){

        byte[] decryptedMsg = privateKeyDecrypt(private_key, cipher);

        short data1_array_length =(short)(((decryptedMsg[0] & 0xFF) << 8) | (decryptedMsg[1] & 0xFF));
        short data2_array_length =(short)(((decryptedMsg[2] & 0xFF) << 8) | (decryptedMsg[3] & 0xFF));
        short data3_array_length =(short)(((decryptedMsg[4] & 0xFF) << 8) | (decryptedMsg[5] & 0xFF));

        byte[] data1_array = new byte[data1_array_length];
        byte[] data2_array = new byte[data2_array_length];
        byte[] data3_array = new byte[data3_array_length];

        Util.arrayCopy(decryptedMsg,(short)6 ,data1_array,(short) 0, data1_array_length);
        Util.arrayCopy(decryptedMsg,(short)(6 + data1_array_length) ,data2_array,(short) 0, data2_array_length);
        Util.arrayCopy(decryptedMsg,(short)(6 + data1_array_length + data2_array_length) ,data3_array,(short) 0, data3_array_length);


        int p1 = new BigInteger(data1_array).intValue();
        int p2 = new BigInteger(data2_array).intValue();
        int p3 = new BigInteger(data3_array).intValue();

        return new ArrayList<>(Arrays.asList(p1,p2,p3));
    }
}


/*TODO: for Anass
* Withdrawal
* Deposit
* Change pin
* */
