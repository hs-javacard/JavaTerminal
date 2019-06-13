package terminal;

import javacard.framework.*;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacard.security.RandomData;
import javacardx.crypto.Cipher;

import javax.smartcardio.CommandAPDU;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import static terminal.Communication.byteArrayToHex;

public class EPApplet extends Applet implements ISO7816 {

    private short balance;
    private byte[] buffer;

    RandomData random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
    byte[] theKey = {0x2d, 0x2a, 0x2d, 0x42, 0x55, 0x49, 0x4c, 0x44, 0x41, 0x43, 0x4f, 0x44, 0x45, 0x2d, 0x2a, 0x2d};
    private AESKey sharedKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128,
            false);
    Cipher aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
    byte[] ivdata = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public static void install(byte[] buffer, short offset, byte length)
            throws SystemException {
        new EPApplet();
    }

    public EPApplet() {
        balance = 10;
        register();
    }

    public void process(APDU apdu) throws ISOException {
        buffer = apdu.getBuffer();
        byte cla = buffer[OFFSET_CLA];
        byte ins = buffer[OFFSET_INS];
        byte p1 = buffer[OFFSET_P1];
        byte[] data = Arrays.copyOfRange(buffer, OFFSET_CDATA,(OFFSET_CDATA + buffer[OFFSET_LC]));

        if (selectingApplet()) { // we ignore this, it makes ins = -92
            return;
        }

        //printAPDU(apdu.getBuffer());
        int data_length = 0;
        switch (cla) {
            case 0: // increment balance
                System.out.println("Instuction 0");
            case 1: // decrement balance
//                short a = 0;
                changeBalance(cla, p1);
                break;
            case 4: // change pin
                data_length = changePIN(ins, data);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
        }


//        Util.setShort(buffer, OFFSET_CDATA, balance);
//        Util.setShort(buffer, OFFSET_LC,  (short) 2);
        //System.out.println(buffer);


        short le = -1;
        le = apdu.setOutgoing();
        if (le < 2) {
            ISOException.throwIt((short) (SW_WRONG_LENGTH | 5));
        }

        Util.setShort(buffer, (short) 1, (short) 0);
        Util.setShort(buffer, (short) 3, (short) 42);
        apdu.setOutgoingLength((short) (data_length + 5));
        apdu.sendBytes((short) 0, (short) (data_length + 5));

//        Util.setShort(buffer, (short) 1, (short) 42);

//        short responseLen = 1;
//
//        apdu.setOutgoingLength(responseLen);
//        apdu.sendBytes((short) 0, responseLen);


    }

    private void changeBalance(short ins, short amount){
        switch (ins) {
            case 0:
                // inc,
                balance += amount;
                break;
            case 1:
                balance -= amount;
                break;
            // dec
        }
    }
/*
    private void retrievePkTAndSendCardNumber(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
//        nonce = Util.getShort(buffer, OFFSET_CDATA);
        KeyHelper.init(pkTerminal, buffer, (short) (OFFSET_CDATA + 0));

        insCounter++;

        Util.arrayCopy("Henk2".getBytes(), (short) 0, buffer, (short) 0, (short) 5);
//        buffer[0] = claCounter;
//        Util.setShort(buffer, (short) 1, nonce);
//        Util.setShort(buffer, (short) 3, cardNumber);

        short whatisthis = encryptRsa(apdu, (short) 5, pkTerminal);


        sendResponse(apdu, (short) 128);
    }
*/
    private int changePIN(short ins, byte[] data){
        int result = 0;
        sharedKey.setKey(theKey, (short) 0);

        byte[] plain = decrypt(data);
        short nonce = Util.getShort(plain, (short) 0);

        byte[] msg = "PIN changed".getBytes();
        byte[] text = new byte[msg.length + 2];

        Util.setShort(text,(short) 0, nonce);
        Util.arrayCopy(msg, (short) 0, text, (short) 2, (short) msg.length);

        byte [] cipher = encrypt(theKey,text, (short) text.length);
        Util.arrayCopy(cipher, (short) 0, buffer, (short) (OFFSET_CDATA), (short) cipher.length);

        result = cipher.length;
        return result;
    }

    public void printAPDU(byte[] buffer){
        String CLA = String.format("0x%02X", buffer[OFFSET_CLA]);
        String INS = String.format("0x%02X", buffer[OFFSET_INS]);
        String P1 = String.format("0x%02X", buffer[OFFSET_P1]);
        String P2 = String.format("0x%02X", buffer[OFFSET_P2]);
        String Lc = String.format("0x%02X", buffer[OFFSET_LC]);

        System.out.println("----------- Card Simulator -----------");
        System.out.println("CLA: " + CLA + " | INS: " + INS + " | P1: " + P1 + " | P2: " + P2 + " | Lc: " + Lc
                + " | DATA: " + byteArrayToHex(Arrays.copyOfRange(buffer, OFFSET_CDATA,buffer.length)));
    }

    public static String byteArrayToHex(byte[] a) {
        String result = "[";
        for(byte b: a)
            result += String.format("0x%02X", b) + ",";
        result = result.substring(0, result.length() - 1) + "]";
        return result;
    }
/*
    public void decryptRsa(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        RSAPrivateKey pk = (RSAPrivateKey) keyPair.getPrivate();
        rsaCipher.init(pk, Cipher.MODE_DECRYPT);
        rsaCipher.doFinal(buffer, (short) OFFSET_CDATA, (short) 128, buffer1, (short) 0);
    }

    public short encryptRsa(APDU apdu, short msgSize, Key key) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, (short) 0, rsaWorkspace, (short) 0, msgSize);

        rsaCipher.init(key, Cipher.MODE_ENCRYPT);
        return rsaCipher.doFinal(rsaWorkspace, (short) 0, (short) msgSize, buffer, (short) 0);
    }

    public void encryptRsa(Key key, byte[] from, byte[] to) {
        rsaCipher.init(key, Cipher.MODE_ENCRYPT);
        rsaCipher.doFinal(from, (short) 0, (short) from.length, to, (short) 0);
    }
*/
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
        Util.setShort(cipher, (short) 0, msgSize);

        return cipher;
    }

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

    public void printBytes(byte[] buffer){
        System.out.println(byteArrayToHex(buffer));
    }
}
