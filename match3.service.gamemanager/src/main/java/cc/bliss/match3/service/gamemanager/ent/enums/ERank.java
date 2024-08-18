/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum ERank {
    UNRANKED(0), BRONZE(1), SILVER(2), GOLD(3), PLATINUM(4), DIAMOND(5), MASTER(6), GRAND_MASTER(7), CHALLENGER(8);

    private final int value;

    ERank(int value) {
        this.value = value;
    }

    public static ERank findByValue(int value) {
        ERank[] values = ERank.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getValue() == value) {
                return values[i];
            }
        }
        return UNRANKED;
    }

    public static ERank findByTrophy(int value) {
        if (value >= 0 && value <= 199) {
            return BRONZE;
        } else if (value <= 399) {
            return SILVER;
        } else if (value <= 599) {
            return GOLD;
        } else if (value <= 799) {
            return PLATINUM;
        } else if (value <= 999) {
            return DIAMOND;
        } else if (value <= 1199) {
            return MASTER;
        } else if (value <= 1399) {
            return GRAND_MASTER;
        } else if (value >= 1400) {
            return CHALLENGER;
        } else {
            return UNRANKED;
        }
    }

    public int getValue() {
        return value;
    }
}
