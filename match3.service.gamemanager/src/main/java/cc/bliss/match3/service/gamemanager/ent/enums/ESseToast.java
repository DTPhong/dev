/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.enums;

/**
 * @author Phong
 */
public enum ESseToast {

    QUEST_COMPLETE(1);

    private final int value;

    private ESseToast(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
