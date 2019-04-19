package terminal;

import javacard.framework.*;

import javax.smartcardio.CommandAPDU;

public class EPApplet extends Applet implements ISO7816 {

    private short balance;

    public static void install(byte[] buffer, short offset, byte length)
            throws SystemException {
        new EPApplet();
    }

    public EPApplet() {
        balance = 10;
        register();
    }

    public void process(APDU apdu) throws ISOException {
        printAPDU(apdu.getBuffer());
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[OFFSET_INS];
        byte p1 = buffer[OFFSET_P1];

        if (selectingApplet()) { // we ignore this, it makes ins = -92
            return;
        }

        switch (ins) {
            case 0: // increment balance
                System.out.println("Instuction 0");
            case 1: // decrement balance
//                short a = 0;
                changeBalance(ins, p1);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
        }


//        Util.setShort(buffer, OFFSET_CDATA, balance);
//        Util.setShort(buffer, OFFSET_LC,  (short) 2);
        System.out.println(buffer);


        short le = -1;
        le = apdu.setOutgoing();
        if (le < 2) {
            ISOException.throwIt((short) (SW_WRONG_LENGTH | 5));
        }


        Util.setShort(buffer, (short) 1, (short) 0);
        Util.setShort(buffer, (short) 3, (short) 42);
        apdu.setOutgoingLength((short) 5);
        apdu.sendBytes((short) 0, (short) 5);

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

    public void printAPDU(byte[] buffer){
        CommandAPDU apdu = new CommandAPDU(buffer);
        String CLA = String.format("0x%02X", apdu.getCLA());
        String INS = String.format("0x%02X", apdu.getINS());
        String P1 = String.format("0x%02X", apdu.getP1());
        String P2 = String.format("0x%02X", apdu.getP2());
        String Lc = String.format("0x%02X", apdu.getNc());
        String Le = String.format("0x%02X", apdu.getNe());

        System.out.println("CLA: " + CLA + " | INS: " + INS + " | P1: " + P1 + " | P2: " + P2 + " | Le: " + Le);

        if(apdu.getNc() > 0){
            System.out.println("Lc: " + Lc + " | DATA: " + byteArrayToHex(apdu.getData()));
        }
    }

    public static String byteArrayToHex(byte[] a) {
        String result = "[";
        for(byte b: a)
            result += String.format("0x%02X", b) + ",";
        result = result.substring(0, result.length() - 1) + "]";
        return result;
    }

}
