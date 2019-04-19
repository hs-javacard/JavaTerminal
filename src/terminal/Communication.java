package terminal;

import com.licel.jcardsim.io.JavaxSmartCardInterface;
import javacard.framework.*;
import org.bouncycastle.util.encoders.Hex;

import javax.smartcardio.CardException;
import javax.smartcardio.*;

public class Communication {
    public void Communication(){

    }

    public void init(){
        sim.installApplet(appletAID, EPApplet.class);
        sim.selectApplet(appletAID);
    }

    public ResponseAPDU sendINS(byte ins){
        //TODO: add try block
        CommandAPDU apdu = new CommandAPDU(0, ins, 0, 0, 5);
        return sim.transmitCommand(apdu);
    }

    public ResponseAPDU send(byte cla, byte ins, byte p1, byte p2, byte ne){
        //TODO: add try block
        CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, ne);
        ResponseAPDU response = new ResponseAPDU(fake.transit(apdu));
        return response;
    }

    public ResponseAPDU sendData(byte cla, byte ins, byte p1, byte p2, byte[] data, byte ne){
        //TODO: add try block
        CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, data, ne);
        ResponseAPDU response = new ResponseAPDU(fake.transit(apdu));
        return response;
    }

    FakeCard fake = new FakeCard();

    //private static final byte[] TEST_APPLET1_AID_BYTES = Hex.decode("01020304050607080A");
    private static final byte[] TEST_APPLET1_AID_BYTES = {(byte) 1,(byte) 2,(byte) 3,(byte) 4,(byte) 5,(byte) 6,(byte) 7,(byte) 8,(byte) 9,};
    private static final AID appletAID = new AID(TEST_APPLET1_AID_BYTES, (short)0, (byte) TEST_APPLET1_AID_BYTES.length);
    JavaxSmartCardInterface sim = new JavaxSmartCardInterface();
}
