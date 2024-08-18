/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

import java.util.Arrays;
import java.util.List;

/**
 * @author Phong
 */
public enum EFriendRequestType {
    NONE(-1), SEND_FRIEND_REQUEST(0), ACCEPT_FRIEND_REQUEST(1), DENY_FRIEND_REQUEST(2), REMOVE_FRIEND(3), SEND_FRIEND_BATTLE(4), ACCEPT_FRIEND_BATTLE(5), DENY_FRIEND_BATTLE(6);
    private final int value;

    EFriendRequestType(int value) {
        this.value = value;
    }

    public static EFriendRequestType findByValue(int value) {
        List<EFriendRequestType> eFriendRequestTypes = Arrays.asList(EFriendRequestType.values());
        return eFriendRequestTypes.stream().filter(e -> e.getValue() == value).findAny().orElse(NONE);
    }

    public int getValue() {
        return value;
    }
}
