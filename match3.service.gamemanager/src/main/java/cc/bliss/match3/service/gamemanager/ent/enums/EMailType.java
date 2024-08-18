/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EMailType {
    NOTICE(0),
    REWARD(1),
    GIFTCODE(2),
    CARD(3),
    CRITICAL(4);

    private final int value;

    private EMailType(int value) {
        this.value = value;
    }

    /**
     * Find a the enum type by its integer value, as defined in the Thrift IDL.
     *
     * @return null if the value is not found.
     */
    public static EMailType findByValue(int value) {
        switch (value) {
            case 0:
                return NOTICE;
            case 1:
                return REWARD;
            case 2:
                return GIFTCODE;
            case 3:
                return CARD;
            case 4:
                return CRITICAL;
            default:
                return null;
        }
    }

    /**
     * Get the integer value of this enum value, as defined in the Thrift IDL.
     */
    public int getValue() {
        return value;
    }
}
