package cc.bliss.match3.service.gamemanager.ent.enums;

public enum EHeroFaction {
    FORE(0), PIRA(1), KALAS(2), BAJU(3);

    private final int value;

    EHeroFaction(int value ) {
        this.value = value;
    }

    public static EHeroFaction fromValue(int value) {
        for (EHeroFaction faction : EHeroFaction.values()) {
            if (faction.value == value) {
                return faction;
            }
        }
        return null;
    }
}
