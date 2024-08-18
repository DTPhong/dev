/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum ReddotConstant {
    MAIL(0),
    SHOP(1),
    CHAT(2),
    EVENT(4),
    LEADERBOARD(5),
    INVITE_FRIEND(11),
    LIST_INVITE(12),
    FOOTBALL_CUP_REWARDS(15),
    LUCKY_WHEEL(16);

    public static int[] ReddotNoti = {MAIL.value, SHOP.value, CHAT.value, EVENT.value
            , LEADERBOARD.value, INVITE_FRIEND.value, LIST_INVITE.value, FOOTBALL_CUP_REWARDS.value, LUCKY_WHEEL.value};
    private final int value;

    private ReddotConstant(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
