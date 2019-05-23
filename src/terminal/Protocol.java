package terminal;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

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

            int n1 = 50;
            int n2 = 250;
            int n3 = 5;
            System.out.println("nonce[int]: " + n1);
            System.out.println("public key[int]: " + n2);
            System.out.println("amount[int]: " + n3);

            byte[] nonce = BigInteger.valueOf(n1).toByteArray();
            byte[] pub = BigInteger.valueOf(n2).toByteArray();
            byte[] amount = BigInteger.valueOf(n3).toByteArray();
            System.out.println("nonce[byte]:" + Arrays.toString(nonce));
            System.out.println("public key[byte]:" + Arrays.toString(pub));
            System.out.println("amount[byte]:" + Arrays.toString(amount));

            byte[] msg = new byte[100];
            Util.setShort(msg,(short) 0,(short)nonce.length);
            Util.setShort(msg,(short) 2,(short)pub.length);
            Util.setShort(msg,(short) 4,(short)amount.length);

            Util.arrayCopy(nonce,(short)0 ,msg,(short) 6, (short) nonce.length);
            Util.arrayCopy(pub,(short)0 ,msg,(short) (6 + nonce.length), (short) pub.length);
            Util.arrayCopy(amount,(short)0 ,msg,(short) (6 + nonce.length + pub.length), (short) amount.length);
            System.out.println("msg[byte]:" + Arrays.toString(msg));
            byte[] encryptedMsg = publicKeyEncrypt(pb,msg);
            System.out.println("enc[byte]:" + Arrays.toString(encryptedMsg));
            byte[] decryptedMsg = privateKeyDecrypt(pk, encryptedMsg);
            System.out.println("enc[byte]:" + Arrays.toString(encryptedMsg));

            short nonce_size =(short)(((decryptedMsg[0] & 0xFF) << 8) | (decryptedMsg[1] & 0xFF));
            short pub_size =(short)(((decryptedMsg[2] & 0xFF) << 8) | (decryptedMsg[3] & 0xFF));
            short amount_size =(short)(((decryptedMsg[4] & 0xFF) << 8) | (decryptedMsg[5] & 0xFF));

            byte[] nonce_dec = new byte[nonce_size];
            byte[] pub_dec = new byte[pub_size];
            byte[] amount_dec = new byte[amount_size];

            Util.arrayCopy(decryptedMsg,(short)6 ,nonce_dec,(short) 0, nonce_size);
            Util.arrayCopy(decryptedMsg,(short)(6 + nonce_size) ,pub_dec,(short) 0, pub_size);
            Util.arrayCopy(decryptedMsg,(short)(6 + nonce_size + pub_size) ,amount_dec,(short) 0, amount_size);
            System.out.println("nonce_dec[byte]:" + Arrays.toString(nonce_dec));
            System.out.println("public key_dec[byte]:" + Arrays.toString(pub_dec));
            System.out.println("amount_dec[byte]:" + Arrays.toString(amount_dec));

            int p1 = new BigInteger(nonce_dec).intValue();
            int p2 = new BigInteger(pub_dec).intValue();
            int p3 = new BigInteger(amount_dec).intValue();

            System.out.println("nonce_dec[int]: " + p1);
            System.out.println("public key_dec[int]: " + p2);
            System.out.println("amount_dec[int]: " + p3);

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

    public byte[] publicKeyEncrypt(RSAPublicKey public_key, byte[] plain_text){
        byte[] cipher = new byte[128];
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);
        rsaCipher.init(public_key, Cipher.MODE_ENCRYPT);
        rsaCipher.doFinal(plain_text, (short) 0, (short) plain_text.length, cipher, (short) 0);
        return cipher;
    }

    public byte[] privateKeyDecrypt(RSAPrivateKey private_key, byte[] cipher){
        byte[] plain_text = new byte[128];
        Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1,false);
        rsaCipher.init(private_key, Cipher.MODE_DECRYPT);
        rsaCipher.doFinal(cipher, (short) 0, (short) cipher.length, plain_text, (short) 0);
        return plain_text;
    }

}