package terminal;

import java.io.IOException;
import java.sql.Timestamp;

public class Logger {

    static String NEWCARDNUMBER;
    static String NEWCARD;
    static String NEWREQUEST;
    static String LOCKEDATTEMPT;
    static String PINATTEMPT_S;
    static String PINATTEMPT_F;
    static String CONFIRMATION;
    static String PINCHANGE;

    private String logPath;

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
        String content = "SIGNED: " + card_number + ' ' + message + ' ' + amount + ' ' + protocol + ' ' + getTS();
        try {
            FileWriter.write(content,logPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Timestamp getTS() {
        return new Timestamp(System.currentTimeMillis());
    }
}
