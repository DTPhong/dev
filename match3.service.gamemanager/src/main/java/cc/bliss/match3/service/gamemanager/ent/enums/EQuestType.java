/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EQuestType {
    NONE(-1), PLAY_GAME(0), MATCH_SPECIAL(1), MATCH_GEM(2)
    , LOGIN(3), MATCH_MAIN_GEM(4), LOGIN_BY_DATE_ID(6)
    , UPGRADE_HERO(5), REDIRECT_LINK(7), ADS_QUEST(8),
    WIN_WITH_HERO(9), DAME_WITH_HERO(10),
    WATCH_ADS_QUEST(11),COLLECT_RARE_CARD(12),COLLECT_EPIC_CARD(13),
    DEAL_1K_SKILL_DAME(14),MERGE_SPECIAL(15),
    COLLECT_TROPHY(16),COLLECT_GOLD(17), OVERVIEW_QUEST(18),;

    private final int value;

    private EQuestType(int value) {
        this.value = value;
    }

    public static EQuestType findByValue(int value) {
        for (EQuestType questType : EQuestType.values()) {
            if (questType.value == value) {
                return questType;
            }
        }
        return NONE;
    }

    public int getValue() {
        return value;
    }
}
