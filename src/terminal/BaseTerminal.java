package terminal;

import javacard.framework.*;

public interface BaseTerminal{

}
/*
class InitializationTerminal extends BaseTerminal{

    public void InitializationTerminal(){

    }

    public void loadProfile(){
        //Keys, card number, bank account number, soft / hard limit
    }

    public void changeProfile(){

    }

    public void decommissionCard(){

    }
}

class ReloadTerminal extends BaseTerminal{

    public void ReloadTerminal(){

    }

    public void addBalance(){

    }

    public void changeSoftLimit(){

    }

    public void changePin(){

    }

}

class PaymentTerminal extends BaseTerminal{

    private short hardLimit;

    public void PaymentTerminal(){
        //Only able to pay if hard limit is not reached
    }

    public void removeMoney(){

    }

    private boolean checkPIN(short pin){
        //Only needed when over soft limit

        return true;
    }
}
*/