package terminal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static terminal.Main.*;

public class InitializationTerminal extends JPanel implements ActionListener, BaseTerminal{

    private Communication comm;

    public InitializationTerminal(JFrame parent){
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
                    case "Save":
                        int k = strConvToInt(keysTF.getText());
                        int cn = strConvToInt(cardNumbTF.getText());
                        int iban = strConvToInt(ibanTF.getText());
                        int sl = strConvToInt(softLimitTF.getText());
                        int hl = strConvToInt(hardLimitTF.getText());

                        System.out.println("Keys: " + Integer.toString(k));
                        System.out.println("Card Number: " + Integer.toString(cn));
                        System.out.println("IBAN: " + Integer.toString(iban));
                        System.out.println("Soft Limit: " + Integer.toString(sl));
                        System.out.println("Hard Limit: " + Integer.toString(hl));
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
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println(MSG_ERROR);
        }
    }

    void buildGUI(JFrame parent) {
        setLayout(new FlowLayout());
        /*
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        */
        Dimension dField = new Dimension(450, 25);
        JPanel p = new JPanel(new FlowLayout(SwingConstants.LEADING, 10, 10));
        p.setPreferredSize(new Dimension(500,330));
        JLabel keysL = new JLabel("Keys: ");
        keysTF = new JTextField("0");
        keysL.setLabelFor(keysTF);
        keysTF.setPreferredSize(dField);
        p.add(keysL);
        p.add(keysTF);

        JLabel cardNumbL = new JLabel("Card Number: ");
        cardNumbTF = new JTextField("0");
        cardNumbL.setLabelFor(cardNumbTF);
        cardNumbTF.setPreferredSize(dField);
        p.add(cardNumbL);
        p.add(cardNumbTF);

        JLabel ibanL = new JLabel("IBAN: ");
        ibanTF = new JTextField("0");
        ibanL.setLabelFor(ibanTF);
        ibanTF.setPreferredSize(dField);
        p.add(ibanL);
        p.add(ibanTF);

        JLabel softLimitL = new JLabel("Soft Limit: ");
        softLimitTF = new JTextField("0");
        softLimitL.setLabelFor(softLimitTF);
        softLimitTF.setPreferredSize(dField);
        p.add(softLimitL);
        p.add(softLimitTF);

        JLabel hardLimitL = new JLabel("Hard Limit: ");
        hardLimitTF = new JTextField("0");
        hardLimitL.setLabelFor(hardLimitTF);
        hardLimitTF.setPreferredSize(dField);
        p.add(hardLimitL);
        p.add(hardLimitTF);


        JPanel gp = new JPanel(new GridLayout(1,3));
        JButton buttonPT = new JButton("PT");
        buttonPT.setBackground(Color.GRAY);
        buttonPT.addActionListener(this);
        JButton buttonRT = new JButton("RT");
        buttonRT.setBackground(Color.GRAY);
        buttonRT.addActionListener(this);
        JButton buttonSave = new JButton("Save");
        buttonSave.addActionListener(this);
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

    public void loadProfile(){
        //Keys, card number, bank account number, soft / hard limit
    }

    public void changeProfile(){

    }

    public void decommissionCard(){

    }

    public int strConvToInt(String str){
        try {
            return Integer.parseInt(str);
        }catch (Exception e){
            System.out.println(MSG_ERROR + "strConvToInt()");
            return -1;
        }
    }

    JTextField keysTF;
    JTextField cardNumbTF;
    JTextField ibanTF;
    JTextField softLimitTF;
    JTextField hardLimitTF;

    private static final long serialVersionUID = 1L;
    static final String TITLE = "Initialization Terminal";
    static final Font FONT = new Font("Monospaced", Font.BOLD, 24);
    static final Dimension PREFERRED_SIZE = new Dimension(300, 300);

    static final int DISPLAY_WIDTH = 20;
    static final String MSG_ERROR = "    -- error --     ";
    static final String MSG_DISABLED = " -- insert card --  ";
    static final String MSG_INVALID = " -- invalid card -- ";
}
