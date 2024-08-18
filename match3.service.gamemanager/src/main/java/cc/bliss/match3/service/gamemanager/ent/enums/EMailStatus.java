/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EMailStatus {
    UNREAD(0),
    READ(1),
    DELETE(2),
    CLAIMED(3);

    private final int value;

    private EMailStatus(int value) {
        this.value = value;
    }

    /**
     * Find a the enum type by its integer value, as defined in the Thrift IDL.
     *
     * @return null if the value is not found.
     */
    public static EMailStatus findByValue(int value) {
        switch (value) {
            case 0:
                return UNREAD;
            case 1:
                return READ;
            case 2:
                return DELETE;
            case 3:
                return CLAIMED;
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
