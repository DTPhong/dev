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
public class Statistic {

    int heroID;
    String heroName;
    int totalHand = 0;
    int winHand = 0;
    int trophy = 0;
    String winRate;
}
