package terminal;

import javacard.framework.*;

public class BaseTerminal{

    public void BaseTerminal(){

        }

}

public class InitializationTerminal extends BaseTerminal{

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

public class ReloadTerminal extends BaseTerminal{

    public void ReloadTerminal(){

    }

    public void addBalance(){

    }

    public void changeSoftLimit(){

    }

    public void changePin(){

    }

}

public class PaymentTerminal extends BaseTerminal{

    private short hardLimit;

    public void PaymentTerminal(){
        //Only able to pay if hard limit is not reached
    }

    public void removeMoney(){

    }

    private boolean checkPIN(short pin){
        //Only needed when over soft limit
    }
}