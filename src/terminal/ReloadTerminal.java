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

    private Communication comm;

    public ReloadTerminal(JFrame parent){
        this.comm = new Communication();
        comm.init();
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
                        firstDisplayString = "Input PIN: ";
                        secondDisplayString = "";
                        thirdDisplayString = "";
                        fourthDisplayString = "";
                        status = STATUS.PIN;
                        updateText();
                        break;
                    case "OK":
                        switch (status){
                            case PIN:
                                int pin = Integer.parseInt(secondDisplayString);
                                System.out.println("User PIN input: " + Integer.toString(pin));
                                firstDisplayString = "1) Balance";
                                secondDisplayString = "2) Soft Limit";
                                thirdDisplayString = "3) Change PIN";
                                fourthDisplayString = "";
                                status = STATUS.MENU;
                                updateText();
                                break;
                            case MENU:
                                switch (fourthDisplayString){
                                    case "1":
                                        firstDisplayString = "Input Balance: ";
                                        secondDisplayString = "";
                                        thirdDisplayString = "";
                                        fourthDisplayString = "";
                                        status = STATUS.BALANCE;
                                        break;
                                    case "2":
                                        firstDisplayString = "Input Soft Limit: ";
                                        secondDisplayString = "";
                                        thirdDisplayString = "";
                                        fourthDisplayString = "";
                                        status = STATUS.SOFT_LIMIT;
                                        break;
                                    case "3":
                                        firstDisplayString = "Input PIN: ";
                                        secondDisplayString = "";
                                        thirdDisplayString = "";
                                        fourthDisplayString = "";
                                        status = STATUS.CHANGE_PIN;
                                        break;
                                    default:
                                        break;
                                }
                                updateText();
                                break;
                            case BALANCE:
                                int balance = Integer.parseInt(secondDisplayString);
                                System.out.println("User Balance input: " + Integer.toString(balance));
                                firstDisplayString = "1) Balance";
                                secondDisplayString = "2) Soft Limit";
                                thirdDisplayString = "3) Change PIN";
                                fourthDisplayString = "";
                                status = STATUS.MENU;
                                updateText();
                                break;
                            case SOFT_LIMIT:
                                int limit = Integer.parseInt(secondDisplayString);
                                System.out.println("User Soft Limit input: " + Integer.toString(limit));
                                firstDisplayString = "1) Balance";
                                secondDisplayString = "2) Soft Limit";
                                thirdDisplayString = "3) Change PIN";
                                fourthDisplayString = "";
                                status = STATUS.MENU;
                                updateText();
                                break;
                            case CHANGE_PIN:
                                int newPin = Integer.parseInt(secondDisplayString);
                                System.out.println("User New PIN input: " + Integer.toString(newPin));
                                firstDisplayString = "1) Balance";
                                secondDisplayString = "2) Soft Limit";
                                thirdDisplayString = "3) Change PIN";
                                fourthDisplayString = "";
                                status = STATUS.MENU;
                                updateText();
                                break;
                        }

                        break;
                    case "CORR":
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
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println(MSG_ERROR);
            //System.out.println(e.getMessage());
            firstDisplayString = MSG_ERROR;
            secondDisplayString = MSG_ERROR;
            thirdDisplayString = MSG_ERROR;
            fourthDisplayString = MSG_ERROR;
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
        firstDisplayString = "Input PIN:";
        firstDisplay.setText(firstDisplayString);

        secondDisplay = new JTextField(DISPLAY_WIDTH);
        secondDisplay.setHorizontalAlignment(JTextField.LEFT);
        secondDisplay.setEditable(false);
        secondDisplay.setFont(FONT);
        secondDisplay.setBackground(Color.darkGray);
        secondDisplay.setForeground(Color.green);
        secondDisplay.setMinimumSize(d);
        gbc.gridy++;
        add(secondDisplay,gbc);
        secondDisplayString = "";
        secondDisplay.setText(secondDisplayString);


        thirdDisplay = new JTextField(DISPLAY_WIDTH);
        thirdDisplay.setHorizontalAlignment(JTextField.LEFT);
        thirdDisplay.setEditable(false);
        thirdDisplay.setFont(FONT);
        thirdDisplay.setBackground(Color.darkGray);
        thirdDisplay.setForeground(Color.green);
        thirdDisplay.setMinimumSize(d);
        gbc.gridy++;
        add(thirdDisplay, gbc);
        thirdDisplayString = "";
        thirdDisplay.setText(thirdDisplayString);


        fourthDisplay = new JTextField(DISPLAY_WIDTH);
        fourthDisplay.setHorizontalAlignment(JTextField.RIGHT);
        fourthDisplay.setEditable(false);
        fourthDisplay.setFont(FONT);
        fourthDisplay.setBackground(Color.darkGray);
        fourthDisplay.setForeground(Color.green);
        fourthDisplay.setMinimumSize(d);
        gbc.gridy++;
        add(fourthDisplay, gbc);
        fourthDisplayString = "";
        fourthDisplay.setText(fourthDisplayString);

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

    public void addBalance(){

    }

    public void changeSoftLimit(){

    }

    public void changePin(){

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
    STATUS status = STATUS.PIN;

    enum STATUS{
        PIN,
        MENU,
        BALANCE,
        SOFT_LIMIT,
        CHANGE_PIN;
    }
}
