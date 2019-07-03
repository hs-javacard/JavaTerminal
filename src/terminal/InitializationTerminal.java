package terminal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static terminal.Main.*;

public class InitializationTerminal extends JPanel implements ActionListener, BaseTerminal{

    private Protocol protocol;

    public InitializationTerminal(JFrame parent, CardThread ct, Logger logger, Bank bank){
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
                        //Stop the initialization terminal and reset to the initial state.
                        isError = false;
                        resetTextFields();
                        break;
                    case "Save":
                        //Perform the initialization of the java card.
                        if(!isError){
                            int balance = Integer.parseInt(balanceTF.getText());
                            int sl = Integer.parseInt(softLimitTF.getText());
                            int hl = Integer.parseInt(hardLimitTF.getText());
                            protocol.initialization((short) balance, (short) sl, (short) hl);
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
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: Press the save button to reset");
            e.printStackTrace();
            setTextFieldsToERROR();
        }
    }

    //Builds the GUI for the Initialization Terminal
    void buildGUI(JFrame parent) {
        setLayout(new FlowLayout());

        Dimension dField = new Dimension(450, 25);
        JPanel p = new JPanel(new FlowLayout(SwingConstants.LEADING, 10, 10));
        p.setPreferredSize(new Dimension(500,330));

        JLabel ibanL = new JLabel("Balance: ");
        balanceTF = new JTextField("1000");
        ibanL.setLabelFor(balanceTF);
        balanceTF.setPreferredSize(dField);
        p.add(ibanL);
        p.add(balanceTF);

        JLabel softLimitL = new JLabel("Soft Limit: ");
        softLimitTF = new JTextField("500");
        softLimitL.setLabelFor(softLimitTF);
        softLimitTF.setPreferredSize(dField);
        p.add(softLimitL);
        p.add(softLimitTF);

        JLabel hardLimitL = new JLabel("Hard Limit: ");
        hardLimitTF = new JTextField("4500");
        hardLimitL.setLabelFor(hardLimitTF);
        hardLimitTF.setPreferredSize(dField);
        p.add(hardLimitL);
        p.add(hardLimitTF);

        JPanel gp = new JPanel(new GridLayout(1,4));
        JButton buttonPT = new JButton("PT");
        buttonPT.setBackground(Color.GRAY);
        buttonPT.addActionListener(this);
        JButton buttonRT = new JButton("RT");
        buttonRT.setBackground(Color.GRAY);
        buttonRT.addActionListener(this);
        JButton buttonSave = new JButton("Save");
        buttonSave.setBackground(Color.green);
        buttonSave.addActionListener(this);
        JButton buttonStop = new JButton("STOP");
        buttonStop.setBackground(Color.red);
        buttonStop.addActionListener(this);

        gp.add(buttonStop);
        gp.add(buttonPT);
        gp.add(buttonRT);
        gp.add(buttonSave);


        add(p);
        add(gp);
        parent.addWindowListener(new InitializationTerminal.CloseEventListener());
    }

    class CloseEventListener extends WindowAdapter {
        public void windowClosing(WindowEvent we) {
            System.exit(0);
        }
    }

    //Set the text field to display the error message
    public void setTextFieldsToERROR(){
        balanceTF.setText(MSG_ERROR + "Press STOP to RESET");
        softLimitTF.setText(MSG_ERROR + "Press STOP to RESET");
        hardLimitTF.setText(MSG_ERROR + "Press STOP to RESET");

        balanceTF.setEditable(false);
        softLimitTF.setEditable(false);
        hardLimitTF.setEditable(false);

        isError = true;
    }

    //Reset the text fields to display the initial values.
    public void resetTextFields(){
        balanceTF.setText("1000");
        softLimitTF.setText("500");
        hardLimitTF.setText("4500");

        balanceTF.setEditable(true);
        softLimitTF.setEditable(true);
        hardLimitTF.setEditable(true);
    }

    JTextField balanceTF;
    JTextField softLimitTF;
    JTextField hardLimitTF;
    boolean isError = false;

    private static final long serialVersionUID = 1L;
    static final String TITLE = "Initialization Terminal";
    static final Font FONT = new Font("Monospaced", Font.BOLD, 24);
    static final Dimension PREFERRED_SIZE = new Dimension(300, 300);

    static final int DISPLAY_WIDTH = 20;
    static final String MSG_ERROR = "    -- error --     ";
    static final String MSG_DISABLED = " -- insert card --  ";
    static final String MSG_INVALID = " -- invalid card -- ";
}
