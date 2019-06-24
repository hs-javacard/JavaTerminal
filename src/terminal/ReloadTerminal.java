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

    public ReloadTerminal(JFrame parent, CardThread ct, Logger logger){
        this.protocol = new Protocol(ct, logger);
        this.protocol.init();
        buildGUI(parent);
        parent.setTitle(TITLE);

    }

    public void menuToPIN(){
        firstDisplayString = "Input PIN: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.PIN;
        updateText();
    }

    public void menuToSTART(){
        firstDisplayString = "1) Balance";
        secondDisplayString = "2) Soft Limit";
        thirdDisplayString = "3) Change PIN";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.MENU;
        updateText();
    }

    public void menuToBALANCE(){
        firstDisplayString = "Input Balance: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.BALANCE;
        updateText();
    }

    public void menuToSOFT_LIMIT(){
        firstDisplayString = "Input Soft Limit: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.SOFT_LIMIT;
        updateText();
    }

    public void menuToCHANGE_PIN(){
        firstDisplayString = "Input new PIN: ";
        secondDisplayString = "";
        thirdDisplayString = "";
        fourthDisplayString = "";
        prev_status = status;
        status = STATUS.CHANGE_PIN;
        updateText();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            Object src = ae.getSource();
            if (src instanceof JButton){
                char c = ((JButton) src).getText().charAt(0);
                String str = ((JButton) src).getText();
                switch (str){
                    case "*":
//                        protocol.testt();
                        break;
                    case "STOP":
                        isERROR = false;
                        menuToSTART();
                        break;
                    case "OK":
                        if(!isERROR){
                            switch (status){
                                case PIN:
                                    int pin = Integer.parseInt(secondDisplayString);
                                    System.out.println("User PIN input: " + Integer.toString(pin));
                                    switch (prev_status){
                                        case CHANGE_PIN:
                                            System.out.println("CHANGE_PIN");
                                            boolean successp = protocol.authentication((byte) 0xd1, (short) pin);
                                            if (successp) {
                                                protocol.change_pin(newPin);
                                            } else {
                                                System.out.println("Authentication failed!");
                                            }

                                            menuToSTART();
                                            break;
                                        case SOFT_LIMIT:
                                            boolean success = protocol.authentication((byte) 0xd2, (short) pin);
                                            if (success){
                                                protocol.change_soft_limit(limit);
                                            } else {
                                                System.out.println("Authentication failed!");
                                            }

                                            menuToSTART();
                                            break;
                                        case BALANCE:
                                            //protocol.authentication((byte) 0xd4, (short) pin);
//                                            protocol.deposit(balance);
//                                            menuToSTART();
                                            break;
                                        default:
                                            break;
                                    }
                                    break;
                                case MENU:
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
                                    balance = Integer.parseInt(secondDisplayString);
                                    System.out.println("User Balance input: " + Integer.toString(balance));

                                    protocol.deposit(balance);
                                    menuToSTART();
                                    //menuToPIN();
                                    break;
                                case SOFT_LIMIT:
                                    limit = Integer.parseInt(secondDisplayString);
                                    System.out.println("User Soft Limit input: " + Integer.toString(limit));

                                    menuToPIN();
                                    break;
                                case CHANGE_PIN:
                                    newPin = Integer.parseInt(secondDisplayString);
                                    System.out.println("User New PIN input: " + Integer.toString(newPin));

                                    menuToPIN();
                                    break;
                            }
                        }
                        break;
                    case "CORR":
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
                        switchToRT();
                        break;
                    case "PT":
                        switchToPT();
                        break;
                    case "IT":
                        switchToIT();
                        break;
                    default:
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
            System.out.println(e.getMessage());
            firstDisplayString = MSG_ERROR;
            secondDisplayString = MSG_ERROR;
            thirdDisplayString = MSG_ERROR;
            fourthDisplayString = "Press STOP to RESET";
            updateText();
            status = STATUS.PIN;
        }
    }

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

    void buildGUI(JFrame parent) {
        setLayout(new GridBagLayout());

        //parent.setSize(new Dimension(300, 300));

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
