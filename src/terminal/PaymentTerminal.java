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

    private Protocol protocol;

    public PaymentTerminal(JFrame parent, CardThread ct, Logger logger, Bank bank){
        this.protocol = new Protocol(ct, logger, bank);
        this.protocol.init();
        buildGUI(parent);
        parent.setTitle(TITLE);

    }

    //Function that gets called when a button is pressed.
    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            Object src = ae.getSource();
            if (src instanceof JButton) {
                char c = ((JButton) src).getText().charAt(0);
                String str = ((JButton) src).getText();
                switch (str){
                    case "STOP":
                        //Stop the terminal and reset to the initial state.
                        isERROR = false;
                        firstDisplayString = "To Be Paid:";
                        secondDisplayString = "";
                        thirdDisplayString = "";
                        updateText();
                        status = STATUS.ToBePaid;
                        break;
                    case "OK":
                        //User has placed an input and the terminal can continue.
                        if(!isERROR){
                            switch(status){
                                //The amount to be payed has been inputted.
                                case ToBePaid:
                                    int amountToBePaidI = Integer.parseInt(secondDisplayString);

                                    //Check if it fits within a short
                                    if (amountToBePaidI > Short.MAX_VALUE || amountToBePaidI < Short.MIN_VALUE) {
                                        firstDisplayString = "Invalid number";
                                        secondDisplayString = "";
                                        thirdDisplayString = "";
                                        status = STATUS.Message;
                                        break;
                                    }

                                    amountToBePaid = (short) amountToBePaidI;
                                    //Send the amount to the java card to check if it is within the limits.
                                    int pay_status = protocol.withdrawal_checklimits(amountToBePaid);
                                    switch (pay_status){
                                        case 1:
                                            // under soft limit; no pin
                                            Protocol.WithdrawResult w = protocol.withdraw();
                                            if (w.success) {
                                                firstDisplayString = "New balance: " + w.balance;
                                            } else {
                                                firstDisplayString = "Error during payment";
                                            }
                                            secondDisplayString = "";
                                            thirdDisplayString = "";
                                            status = STATUS.Message;
                                            break;
                                        case 0:
                                            // internal error
                                            firstDisplayString = "Internal error";
                                            secondDisplayString = "";
                                            thirdDisplayString = "";
                                            status = STATUS.Message;
                                            break;
                                        case -1:
                                            // insufficient balance
                                            firstDisplayString = "Insufficient balance";
                                            secondDisplayString = "";
                                            thirdDisplayString = "";
                                            status = STATUS.Message;
                                            break;
                                        case -2:
                                            // over soft limit; ask pin
                                            System.out.println("To Be Paid Amount: " + Integer.toString(amountToBePaid));
                                            firstDisplayString = "To Be Paid: " + secondDisplayString;
                                            secondDisplayString = "Input PIN: ";
                                            status = STATUS.InputPIN;
                                            break;
                                        case -3:
                                            // hard limit exceeded
                                            firstDisplayString = "Hard limit exceeded";
                                            secondDisplayString = "";
                                            thirdDisplayString = "";
                                            status = STATUS.Message;
                                            break;
                                    }
                                    break;
                                case InputPIN:
                                    //The user has inputted a pin code
                                    int pin = Integer.parseInt(thirdDisplayString);
                                    System.out.println("User PIN input: " + Integer.toString(pin));
                                    firstDisplayString = "";
                                    secondDisplayString = "";
                                    thirdDisplayString = "";
                                    //Send the input pin to the java card to check if it is correct.
                                    boolean pin_ok = protocol.checkPin(WITHDR_CLA,(short) pin, (byte) 3);
                                    System.out.println("Pin check: " + pin_ok);
                                    if(pin_ok) {
                                        //If the pin is correct perform the rest of the withdraw protocol.
                                        Protocol.WithdrawResult w = protocol.withdraw();
                                        if (w.success) {
                                            firstDisplayString = "New balance: " + w.balance;
                                        } else {
                                            firstDisplayString = "Error during payment";
                                        }
                                    } else {
                                        firstDisplayString = "Wrong pin";
                                    }
                                    secondDisplayString = "";
                                    thirdDisplayString = "";
                                    break;
                                case HasPaid:
                                    //The transaction is complete and the terminal returns to its initial state.
                                    firstDisplayString = "To Be Paid:";
                                    secondDisplayString = "";
                                    thirdDisplayString = "";
                                    status = STATUS.ToBePaid;
                                    break;
                                case Message:
                                    //The transaction has not been completed and the terminal returns to its initial
                                    // state so that the user can try again.
                                    status = STATUS.ToBePaid;
                                    break;
                            }
                            //Apply text changes to the GUI
                            updateText();
                        }
                        break;
                    case "CORR":
                        //Reset the display such that the user can retry to give a correct input.
                        if(!isERROR){
                            switch(status){
                                case ToBePaid:
                                    secondDisplayString = "";
                                    break;
                                case InputPIN:
                                    thirdDisplayString = "";
                                    break;
                                case HasPaid:
                                    secondDisplayString = "";
                                    thirdDisplayString = "";
                                    break;
                            }
                            updateText();
                        }
                        break;
                    case "RT":
                        //Switch to the Reload terminal
                        switchToRT();
                        break;
                    case "PT":
                        //Switch to the Payment terminal
                        switchToPT();
                        break;
                    case "IT":
                        //Switch to the Initialization terminal
                        switchToIT();
                        break;
                    default:
                        //Add button number to the display
                        if(!isERROR){
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
                        }
                        break;
                }
            }
        } catch (Exception e) {
            isERROR = true;
            e.printStackTrace();
            firstDisplayString = MSG_ERROR;
            secondDisplayString = MSG_ERROR;
            thirdDisplayString = "Press STOP to RESET";
            System.out.println("ERROR: Press the STOP button to reset");
            updateText();
            status = STATUS.ToBePaid;
        }
    }

    //Update the display text on the GUI
    void updateText(){
        firstDisplay.setText(firstDisplayString);
        secondDisplay.setText(secondDisplayString);
        thirdDisplay.setText(thirdDisplayString);
    }

    //Construct the GUI
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

    //Give correct colours to the keys.
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
    static private final byte WITHDR_CLA = (byte) 0xd3;
    short amountToBePaid = 0;
    boolean isERROR = false;

    enum STATUS{
        ToBePaid,
        InputPIN,
        Message,
        HasPaid
    }

}
