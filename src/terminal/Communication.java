package terminal;

import com.licel.jcardsim.io.JavaxSmartCardInterface;
import javacard.framework.*;
import org.bouncycastle.util.encoders.Hex;

import javax.smartcardio.CardException;
import javax.smartcardio.*;
import java.util.Arrays;

import static javacard.framework.ISO7816.*;

public class Communication {
    public Communication(){

    }

    public void init(){
        sim.installApplet(appletAID, EPApplet.class);
        sim.selectApplet(appletAID);
    }

    public ResponseAPDU sendINS(byte ins){
        try{
            CommandAPDU apdu = new CommandAPDU(0, ins, 0, 0, 5);
            return sim.transmitCommand(apdu);
        }catch (Exception e){
            System.out.println("ERROR: sendINS()");
            return null;
        }
    }

    public ResponseAPDU send(byte cla, byte ins, byte p1, byte p2, byte ne){
        try{
            CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, ne);
            return sim.transmitCommand(apdu);
        }catch (Exception e){
            System.out.println("ERROR: send()");
            return null;
        }
    }

    public ResponseAPDU sendData(byte cla, byte ins, byte p1, byte p2, byte[] data, byte ne){
        try{
            CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, data, ne);
            return sim.transmitCommand(apdu);
        }catch (Exception e){
            System.out.println("ERROR: sendData()");
            return null;
        }
    }


    public void printAPDU(byte[] buffer){
        String CLA = String.format("0x%02X", buffer[OFFSET_CLA]);
        String INS = String.format("0x%02X", buffer[OFFSET_INS]);
        String P1 = String.format("0x%02X", buffer[OFFSET_P1]);
        String P2 = String.format("0x%02X", buffer[OFFSET_P2]);
        String Lc = String.format("0x%02X", buffer[OFFSET_LC]);

        System.out.println("----------- Terminal -----------");
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

    //private static final byte[] TEST_APPLET1_AID_BYTES = Hex.decode("01020304050607080A");
    private static final byte[] TEST_APPLET1_AID_BYTES = {(byte) 1,(byte) 2,(byte) 3,(byte) 4,(byte) 5,(byte) 6,(byte) 7,(byte) 8,(byte) 9,};
    private static final AID appletAID = new AID(TEST_APPLET1_AID_BYTES, (short)0, (byte) TEST_APPLET1_AID_BYTES.length);
    JavaxSmartCardInterface sim = new JavaxSmartCardInterface();
}
