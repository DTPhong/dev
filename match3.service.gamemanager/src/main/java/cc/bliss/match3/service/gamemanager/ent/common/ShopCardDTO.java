/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Phong
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopCardDTO {

    int id;
    int rewardType;
    int rewardQuantity;
    int rewardRefID;
    int status;

    String title;
    int moneyType;
    int moneyRequire;
}
