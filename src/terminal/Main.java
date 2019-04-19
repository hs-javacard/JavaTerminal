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


public class Main {
    public static void main(String[] args){
        System.out.println("Hello JavaCard");
        Communication t = new Communication();
        t.init();
        ResponseAPDU res = t.sendINS((byte) 1);
        System.out.println(res.getBytes());

        /*
        byte[] data = {0,5,10,15};
        t.sendData((byte) 0,(byte) 2,(byte) 0,(byte) 0, data,(byte) 5);
        */
    }
}
