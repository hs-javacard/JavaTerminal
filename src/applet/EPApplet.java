package applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

import java.util.Arrays;

import static terminal.Communication.byteArrayToHex;

public class EPApplet extends Applet implements ISO7816 {

    private static final byte PIN_TRY_LIMIT = 3;
    private static final byte MAX_PIN_SIZE = 4;

    private RSAPublicKey pkTerminal;
    private KeyPair keyPair;
    private AESKey sharedKey;

    private Cipher aesCipher;
    private Cipher rsaCipher;

    private byte[] buffer1;

    private byte[] rsaWorkspace;
    private byte[] aesWorkspace;

    private RandomData random;

    private byte[] theKey = {0x2d, 0x2a, 0x2d, 0x42, 0x55, 0x49, 0x4c, 0x44, 0x41, 0x43, 0x4f, 0x44, 0x45, 0x2d, 0x2a, 0x2d};
    private byte[] ivdata;

    private OwnerPIN pin;

    private short cardNumber;

    private short balance;
    private short paymentAmount;

    private short dayNumber;
    private short totalToday;

    private short softLimit;
    private short hardLimit;

    private short nonce;

    // We assume card is ejected after completed interaction and these values get reset
    private byte claCounter;
    private byte insCounter;

    public static void install(byte[] buffer, short offset, byte length)
            throws SystemException {
        new EPApplet();
    }

    public EPApplet() {

        try {
            keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
            keyPair.genKeyPair();
        } catch (CryptoException e) {
            short reason = e.getReason();
            ISOException.throwIt(reason);
        }

        aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
        pkTerminal = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, true);

        buffer1 = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);

        rsaWorkspace = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
        aesWorkspace = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_DESELECT);

        ivdata = JCSystem.makeTransientByteArray((short) 16, JCSystem.CLEAR_ON_DESELECT);
//        theKey = JCSystem.makeTransientByteArray((short) 16, JCSystem.CLEAR_ON_DESELECT);
//        theKey = {0x2d, 0x2a, 0x2d, 0x42, 0x55, 0x49, 0x4c, 0x44, 0x41, 0x43, 0x4f, 0x44, 0x45, 0x2d, 0x2a, 0x2d};

        random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

        sharedKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);

        cardNumber = 4;
        balance = 4000;
        softLimit = 2000;
        hardLimit = 10000;
        totalToday = 0;
        register();
    }

    @Override
    public boolean select() {
        resetCounters();
        return true;
    }

    public void process(APDU apdu) throws ISOException {
        byte[] buffer = apdu.getBuffer();
        byte cla = buffer[OFFSET_CLA];
        byte ins = buffer[OFFSET_INS];

        if (selectingApplet()) // we ignore this, it makes ins = -92
            return;

        /*
        if (validCounters(cla, ins)) {
            claCounter = cla;
        } else {
            ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
            return;
        }*/

//        if (ins == 0)
//            initTerminalKey(apdu);

        switch (cla) {
            case -1:
                initialize(apdu);
                break;
            case 0:
                changePinMain(apdu);
                break;
            case 1: // Change soft limit
                changeSoftLimitMain(apdu);
                break;
            case 2: // Payment, so balance decrease
                payment(apdu);
                break;
            case 3: // Deposit, so balance increase
                deposit(apdu);
                break;
            case 101:
                Util.arrayCopy("Henk!".getBytes(), (short) 0, buffer, (short) 0, (short) 5);

                short encSize = encryptAes(apdu, (short) 5);
                sendResponse(apdu, encSize);
                break;
            default:
                ISOException.throwIt(SW_CLA_NOT_SUPPORTED);
                break;
        }
    }

    //<editor-fold desc="Initialize">

    private void initialize(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[OFFSET_INS];

        switch (ins) {
            case 0:
                setInitData(apdu);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
                break;
        }

    }

    private void setInitData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte offset = OFFSET_CDATA;

        cardNumber = Util.getShort(buffer, (short) offset);
        balance = Util.getShort(buffer, (short) (offset + 2));
        softLimit = Util.getShort(buffer, (short) (offset + 6));
        hardLimit = Util.getShort(buffer, (short) (offset + 8));

        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);
        pin.update(buffer, (short) (offset + 4), (byte) 2);

        resetCounters();
        sendPublicKey(apdu);
    }

    private void sendPublicKey(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        RSAPublicKey pb = (RSAPublicKey) keyPair.getPublic();
        short expSize = pb.getExponent(buffer, (short) 4);
        short modSize = pb.getModulus(buffer, (short) (expSize + 4));

        buffer[0] = claCounter;
        Util.setShort(buffer, (short) 1, expSize);
        Util.setShort(buffer, (short) 3, modSize);
        sendResponse(apdu, (short) (1 + expSize + modSize + 4));
    }

    //</editor-fold>

    //<editor-fold desc="Change PIN">

    private void changePinMain(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[OFFSET_INS];

        switch (ins) {
            case 0: // respond with card number
                retrievePkTAndSendCardNumber(apdu);
                break;
            case 1: // check pin
                checkPin(apdu);
                break;
            case 2:
                changePin(apdu);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void changePin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        //pin.update(buffer, OFFSET_CDATA, (byte) 2);
        resetCounters();
        System.out.print("[CARD]: Pin change successfull[but not really :( ]");
        buffer[1] = 1; // set status code
        sendResponse(apdu, (short) 2);
    }

    //</editor-fold>

    //<editor-fold desc="Change Soft limit">

    private void changeSoftLimitMain(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[OFFSET_INS];

        switch (ins) {
            case 0: // respond with card number
                retrievePkTAndSendCardNumber(apdu);
                break;
            case 1:
                retrieveSymmetricKey(apdu);
                break;
            case 2: // check pin
                checkPin(apdu);
                break;
            case 3:
                changeSoftLimit(apdu);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void changeSoftLimit(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte statusCode;

        sharedKey.setKey(theKey,(short) 0);
        decryptAes(apdu);


        short newNonce = Util.getShort(buffer, (short)(OFFSET_CDATA + 2));
        short newSoftLimit = Util.getShort(buffer, (short)(OFFSET_CDATA + 4));

        if (newSoftLimit > hardLimit) {
            statusCode = -1;
        } else {
            statusCode = 1;
            softLimit = newSoftLimit;
            System.out.println("[CARD] New Soft Limit = " + newSoftLimit);
        }
        buffer[1] = statusCode; // set status code
        Util.setShort(buffer, (short) OFFSET_CDATA, softLimit);
        sendResponse(apdu, (short) 7);
    }

    //</editor-fold>

    //<editor-fold desc="Payment">

    private void payment(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[OFFSET_INS];

        switch (ins) {
            case 0: // respond with card number
                retrievePkTAndSendCardNumber(apdu);
                break;
            case 1:
                retrieveSymmetricKey(apdu);
                break;
            case 2: // check soft limit
                checkSoftLimit(apdu);
                break;
            case 3: // check pin
                checkPin(apdu);
                break;
            case 4: // decrease balance
                decreaseBalance(apdu);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void checkSoftLimit(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        retrieveNonce(buffer);

        paymentAmount = Util.getShort(buffer, (short)(OFFSET_CDATA + 2));
        short dayNumber = Util.getShort(buffer, (short) (OFFSET_CDATA + 4));
        if (this.dayNumber != dayNumber)
            totalToday = 0;

        byte statusCode;

        if (balance < paymentAmount) {
            statusCode = -1;
            resetCounters();

        } else if (paymentAmount > softLimit) {
            statusCode = -2;
            insCounter++;

        } else if (paymentAmount + totalToday > hardLimit) {
            statusCode = -3;
            resetCounters();

        } else {
            totalToday += paymentAmount;
            balance -= paymentAmount;
            System.out.println("[CARD] Paid amount = " + paymentAmount);
            System.out.println("[CARD] Total amount today = " + totalToday);
            System.out.println("[CARD] New balance = " + balance);

            statusCode = 1;
            insCounter += 2;
        }

        Util.arrayFillNonAtomic(buffer, (short) OFFSET_CDATA, (short) (buffer.length - 5),(byte) 0);
        Util.setShort(buffer, (short) OFFSET_CDATA, nonce);
        Util.setShort(buffer, (short) (OFFSET_CDATA + 2), (short) statusCode);
        if(statusCode == 1){
            Util.setShort(buffer, (short) (OFFSET_CDATA + 4), balance);
        }
        sendResponse(apdu, (short) 11);
    }

    private void decreaseBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        retrieveNonce(buffer);
        paymentAmount = Util.getShort(buffer, (short) (OFFSET_CDATA + 2));
        balance -= paymentAmount;

        System.out.println("[Card]: Payment amount = " + paymentAmount);

        resetCounters();

        Util.setShort(buffer, (short) 1, balance);
        sendResponse(apdu, (short) 3);
    }

    //</editor-fold>

    //<editor-fold desc="Deposit">

    private void deposit(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[OFFSET_INS];

        switch (ins) {
            case 0: // respond with card number
                retrievePkTAndSendCardNumber(apdu);
                break;
            case 1:
                retrieveSymmetricKey(apdu);
                break;
            case 2: // increase balance
                increaseBalance(apdu);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void increaseBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        sharedKey.setKey(theKey, (short) 0);
        decryptAes(apdu);

        short newNonce = Util.getShort(buffer, (short)(OFFSET_CDATA + 2));
        short amount = Util.getShort(buffer, (short)(OFFSET_CDATA + 4));
        System.out.println("[CARD]: Nonce received = " + newNonce);
        System.out.println("[CARD]: Balance increase = " + amount);
        balance += amount;

        Util.arrayFillNonAtomic(buffer, (short) OFFSET_CDATA, (short) (buffer.length - 5) ,(byte) 0);
        Util.setShort(buffer, (short) OFFSET_CDATA, balance);

        resetCounters();

        Util.setShort(buffer, (short) 1, balance);
        sendResponse(apdu, (short) 7);
    }

    //</editor-fold>

    private void retrievePkTAndSendCardNumber(APDU apdu) {
        cardNumber = (short) 5;
        byte[] buffer = apdu.getBuffer();
        //nonce = Util.getShort(buffer, OFFSET_CDATA);
        retrieveNonce(buffer);

        System.out.println("[CARD]: nonce received = " + nonce);
        KeyHelper.init(pkTerminal, buffer, (short) (OFFSET_CDATA + 2));

        insCounter++;

        byte[] temp_buffer = new byte[6];
        Util.setShort(temp_buffer, (short) 0, (short) 4);
        Util.setShort(temp_buffer, (short) 2, nonce);
        Util.setShort(temp_buffer, (short) 4, cardNumber);

        System.out.print("[CARD]: ");
        printBytes(Arrays.copyOfRange(temp_buffer, 2, temp_buffer.length));

        byte[] cipher = new byte[128];
        encryptRsa(pkTerminal, temp_buffer, cipher);
        Util.arrayFillNonAtomic(buffer, (short) OFFSET_CDATA, (short) (buffer.length - 5),(byte) 0);

        Util.setShort(buffer,(short) OFFSET_CDATA,(short) cipher.length);
        Util.arrayCopy(cipher, (short) 0, buffer, (short) (OFFSET_CDATA + 2), (short) cipher.length);

        //printBytes(cipher);

        sendResponse(apdu, (short) 135);
    }

    private void retrieveSymmetricKey(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        //DECRYPT HERE
        short cipher_length = Util.getShort(buffer, (short) OFFSET_CDATA);
        byte[] cipher = new byte[cipher_length];
        Util.arrayCopy(buffer, (short)(OFFSET_CDATA + 2), cipher, (short) 0, cipher_length);

        //decryptRsa(cipher);

        /*
        short plain_text_length = Util.getShort(buffer1, (short) 0);
        byte[] plain_text = new byte[plain_text_length];
        Util.arrayCopy(buffer1, (short) 2, plain_text, (short)0,plain_text_length);

        retrieveNonce(plain_text);

        //SET SYSTEM KEY
        Util.arrayCopy(plain_text, (short) 2, theKey, (short) 0, (short) (plain_text.length - 2));
        */

        System.out.println("[CARD]: nonce received = " + nonce);
        System.out.print("[CARD]: ");
        printBytes(theKey);

        buffer[0] = claCounter;
        Util.setShort(buffer, (short) 1, nonce);

        //ENCRYPT HERE
        sendResponse(apdu, (short) 3);
    }

    private void checkPin(APDU apdu) {
        //DECRYPT AES HERE
        byte[] buffer = apdu.getBuffer();
        retrieveNonce(buffer);

        //DECRYPT RSA HERE
        boolean correctPin = pin.check(buffer, (short) (OFFSET_CDATA + 2), (byte) 2);
        byte statusCode;

        if (correctPin) {
            insCounter++;
            pin.reset();

            statusCode = 1;
        } else {
            statusCode = -1;
        }

        buffer[0] = claCounter;
        buffer[1] = statusCode;

        //ENCRYPT HERE
        sendResponse(apdu, (short) 2);
    }

    private void sendResponse(APDU apdu, short length) {
        apdu.setOutgoingAndSend((short) 0, length);
    }

    private boolean validCounters(byte cla, byte ins) {
        if (claCounter != -1 && claCounter != cla)
            return false;

        if (insCounter + 1 != ins)
            return false;

        return true;
    }

    private void resetCounters() {
        claCounter = -1;
        insCounter = -1;
    }

    private void retrieveNonce(byte[] buffer) {
        nonce = Util.getShort(buffer, OFFSET_CDATA);
    }

    private void initTerminalKey(APDU apdu) {
        KeyHelper.init(pkTerminal, buffer1, (short) 0);
    }

    //<editor-fold desc="RSA">

    public void decryptRsa(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        RSAPrivateKey pk = (RSAPrivateKey) keyPair.getPrivate();
        rsaCipher.init(pk, Cipher.MODE_DECRYPT);
        rsaCipher.doFinal(buffer, (short) OFFSET_CDATA, (short) 128, buffer1, (short) 0);
    }

    public void decryptRsa(byte[] buffer) {
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

    //</editor-fold>

    //<editor-fold desc="AES">

    private short encryptAes(APDU apdu, short msgSize) {
        byte[] buffer = apdu.getBuffer();

        Util.arrayCopy(buffer, (short) 0, aesWorkspace, (short) 0, msgSize);

        short blocks = getBlockSize(msgSize);
        short encSize = (short) (blocks * 16);
        short paddingSize = (short) (encSize - msgSize);

        Util.arrayFillNonAtomic(aesWorkspace, msgSize, paddingSize, (byte) 3);

        // generate IV
        random.generateData(ivdata, (short) 0, (short) 16);

        sharedKey.setKey(theKey, (short) 0);

        aesCipher.init(sharedKey, Cipher.MODE_ENCRYPT, ivdata, (short) 0, (short) 16);
        aesCipher.doFinal(aesWorkspace, (short) 0, encSize, buffer, (short) 2);

        short offset = 16 + 2;

        Util.arrayCopy(ivdata, (short) 0, buffer, offset, (short) 16);
        Util.setShort(buffer, (short) 0, msgSize);

        return (short) (encSize + 2 + 16);
    }

    private void decryptAes(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        short msgSize = Util.getShort(buffer, (short) (OFFSET_CDATA));
        short blocks = getBlockSize(msgSize);
        short encSize = (short) (blocks * 16);

        Util.arrayCopy(buffer, (short) (OFFSET_CDATA + 2), aesWorkspace, (short) 0, encSize);
        Util.arrayCopy(buffer, (short) (OFFSET_CDATA + 2 + encSize), ivdata, (short) 0, (short) 16);

        aesCipher.init(sharedKey, Cipher.MODE_DECRYPT, ivdata, (short) 0, (short) 16);
        aesCipher.doFinal(aesWorkspace, (short) 0, encSize, buffer, (short) (OFFSET_CDATA + 2));
    }

    private short getBlockSize(short msgSize) {
        short blocks = (short) (msgSize / 16);
        if ((msgSize % 16) > 0)
            blocks++;

        return blocks;
    }

    //</editor-fold>

    public void printBytes(byte[] buffer){
        System.out.println(byteArrayToHex(buffer));
    }
}
