package terminal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import javacard.framework.*;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static javacard.framework.ISO7816.*;






public class Main {




    static final String TITLE = "Base Terminal";
    static JFrame frame = new JFrame(TITLE);
    static Container c = frame.getContentPane();
    static CardThread ct;
    static Logger logger;

    public static void main(String[] args){
// deposit, reloadterminal

        ct = new CardThread();
        ct.start();
        logger = new Logger();



        ReloadTerminal panel = new ReloadTerminal(frame, ct, logger);

        //PaymentTerminal panel = new PaymentTerminal(frame);

        c.add(panel);
        frame.setResizable(true);
        frame.pack();
        frame.setSize(300,280);
        frame.setVisible(true);
        switchToIT();


//        Protocol prot = new Protocol(ct);
//        prot.init();
//        prot.change_soft_limit(25);




//        Protocol prot = new Protocol();
//        prot.init();
        //prot.test();
        //prot.change_pin(1);





/*
        System.out.println("Hello JavaCard");
        Communication t = new Communication();
        t.init();
        ResponseAPDU res = t.sendINS((byte) 0);
        //t.printAPDU(res.getBytes());
/*
        byte[] data = {0,5,10,15};
        res = t.sendData((byte) 1,(byte) 0,(byte) 0,(byte) 4, data,(byte) 5);
        t.printAPDU(res.getBytes());
        */
    }

    public static void switchToPT(){
        frame.setSize(320,320);
        PaymentTerminal panel = new PaymentTerminal(frame, ct, logger);
        c.removeAll();
        c.repaint();
        c.revalidate();
        c.add(panel);
        c.repaint();
        c.revalidate();
    }

    public static void switchToRT(){
        frame.setSize(300,320);
        ReloadTerminal panel = new ReloadTerminal(frame, ct, logger);
        c.removeAll();
        c.repaint();
        c.revalidate();
        c.add(panel);
        c.repaint();
        c.revalidate();
    }

    public static void switchToIT(){
        frame.setSize(550,450);
        InitializationTerminal panel = new InitializationTerminal(frame, ct, logger);
        c.removeAll();
        c.repaint();
        c.revalidate();
        c.add(panel);
        c.repaint();
        c.revalidate();
    }

    public void setEnabled(boolean b) {
//        super.setEnabled(b);
//        if (b) {
////            setText(0);
//        } else {
////            setText(MSG_DISABLED);
//        }
//        Component[] keys = keypad.getComponents();
//        for (int i = 0; i < keys.length; i++) {
//            keys[i].setEnabled(b);
//        }
    }


}

class CardThread extends Thread {
    CardChannel applet;

    static final byte[] CALC_APPLET_AID = { (byte) 0x22, (byte) 0x34,
            (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0xab };
    static final CommandAPDU SELECT_APDU = new CommandAPDU(
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, CALC_APPLET_AID);

    public void run() {
        try {
            TerminalFactory tf = TerminalFactory.getDefault();
            CardTerminals ct = tf.terminals();
            List<CardTerminal> cs = ct.list();//list(CardTerminals.State.CARD_PRESENT);
//            boolean notCool = ct.list().isEmpty();
            int terminalC = ct.list().size();
            if (terminalC == 0) {
                System.err.println("No terminals found.");
                return;
            } else if (terminalC > 1) {
                System.err.println("Too many terminals found");
            }

            while (true) {
                try {
                    for(CardTerminal c : cs) {
                        if (c.isCardPresent()) {
                            try {
                                Card card = c.connect("*");
                                try {

                                    applet = card.getBasicChannel();
                                    ResponseAPDU resp = applet.transmit(SELECT_APDU);
                                    if (resp.getSW() != 0x9000) {
                                        throw new Exception("Select failed");
                                    }
                                    System.out.println("Card found!");
//                                        setText(sendKey((byte) '='));
//                                    setEnabled(true);

                                    // Wait for the card to be removed
                                    while (c.isCardPresent()){
                                        sleep(100);

                                    }
//                                    setEnabled(false);
//                                        setText(MSG_DISABLED);
                                    break;
                                } catch (Exception e) {
                                    System.err.println("Card does not contain EPApplet?!");
//                                        setText(MSG_INVALID);
                                    sleep(2000);
//                                        setText(MSG_DISABLED);
                                    continue;
                                }
                            } catch (CardException e) {
                                System.err.println("Couldn't connect to card!");
//                                    setText(MSG_INVALID);
                                sleep(2000);
//                                    setText(MSG_DISABLED);
                                continue;
                            }
                        } else {
                            System.err.println("No card present!");
//                                setText(MSG_INVALID);
                            sleep(2000);
//                                setText(MSG_DISABLED);
                            continue;
                        }
                    }
                } catch (CardException e) {
                    System.err.println("Card status problem!");
                }
            }
        } catch (Exception e) {
//            setEnabled(false);
//                setText(MSG_ERROR);
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
