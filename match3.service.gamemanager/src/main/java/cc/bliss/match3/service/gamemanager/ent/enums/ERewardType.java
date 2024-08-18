/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

import java.util.List;

/**
 * @author Phong
 */
public enum ERewardType {
    NONE(-1), GOLD(0), HERO_CARD(1), HERO(2), FEATURE(3),
    WOODEN_CHEST(4), SLIVER_CHEST(5), GOLDEN_CHEST(6), SHINY_JEWEL_CHEST(7), BLACK_CROWN_CHEST(8),
    EMERALD(9),
//    DIAMOND(10),
    AMETHYST(11), ROYAL_AMETHYST(12),
    RARE_CARD(13), EPIC_CARD(14), LEGENDARY_CARD(15),
    PACK_GOLD_1(16), PACK_GOLD_2(17), PACK_GOLD_3(18), PACK_GOLD_4(19), PACK_GOLD_5(20), PACK_GOLD_6(21),
    PACK_EMERALD_1(22), PACK_EMERALD_2(23), PACK_EMERALD_3(24), PACK_EMERALD_4(25), PACK_EMERALD_5(26), PACK_EMERALD_6(27),
    CASKET_OF_AMETHYST(28), TUB_OF_AMETHYST(29),
    COMMON_EMERALD(30),
    ADS_CHEST(31), THUMBNAIL(32), REMOVE_ADS_TROPHY_ROAD_PACK(35), BEGINNER_CHEST(36), VALOR_CHEST(37),
    MYSTERY_BOX_RARE(38),MYSTERY_BOX_EPIC(39),MYSTERY_BOX_LEGENDARY(40),MYSTERY_BOX_MYTHIC(41);

    private final int value;

    ERewardType(int value) {
        this.value = value;
    }

    public static ERewardType findByValue(int value) {
        ERewardType[] triggerTypes = ERewardType.values();
        for (ERewardType triggerType : triggerTypes) {
            if (triggerType.getValue() == value) {
                return triggerType;
            }
        }
        return null;
    }

    public static ERewardType findByValueV2(int value, List<ERewardType> triggerTypes) {
        for (ERewardType triggerType : triggerTypes) {
            if (triggerType.getValue() == value) {
                return triggerType;
            }
        }
        return ERewardType.RARE_CARD;
    }

    public static ERewardType findByName(String value) {
        ERewardType[] rewardTypes = ERewardType.values();
        for (ERewardType rewardType : rewardTypes) {
            if (rewardType.name().contentEquals(value)) {
                return rewardType;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public static boolean isGold(int value){
        return value == ERewardType.GOLD.getValue()
                || value == ERewardType.PACK_GOLD_1.getValue()
                || value == ERewardType.PACK_GOLD_2.getValue()
                || value == ERewardType.PACK_GOLD_3.getValue()
                || value == ERewardType.PACK_GOLD_4.getValue()
                || value == ERewardType.PACK_GOLD_5.getValue()
                || value == ERewardType.PACK_GOLD_6.getValue();
    }

    public static boolean isCard(int value){
        return value == ERewardType.HERO_CARD.getValue()
                || value == ERewardType.RARE_CARD.getValue()
                || value == ERewardType.EPIC_CARD.getValue()
                || value == ERewardType.LEGENDARY_CARD.getValue();
    }

    public static boolean isEmerald(int value){
        return value == ERewardType.EMERALD.getValue()
                || value == ERewardType.COMMON_EMERALD.getValue();
    }

    public static boolean isChest(int value){
        return value == ERewardType.WOODEN_CHEST.getValue()
                || value == ERewardType.SLIVER_CHEST.getValue()
                || value == ERewardType.GOLDEN_CHEST.getValue()
                || value == ERewardType.SHINY_JEWEL_CHEST.getValue()
                || value == ERewardType.BLACK_CROWN_CHEST.getValue()
                || value == ERewardType.BEGINNER_CHEST.getValue();
    }
}
