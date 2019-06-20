package terminal;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static terminal.Main.*;



public class PaymentTerminal extends JPanel implements ActionListener, BaseTerminal{

    private static short hardLimit;
    private Protocol protocol;

    public PaymentTerminal(JFrame parent, CardThread ct){
        //Only able to pay if hard limit is not reached
        this.protocol = new Protocol(ct);
        this.protocol.init();
        buildGUI(parent);
        parent.setTitle(TITLE);

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
                        firstDisplayString = "To Be Paid:";
                        secondDisplayString = "";
                        thirdDisplayString = "";
                        updateText();
                        status = STATUS.ToBePaid;
                        break;
                    case "OK":
                        switch(status){
                            case ToBePaid:
                                int amountToBePaid = Integer.parseInt(secondDisplayString);
                                System.out.println("To Be Paid Amount: " + Integer.toString(amountToBePaid));
                                firstDisplayString = "To Be Paid: " + secondDisplayString;
                                secondDisplayString = "Input PIN: ";
                                status = STATUS.InputPIN;
                                break;
                            case InputPIN:
                                int pin = Integer.parseInt(thirdDisplayString);
                                System.out.println("User PIN input: " + Integer.toString(pin));
                                firstDisplayString = "PIN sent to card";
                                secondDisplayString = "";
                                thirdDisplayString = "";
                                status = STATUS.HasPaid;
                                break;
                            case HasPaid:
                                firstDisplayString = "To Be Paid:";
                                secondDisplayString = "";
                                thirdDisplayString = "";
                                status = STATUS.ToBePaid;
                                break;
                        }
                        updateText();
                        break;
                    case "CORR":
                        switch(status){
                            case ToBePaid:
                                secondDisplayString = "";
                                break;
                            case InputPIN:
                                thirdDisplayString = "";
                                break;
                            case HasPaid:

                                break;
                        }
                        updateText();
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
                        switch(status){
                            case ToBePaid:
                                secondDisplayString += str;
                                break;
                            case InputPIN:
                                thirdDisplayString += str;
                                break;
                            case HasPaid:

                                break;
                        }
                        updateText();
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println(MSG_ERROR);
            firstDisplayString = MSG_ERROR;
            secondDisplayString = MSG_ERROR;
            thirdDisplayString = MSG_ERROR;
            updateText();
            status = STATUS.ToBePaid;
        }
    }

    void updateText(){
        firstDisplay.setText(firstDisplayString);
        secondDisplay.setText(secondDisplayString);
        thirdDisplay.setText(thirdDisplayString);
    }

    void buildGUI(JFrame parent) {
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        firstDisplay = new JTextField(DISPLAY_WIDTH);
        firstDisplay.setHorizontalAlignment(JTextField.LEFT);
        firstDisplay.setEditable(false);
        firstDisplay.setFont(FONT);
        firstDisplay.setBackground(Color.darkGray);
        firstDisplay.setForeground(Color.green);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(firstDisplay,gbc);
        firstDisplayString = "To Be Paid:";
        firstDisplay.setText(firstDisplayString);

        secondDisplay = new JTextField(DISPLAY_WIDTH);
        secondDisplay.setHorizontalAlignment(JTextField.RIGHT);
        secondDisplay.setEditable(false);
        secondDisplay.setFont(FONT);
        secondDisplay.setBackground(Color.darkGray);
        secondDisplay.setForeground(Color.green);
        gbc.gridy++;
        add(secondDisplay,gbc);

        thirdDisplay = new JTextField(DISPLAY_WIDTH);
        thirdDisplay.setHorizontalAlignment(JTextField.RIGHT);
        thirdDisplay.setEditable(false);
        thirdDisplay.setFont(FONT);
        thirdDisplay.setBackground(Color.darkGray);
        thirdDisplay.setForeground(Color.green);
        gbc.gridy++;
        add(thirdDisplay, gbc);
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

        gbc.gridy++;

        add(keypad, gbc);
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

    String firstDisplayString = "";
    String secondDisplayString = "";
    String thirdDisplayString = "";
    JTextField thirdDisplay;
    JTextField secondDisplay;
    JTextField firstDisplay;
    JPanel keypad;
    STATUS status = STATUS.ToBePaid;

    enum STATUS{
        ToBePaid,
        InputPIN,
        HasPaid;
    }

}
