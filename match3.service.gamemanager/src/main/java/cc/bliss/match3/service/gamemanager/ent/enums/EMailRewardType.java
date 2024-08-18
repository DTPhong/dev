/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EMailRewardType {
    MONEY(0),
    VIP_POINT(1),
    GIFTCODE(2),
    BCOIN(3);

    private final int value;

    private EMailRewardType(int value) {
        this.value = value;
    }

    /**
     * Find a the enum type by its integer value, as defined in the Thrift IDL.
     *
     * @return null if the value is not found.
     */
    public static EMailRewardType findByValue(int value) {
        switch (value) {
            case 0:
                return MONEY;
            case 1:
                return VIP_POINT;
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
