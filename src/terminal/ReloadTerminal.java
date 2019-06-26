package terminal;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static terminal.Main.*;

public class ReloadTerminal extends JPanel implements ActionListener, BaseTerminal{

    private Protocol protocol;

    public ReloadTerminal(JFrame parent, CardThread ct, Logger logger, Bank bank){
        this.protocol = new Protocol(ct, logger, bank);
        this.protocol.init();
        buildGUI(parent);
        parent.setTitle(TITLE);

    }

    //Change the GUI to the PIN state.
    public void menuToPIN(){
        firstDisplayString = "Input PIN: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.PIN;
        updateText();
    }

    //Change the GUI to the START state.
    public void menuToSTART(){
        firstDisplayString = "1) Balance";
        secondDisplayString = "2) Soft Limit";
        thirdDisplayString = "3) Change PIN";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.MENU;
        updateText();
    }

    //Change the GUI to the BALANCE state.
    public void menuToBALANCE(){
        firstDisplayString = "Input Balance: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.BALANCE;
        updateText();
    }

    //Change the GUI to the SOFT LIMIT state.
    public void menuToSOFT_LIMIT(){
        firstDisplayString = "Input Soft Limit: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.SOFT_LIMIT;
        updateText();
    }

    //Change the GUI to the CHANGE PIN state.
    public void menuToCHANGE_PIN(){
        firstDisplayString = "Input new PIN: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.CHANGE_PIN;
        updateText();
    }

    //Function that gets called when a button is pressed.
    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            Object src = ae.getSource();
            if (src instanceof JButton){
                char c = ((JButton) src).getText().charAt(0);
                String str = ((JButton) src).getText();
                //SWitch for a button press.
                switch (str){
                    case "*":
//                        protocol.testt();
                        break;
                    case "STOP":
                        //Reset the terminal to the initial state
                        isERROR = false;
                        menuToSTART();
                        break;
                    case "OK":
                        if(!isERROR){
                            switch (status){
                                case PIN:
                                    //User inputted a pin
                                    int pin = Integer.parseInt(secondDisplayString);
                                    System.out.println("User PIN input: " + Integer.toString(pin));
                                    switch (prev_status){
                                        case CHANGE_PIN:
                                            //Perform the authentication protocol before we do the change pin protocol.
                                            System.out.println("CHANGE_PIN");
                                            boolean successp = protocol.authentication((byte) 0xd1, (short) pin);
                                            if (successp) {
                                                protocol.change_pin(newPin);
                                            } else {
                                                System.out.println("Authentication failed!");
                                            }
                                            //Return to the START state
                                            menuToSTART();
                                            break;
                                        case SOFT_LIMIT:
                                            //Perform the authentication protocol before we do the soft limit protocol.
                                            boolean success = protocol.authentication((byte) 0xd2, (short) pin);
                                            if (success){
                                                protocol.change_soft_limit(limit);
                                            } else {
                                                System.out.println("Authentication failed!");
                                            }
                                            //Return to the START state
                                            menuToSTART();
                                            break;
                                        default:
                                            break;
                                    }
                                    break;
                                case MENU:
                                    //Switch state depending on the user input
                                    switch (fourthDisplayString){
                                        case "1":
                                            menuToBALANCE();
                                            break;
                                        case "2":
                                            menuToSOFT_LIMIT();
                                            break;
                                        case "3":
                                            menuToCHANGE_PIN();
                                            break;
                                        default:
                                            break;
                                    }
                                    break;
                                case BALANCE:
                                    //Perform the deposit protocol
                                    balance = Integer.parseInt(secondDisplayString);
                                    System.out.println("User Balance input: " + Integer.toString(balance));

                                    protocol.deposit(balance);
                                    //Return to the START state
                                    menuToSTART();
                                    break;
                                case SOFT_LIMIT:
                                    //Save the input of the user and go to the pin state to request a pin code.
                                    limit = Integer.parseInt(secondDisplayString);
                                    System.out.println("User Soft Limit input: " + Integer.toString(limit));

                                    menuToPIN();
                                    break;
                                case CHANGE_PIN:
                                    //Save the input of the user and go to the pin state to request a pin code.
                                    newPin = Integer.parseInt(secondDisplayString);
                                    System.out.println("User New PIN input: " + Integer.toString(newPin));

                                    menuToPIN();
                                    break;
                            }
                        }
                        break;
                    case "CORR":
                        //Reset the display such that the user can retry to give a correct input.
                        if(!isERROR){
                            switch (status){
                                case PIN:
                                    secondDisplayString = "";
                                    break;
                                case MENU:
                                    fourthDisplayString = "";
                                    break;
                                case BALANCE:
                                    secondDisplayString = "";
                                    break;
                                case SOFT_LIMIT:
                                    secondDisplayString = "";
                                    break;
                                case CHANGE_PIN:
                                    secondDisplayString = "";
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
                            switch (status){
                                case PIN:
                                    secondDisplayString += str;
                                    break;
                                case MENU:
                                    fourthDisplayString += str;
                                    break;
                                case BALANCE:
                                    secondDisplayString += str;
                                    break;
                                case SOFT_LIMIT:
                                    secondDisplayString += str;
                                    break;
                                case CHANGE_PIN:
                                    secondDisplayString += str;
                                    break;
                            }
                            updateText();
                        }
                        break;
                }
            }
        } catch (Exception e) {
            isERROR = true;
            System.out.println(MSG_ERROR);
            e.printStackTrace();
            System.out.println(e.getMessage());
            firstDisplayString = MSG_ERROR;
            secondDisplayString = MSG_ERROR;
            thirdDisplayString = MSG_ERROR;
            fourthDisplayString = "Press STOP to RESET";
            updateText();
            status = STATUS.PIN;
        }
    }

    //Update the display text on the GUI
    void updateText(){
        if(status == STATUS.MENU){
            firstDisplay.setHorizontalAlignment(JTextField.LEFT);
            secondDisplay.setHorizontalAlignment(JTextField.LEFT);
            thirdDisplay.setHorizontalAlignment(JTextField.LEFT);
            fourthDisplay.setHorizontalAlignment(JTextField.RIGHT);
        }else{
            firstDisplay.setHorizontalAlignment(JTextField.LEFT);
            secondDisplay.setHorizontalAlignment(JTextField.RIGHT);
            thirdDisplay.setHorizontalAlignment(JTextField.RIGHT);
            fourthDisplay.setHorizontalAlignment(JTextField.RIGHT);
        }
        firstDisplay.setText(firstDisplayString);
        secondDisplay.setText(secondDisplayString);
        thirdDisplay.setText(thirdDisplayString);
        fourthDisplay.setText(fourthDisplayString);
    }

    //Construct the GUI
    void buildGUI(JFrame parent) {
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        Dimension d = new Dimension();
        d.setSize(300,100);
        firstDisplay = new JTextField(DISPLAY_WIDTH);
        firstDisplay.setHorizontalAlignment(JTextField.LEFT);
        firstDisplay.setEditable(false);
        firstDisplay.setFont(FONT);
        firstDisplay.setBackground(Color.darkGray);
        firstDisplay.setForeground(Color.green);
        firstDisplay.setMinimumSize(d);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(firstDisplay,gbc);

        secondDisplay = new JTextField(DISPLAY_WIDTH);
        secondDisplay.setHorizontalAlignment(JTextField.LEFT);
        secondDisplay.setEditable(false);
        secondDisplay.setFont(FONT);
        secondDisplay.setBackground(Color.darkGray);
        secondDisplay.setForeground(Color.green);
        secondDisplay.setMinimumSize(d);
        gbc.gridy++;
        add(secondDisplay,gbc);

        thirdDisplay = new JTextField(DISPLAY_WIDTH);
        thirdDisplay.setHorizontalAlignment(JTextField.LEFT);
        thirdDisplay.setEditable(false);
        thirdDisplay.setFont(FONT);
        thirdDisplay.setBackground(Color.darkGray);
        thirdDisplay.setForeground(Color.green);
        thirdDisplay.setMinimumSize(d);
        gbc.gridy++;
        add(thirdDisplay, gbc);

        fourthDisplay = new JTextField(DISPLAY_WIDTH);
        fourthDisplay.setHorizontalAlignment(JTextField.RIGHT);
        fourthDisplay.setEditable(false);
        fourthDisplay.setFont(FONT);
        fourthDisplay.setBackground(Color.darkGray);
        fourthDisplay.setForeground(Color.green);
        fourthDisplay.setMinimumSize(d);
        gbc.gridy++;
        add(fourthDisplay, gbc);

        menuToSTART();

        keypad = new JPanel(new GridLayout(5, 4));
        key("1");
        key("2");
        key("3");
        key("PT");

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
        parent.addWindowListener(new ReloadTerminal.CloseEventListener());
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
    static final String TITLE = "Reload Terminal";
    static final Font FONT = new Font("Monospaced", Font.BOLD, 24);
    static final Dimension PREFERRED_SIZE = new Dimension(300, 300);

    static final int DISPLAY_WIDTH = 20;
    static final String MSG_ERROR = "    -- error --     ";
    static final String MSG_DISABLED = " -- insert card --  ";
    static final String MSG_INVALID = " -- invalid card -- ";

    String firstDisplayString = "";
    String secondDisplayString = "";
    String thirdDisplayString = "";
    String fourthDisplayString = "";
    JTextField firstDisplay;
    JTextField secondDisplay;
    JTextField thirdDisplay;
    JTextField fourthDisplay;
    JPanel keypad;
    STATUS status = STATUS.MENU;
    STATUS prev_status = STATUS.MENU;

    int balance;
    int newPin;
    int limit;
    boolean isERROR = false;

    enum STATUS{
        PIN,
        MENU,
        BALANCE,
        SOFT_LIMIT,
        CHANGE_PIN;
    }
}
