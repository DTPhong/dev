/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EShopSection {
    DAILY_OFFER(0), WISH_CRYSTAL(1), CHEST(2), COIN_PACK(3), EMERALD_PACK(4);

    private final int value;

    private EShopSection(int value) {
        this.value = value;
    }

    /**
     * Find a the enum type by its integer value, as defined in the Thrift IDL.
     *
     * @return null if the value is not found.
     */
    public static EShopSection findByValue(int value) {
        EShopSection[] sections = EShopSection.values();
        for (EShopSection section : sections) {
            if (section.getValue() == value) {
                return section;
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
