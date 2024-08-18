/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EHeroRarity {
    MYTHIC, EPIC, LEGENDARY;

    public static EHeroRarity findByName(String name) {
        for (EHeroRarity value : EHeroRarity.values()) {
            if (value.name().toLowerCase().contentEquals(name.toLowerCase())) {
                return value;
            }
        }
        return MYTHIC;
    }
}
