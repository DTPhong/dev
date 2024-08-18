/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum EQuestStatus {
    EXPIRED(0), CLAIMABLE(1), PROGRESS(2), CLAIMED(3), RECLAIM(4);

    private final int value;

    private EQuestStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
