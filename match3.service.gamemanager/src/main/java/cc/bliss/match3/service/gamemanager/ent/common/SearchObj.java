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
public class SearchObj {

    private long fromDate = 0;
    private long toDate = System.currentTimeMillis();
    private int page;
    private int limit;
    private int playerID;
    private int roomID;
    private String username;
    private String searchString;
}
