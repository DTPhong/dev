package cc.bliss.match3.service.gamemanager.ent.enums;

public enum EEventType {
    NONE(0),
    RUSH_ARENA(1),
    RUSH_SAVE_THE_HEART(11),
    RUSH_ACTIVE_LIGHTNING(12),
    RUSH_WIN_STREAK(13),
    RUSH_PERFECT_VICTORY(14),
    WIN_BATTLE(2);

    private final int value;

    private EEventType(int value) {
        this.value = value;
    }

    public static EEventType findByValue(int value) {
        EEventType[] triggerTypes = EEventType.values();
        for (EEventType triggerType : triggerTypes) {
            if (triggerType.getValue() == value) {
                return triggerType;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
