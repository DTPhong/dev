/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author baotn
 */
public enum ETriggerType {
    TROPHY_ROAD(0),
    QUEST_EVENT(1),
    DAILY_REWARD(2),
    DAILY_QUEST(3),
    GACHA_EVENT(4),
    LOGIN_7DAYS(5);

    private final int value;

    private ETriggerType(int value) {
        this.value = value;
    }

    /**
     * Find a the enum type by its integer value, as defined in the Thrift IDL.
     *
     * @param value
     * @return null if the value is not found.
     */
    public static ETriggerType findByValue(int value) {
        ETriggerType[] triggerTypes = ETriggerType.values();
        for (ETriggerType triggerType : triggerTypes) {
            if (triggerType.getValue() == value) {
                return triggerType;
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
