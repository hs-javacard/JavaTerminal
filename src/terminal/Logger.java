package terminal;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class Logger {

//<<<<<<< HEAD
//    private Map<Short, Short> pin_map;
//    private Map<Short, Object> exp_map;
//    private Map<Short, Object> mod_map;
//    private Map<Short, byte[]> sec_map = new HashMap<>();
//=======
    static String NEWCARDNUMBER;
    static String NEWCARD;
    static String NEWREQUEST;
    static String LOCKEDATTEMPT;
    static String PINATTEMPT_S;
    static String PINATTEMPT_F;
    static String CONFIRMATION;
    static String PINCHANGE;

    private String logPath;
//>>>>>>> master

    public Logger(){
        NEWCARDNUMBER = "New Card Number";
        NEWCARD =       "New Card";
        NEWREQUEST =    "New Request";
        LOCKEDATTEMPT = "Locked card attempt";
        PINATTEMPT_S =  "PIN attempt success";
        PINATTEMPT_F =  "PIN attempt failed";
        CONFIRMATION =  "Confirmation by card";
        PINCHANGE =     "PIN changed";

        this.logPath = "log.txt";
    }

    public void logRequest(short card_number, String message, String protocol){
        String content = card_number + ' ' + message + ' ' + protocol + ' ' + getTS();

        try {
            FileWriter.write(content, logPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logRequestSigned(short card_number, String message, String amount, String protocol) {
        String content = card_number + ' ' + message + ' ' + amount + ' ' + protocol + ' ' + getTS();
        try {
            FileWriter.write(content,logPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//<<<<<<< HEAD
//    public void SaveSec(short card_number, byte[] sec){
//        sec_map.put(card_number, sec);
//    }
//
//    public short getPin(short card_number){
//        return pin_map.get(card_number);
//    }
//
//    public byte[] getExp(short card_number){return (byte[]) exp_map.get(card_number);}
//
//    public byte[] getMod(short card_number){return (byte[]) mod_map.get(card_number);}
//
//    public byte[] getSec(short card_number){return sec_map.get(card_number);}
//=======
    private Timestamp getTS() {
        return new Timestamp(System.currentTimeMillis());
    }
//>>>>>>> master
}
