/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EHeroClass {
    RED, GREEN, YELLOW, BLUE;

    public static EHeroClass findByName(String name) {
        for (EHeroClass value : EHeroClass.values()) {
            if (value.name().toLowerCase().contentEquals(name.toLowerCase())) {
                return value;
            }
        }
        return RED;
    }
}
