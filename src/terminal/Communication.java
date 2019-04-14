package terminal;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class Communication {
    public void Communication(){

    }

    public ResponseAPDU send(/*byte ins*/){
        //TODO: add try block
        byte[] d = {1,2,3,4, -127, 10};
        CommandAPDU apdu = new CommandAPDU(0, 3, 0, 0, d,4);
        ResponseAPDU response = new ResponseAPDU(fake.transit(apdu));
        return response;
    }
    FakeCard fake = new FakeCard();
}
