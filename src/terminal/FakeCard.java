package terminal;

import javacard.framework.*;
import javax.smartcardio.CommandAPDU;
import static javacard.framework.ISO7816.*;


public class FakeCard {
    public void FakeCard(){

    }

    public byte[] transit(CommandAPDU apdu){
        byte[] buffer = process(apdu);
        return buffer;
    }

    public byte[] process(CommandAPDU apdu)throws ISOException{
        byte[] buffer = apdu.getBytes();
        byte ins = buffer[OFFSET_INS];

        switch (ins) {
            case 0x00:
                System.out.println("--- Instruction 0 ---");
                printAPDU(apdu);
                break;
            case 0x01:
                break;
            case 0x02:
                break;
            default:
                System.out.println("ERROR: Unknown Instruction");
                printAPDU(apdu);
                break;
        }

        return buffer;
    }

    public void printAPDU(CommandAPDU apdu){
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
