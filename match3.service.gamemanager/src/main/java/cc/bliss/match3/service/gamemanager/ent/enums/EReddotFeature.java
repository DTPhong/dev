/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EReddotFeature {
    DAILY_QUEST(0), MAIL(1), DAILY_REWARD(2), EVENT_LOGIN_7D(4)
//    FRIEND(3),
//    OVERALL(99)
    ;
    private final int value;

    EReddotFeature(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
