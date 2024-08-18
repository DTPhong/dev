package cc.bliss.match3.service.gamemanager.ent.enums;

public enum EBotType {
    USER(0), TUTORIAL(1), VERY_EASY(2), EASY(3), HARD(4), VERY_HARD(5), SUPER_HARD(6);

    private final int value;

    EBotType(int value ) {
        this.value = value;
    }

    public static EBotType fromValue(int value) {
        for (EBotType faction : EBotType.values()) {
            if (faction.value == value) {
                return faction;
            }
        }
        return USER;
    }
}
