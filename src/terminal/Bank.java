package terminal;

import java.util.*;

public class Bank {

    private Map<Short, Map<Short, ArrayList<byte[]>>> cardList;
    private Map<Short, ArrayList<byte[]>> banValue;
    private ArrayList<byte[]> otherInfo;

    public Bank() {
        this.cardList = new HashMap<>();
        this.banValue = new HashMap<>();
        this.otherInfo = new ArrayList<>();
    }

    /**
     * Generates a new card number.
     * Also sets dummy values for other information about the card.
     *
     * @return new card number
     */
    public short generateCardNumber() {
        short cn = 9; //TODO random cn
        short ban = 1; //TODO random ban

        //Dummy values (null)
        otherInfo.add(0,null); //EXP
        otherInfo.add(1,null); //MOD
        otherInfo.add(2,null); //STATE
        otherInfo.add(3,null); //SEC

        banValue.put(ban, otherInfo);
        cardList.put(cn, banValue);

        return cn;
    }

    /**
     * Set public key, state and secret of the card, in the following structure.
     *
     * Structure:
     *          CN,
     *              BAN,
     *                  EXP, MOD, STATE, SEC
     *
     * Where:   CN: Card Number
     *          BAN: Bank Account Number
     *          EXP: Public key exponent
     *          MOD: Public key modulo
     *          SEC: Card Secret
     */
    public void setCardInfo(short cn, byte[] exp, byte[] mod, byte[] sec) {
        byte[] state = new byte[1];// TODO set card state to ready, we for now assume this 0 means unlocked
        short linkedBan = getBan(cn);

        if (cardList.containsKey(cn) && banValue.containsKey(linkedBan)) {
            otherInfo.set(0, exp);
            otherInfo.set(1, mod);
            otherInfo.set(2, state);
            otherInfo.set(3, sec);

            banValue.put(linkedBan, otherInfo);
            cardList.put(cn, banValue);
        }
    }

    /**
     * Gives public key, state and secret of the card, in the following order.
     *
     * 0: Exponent (Public Key)
     * 1: Modulo (Public Key)
     * 2: State
     * 3: Secret
     *
     * @return list of the object numbered above
     *
     */
    public ArrayList<byte[]> getCardInfo(short cn) {
        short linkedBan = getBan(cn);

        if (cardList.containsKey(cn) && banValue.containsKey(linkedBan)) {
            return cardList.get(cn).get(linkedBan);
        }
        else {
            return null;
        }

    }

    /**
     * Update state of card.
     *
     * @param cn Card Number
     * @param state State of the card
     */
    public void updateStateCard(short cn, byte[] state) {
        short linkedBan = getBan(cn);

        if (cardList.containsKey(cn) && banValue.containsKey(linkedBan)) {
            cardList.get(cn).get(linkedBan).set(2, state);
        }
    }

    /**
     * Get bank account number of the card.
     * Relates to the given card number.
     *
     * @return short of bank account number
     */
    private short getBan(short cn) {
        Object[] newBanValues;
        newBanValues = cardList.get(cn).keySet().toArray();

        return (Short) newBanValues[0];
    }
}
