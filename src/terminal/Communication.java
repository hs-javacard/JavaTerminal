package terminal;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class Communication {
    public void Communication(){

    }

    public ResponseAPDU sendINS(byte ins){
        //TODO: add try block
        CommandAPDU apdu = new CommandAPDU(0, ins, 0, 0, 5);
        ResponseAPDU response = new ResponseAPDU(fake.transit(apdu));
        return response;
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
}
