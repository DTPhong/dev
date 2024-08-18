/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.Data;

import java.util.List;

/**
 * @author Phong
 */
@Data
public class ChestConfig {

    int id;
    int price;
    int moneyType;
    //field for win battle - mystery box
    int appearanceRate;
    // -------
    List<RewardEnt> chestItems;
}
