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

    public static void main(String[] args){

        /*
        PaymentTerminal panel = new PaymentTerminal(frame);
        c.add(panel);
        frame.setResizable(true);
        frame.pack();
        frame.setSize(300,280);
        frame.setVisible(true);
        switchToRT();
        */


        Protocol prot = new Protocol();
        prot.init();
        prot.change_soft_limit(25);




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
        frame.setSize(300,280);
        PaymentTerminal panel = new PaymentTerminal(frame);
        c.removeAll();
        c.repaint();
        c.revalidate();
        c.add(panel);
        c.repaint();
        c.revalidate();
    }

    public static void switchToRT(){
        frame.setSize(300,320);
        ReloadTerminal panel = new ReloadTerminal(frame);
        c.removeAll();
        c.repaint();
        c.revalidate();
        c.add(panel);
        c.repaint();
        c.revalidate();
    }

    public static void switchToIT(){
        frame.setSize(550,450);
        InitializationTerminal panel = new InitializationTerminal(frame);
        c.removeAll();
        c.repaint();
        c.revalidate();
        c.add(panel);
        c.repaint();
        c.revalidate();
    }
}

