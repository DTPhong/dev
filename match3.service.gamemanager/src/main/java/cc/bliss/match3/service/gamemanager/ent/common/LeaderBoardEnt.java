/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Phong
 */
@Data
@AllArgsConstructor
public class LeaderBoardEnt {

    long userID;
    int trophy;
    int rank;
    long timeReachedTrophy;
    int myTrophy;
    int heroId;

    public LeaderBoardEnt(long userID, int trophy) {
        this.userID = userID;
        this.trophy = trophy;
    }
    public LeaderBoardEnt(long userID, int trophy, int rank) {
        this.userID = userID;
        this.trophy = trophy;
        this.rank = rank;
    }
    public LeaderBoardEnt(long userID, int trophy, int rank, long timeReachedTrophy) {
        this.userID = userID;
        this.trophy = trophy;
        this.rank = rank;
        this.timeReachedTrophy = timeReachedTrophy;
    }
    public LeaderBoardEnt(int heroId, int trophy, int myTrophy) {
        this.heroId = heroId;
        this.trophy = trophy;
        this.myTrophy = myTrophy;
    }

}
