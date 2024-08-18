/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EBadge {
    UNRANKED(0),
    BRONZE3(10), BRONZE2(11), BRONZE1(12),
    SILVER3(20), SILVER2(21), SILVER1(22),
    GOLD3(30), GOLD2(31), GOLD1(32),
    PLATINUM3(40), PLATINUM2(41), PLATINUM1(42),
    DIAMOND3(50), DIAMOND2(51), DIAMOND1(52),
    MASTER3(60), MASTER2(61), MASTER1(62),
    GRAND_MASTER3(70), GRAND_MASTER2(71), GRAND_MASTER1(72),
    CHALLENGER(80);
    private final int value;

    EBadge(int value) {
        this.value = value;
    }

    public static EBadge findByTrophy(int value) {
        if (value <= 0) {
            return UNRANKED;
        } else if (value <= 49) {
            return BRONZE3;
        } else if (value <= 99) {
            return BRONZE2;
        } else if (value <= 199) {
            return BRONZE1;
        } else if (value <= 249) {
            return SILVER3;
        } else if (value <= 299) {
            return SILVER2;
        } else if (value <= 399) {
            return SILVER1;
        } else if (value <= 449) {
            return GOLD3;
        } else if (value <= 499) {
            return GOLD2;
        } else if (value <= 599) {
            return GOLD1;
        } else if (value <= 649) {
            return PLATINUM3;
        } else if (value <= 699) {
            return PLATINUM2;
        } else if (value <= 799) {
            return PLATINUM1;
        } else if (value <= 849) {
            return DIAMOND3;
        } else if (value <= 899) {
            return DIAMOND2;
        } else if (value <= 999) {
            return DIAMOND1;
        } else if (value <= 1049) {
            return MASTER3;
        } else if (value <= 1099) {
            return MASTER2;
        } else if (value <= 1199) {
            return MASTER1;
        } else if (value <= 1249) {
            return GRAND_MASTER3;
        } else if (value <= 1299) {
            return GRAND_MASTER2;
        } else if (value <= 1399) {
            return GRAND_MASTER1;
        } else if (value >= 1400) {
            return CHALLENGER;
        }
        return UNRANKED;
    }

    public int getValue() {
        return value;
    }
}
