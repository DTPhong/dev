/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EEventRecordType {
    DAILY, WEEKLY, SEASONAL, MONTHLY, NONE;

    public static EEventRecordType findByName(String name) {
        for (EEventRecordType value : EEventRecordType.values()) {
            if (value.name().toLowerCase().contentEquals(name.toLowerCase())) {
                return value;
            }
        }
        return DAILY;
    }
}
