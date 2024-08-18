/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author Phong
 */
@Data
public class LeaderBoardMainTab {
    int id;
    String title;
    List<LeaderBoardSubTab> subTab = Collections.EMPTY_LIST;
}
