package terminal;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container.*;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.smartcardio.CommandAPDU;
import javax.swing.*;
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static terminal.Main.*;



public class PaymentTerminal extends JPanel implements ActionListener, BaseTerminal{

    private short hardLimit;

    public PaymentTerminal(JFrame parent){
        //Only able to pay if hard limit is not reached
        buildGUI(parent);
        parent.setTitle(TITLE);

    }

    public void removeMoney(){

    }

    private boolean checkPIN(short pin){
        //Only needed when over soft limit
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            Object src = ae.getSource();
            if (src instanceof JButton) {
                char c = ((JButton) src).getText().charAt(0);
                String str = ((JButton) src).getText();
                switch (str){
                    case "STOP":
                        displayString = "";
                        setText(displayString);
                        break;
                    case "OK":
                        displayString = "PIN sent to card";
                        setText(displayString);
                        break;
                    case "CORR":
                        displayString = "";
                        setText(displayString);
                        break;
                    case "RT":
                        switchToRT();
                        break;
                    case "PT":
                        switchToPT();
                        break;
                    case "IT":
                        switchToIT();
                        break;
                    default:
                        displayString += str;
                        setText(displayString);
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println(MSG_ERROR);
        }
    }

    String getText() {
        return display.getText();
    }

    void setText(String txt) {
        display.setText(txt);
    }

    void setText(int n) {
        setText(Integer.toString(n));
    }

    void buildGUI(JFrame parent) {
        setLayout(new BorderLayout());

        helpDisplay = new JTextField(DISPLAY_WIDTH);
        helpDisplay.setHorizontalAlignment(JTextField.RIGHT);
        helpDisplay.setEditable(false);
        helpDisplay.setFont(FONT);
        helpDisplay.setBackground(Color.darkGray);
        helpDisplay.setForeground(Color.green);
        add(helpDisplay, BorderLayout.NORTH);
        helpDisplay.setText("Input PIN:");

        display = new JTextField(DISPLAY_WIDTH);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setEditable(false);
        display.setFont(FONT);
        display.setBackground(Color.darkGray);
        display.setForeground(Color.green);
        add(display, BorderLayout.CENTER);
        keypad = new JPanel(new GridLayout(5, 3));

        key("1");
        key("2");
        key("3");
        key("RT");

        key("4");
        key("5");
        key("6");
        key("IT");

        key("7");
        key("8");
        key("9");
        key(null);

        key("*");
        key("0");
        key("#");
        key(null);

        key("STOP");
        key("CORR");
        key("OK");
        key(null);

        add(keypad, BorderLayout.SOUTH);
        parent.addWindowListener(new CloseEventListener());
    }

    void key(String txt) {
        if (txt == null) {
            keypad.add(new JLabel());
        } else {
            JButton button = new JButton(txt);
            if(txt.equals("OK")){
                button.setBackground(Color.GREEN);
            }else if(txt.equals("CORR")){
                button.setBackground(Color.YELLOW);
            }else if(txt.equals("STOP")){
                button.setBackground(Color.RED);
            }else if(txt.equals("PT") || txt.equals("RT") || txt.equals("IT")){
                button.setBackground(Color.GRAY);
            }
            button.addActionListener(this);
            keypad.add(button);
        }
    }

    class CloseEventListener extends WindowAdapter {
        public void windowClosing(WindowEvent we) {
            System.exit(0);
        }
    }

    private static final long serialVersionUID = 1L;
    static final String TITLE = "Payment Terminal";
    static final Font FONT = new Font("Monospaced", Font.BOLD, 24);
    static final Dimension PREFERRED_SIZE = new Dimension(300, 300);

    static final int DISPLAY_WIDTH = 20;
    static final String MSG_ERROR = "    -- error --     ";
    static final String MSG_DISABLED = " -- insert card --  ";
    static final String MSG_INVALID = " -- invalid card -- ";

    String displayString = "";
    JTextField display;
    JTextField helpDisplay;
    JPanel keypad;

}
