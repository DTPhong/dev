/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author baotn
 */
public enum ETriggerStatus {
    DISABLE(0),
    ENABLE(1);

    private final int value;

    private ETriggerStatus(int value) {
        this.value = value;
    }

    /**
     * Find a the enum type by its integer value, as defined in the Thrift IDL.
     *
     * @return null if the value is not found.
     */
    public static ETriggerStatus findByValue(int value) {
        ETriggerStatus[] triggerStatuses = ETriggerStatus.values();
        for (ETriggerStatus triggerStatus : triggerStatuses) {
            if (triggerStatus.getValue() == value) {
                return triggerStatus;
            }
        }
        return null;
    }

    /**
     * Get the integer value of this enum value, as defined in the Thrift IDL.
     */
    public int getValue() {
        return value;
    }
}
