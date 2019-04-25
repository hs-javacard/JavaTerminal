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
    public static void main(String[] args){
        System.out.println("Hello JavaCard");
        Communication t = new Communication();
        t.init();
        ResponseAPDU res = t.sendINS((byte) 0);
        t.printAPDU(res.getBytes());

        byte[] data = {0,5,10,15};
        res = t.sendData((byte) 0,(byte) 0,(byte) 0,(byte) 4, data,(byte) 5);
        t.printAPDU(res.getBytes());
    }
}

