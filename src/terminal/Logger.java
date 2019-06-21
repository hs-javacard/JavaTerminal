package terminal;

import java.util.HashMap;
import java.util.Map;

public class Logger {

    private Map<Short, Short> pin_map;
    private Map<Short, Object> exp_map;
    private Map<Short, Object> mod_map;

    public Logger(){
        this.pin_map = new HashMap<>();
        this.exp_map = new HashMap<>();
        this.mod_map = new HashMap<>();
    }

    public void SavePin(short card_number, short pin){
        pin_map.put(card_number, pin);
    }

    public void SaveExp(short card_number, byte[] exp){
        exp_map.put(card_number, exp);
    }

    public void SaveMod(short card_number, byte[] mod){
        mod_map.put(card_number, mod);
    }

    public short getPin(short card_number){
        return pin_map.get(card_number);
    }

    public byte[] getExp(short card_number){return (byte[]) exp_map.get(card_number);}

    public byte[] getMod(short card_number){return (byte[]) mod_map.get(card_number);}
}
