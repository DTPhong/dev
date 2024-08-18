/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import cc.bliss.match3.service.gamemanager.ent.enums.EUpdateMoneyResult;
import lombok.Data;

/**
 * @author Phong
 */
@Data
public class UpdateMoneyResult {

    private long before;
    private long after;
    private long delta;
    private EUpdateMoneyResult eUpdateMoneyResult;
}
