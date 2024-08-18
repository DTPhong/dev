/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.Data;

/**
 * @author Phong
 */
@Data
public class LeaderBoardSubTab {
    int id;
    String heroName;
    String heroImg;
    String topName;
    String topImg;
    int topTrophy;
    int currentTrophy;
    int mainGem;
}
