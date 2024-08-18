/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import bliss.lib.framework.util.ConvertUtils;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.ent.enums.TicketStatus;
import lombok.Data;

import javax.persistence.Transient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Phong
 */
@Data
public class TicketEnt {

    private long userID;
    private String username;
    private int heroTrophy;
    private int totalTrophy;
    private int level;
    private int winStreak;
    private int loseStreak;
    private int battleWon;
    private long botID;

    private long initTime;
    private long matchTime;

    private TicketStatus status;

    private int roomID;
    private String ip;
    private int port;
    private int currentRoom;
    private int maxRoom;
    private String gameServerId;
    private String gameServerNameSpace = "";
    private int tutorial;

    /**
     * List ticket đã so sánh key userID / value timestamp create ticket
     */
    private Map<Long, Long> comparingTicket = new HashMap<>();


    public TicketEnt() {
        initTime = System.currentTimeMillis();
        status = TicketStatus.WAITING;
    }

    public boolean isExpiredTicket() {
        if (status == TicketStatus.ON_ROOM) {
            return System.currentTimeMillis() - matchTime > GameConstant.DELTA_TIME_EXPIRED_ON_ROOM_TICKET;
        } else if (status == TicketStatus.MATCHED) {
            return System.currentTimeMillis() - matchTime > GameConstant.DELTA_TIME_EXPIRED_MATCHED_TICKET;
        } else {
            return System.currentTimeMillis() - initTime > GameConstant.DELTA_TIME_EXPIRED_FINDING_TICKET;
        }
    }

    /**
     * @return
     * Group tier các user theo chuỗi trận thắng
     * Group1: 0-4
     * Group2: 5-8
     * Group3: 9-12
     * Group4: else
     */
    public int getWinStreakTier(){
        if (winStreak <= 5)
            return 0;
//        else if (winStreak >= 5 && winStreak <= 8)
//            return 1;
//        else if (winStreak >= 9 && winStreak <= 12)
//            return 2;
        else
            return 3;
    }

    /**
     * @return
     * Thời gian tìm trận
     */
    public long getDeltaTime(){
        return System.currentTimeMillis() - initTime;
    }

    /**
     * @return
     * Độ chênh lệch level hero giữa 2 user, mỗi 1s tăng 1
     * Tối đa 2
     */
    public int getDeltaLevel(){
        long deltaTime = getDeltaTime();
        int deltaLevel = ConvertUtils.toInt(deltaTime / 1000);
        if (deltaLevel > 2){
            deltaLevel = 2;
        }
        return deltaLevel;
    }

    /**
     * @return
     * Độ chênh lệch trophy, mỗi 0.5s tăng deltaTrophy lên 10
     * Tối đa 50
     */
    public int getDeltaTrophy(){
        long deltaTime = getDeltaTime();
        int deltaTrophy = ConvertUtils.toInt(10 * deltaTime / 500);
        if (deltaTrophy > 50){
            deltaTrophy = 50;
        }
        return deltaTrophy;
    }

    /**
     * @return
     * Group 1: user below 4000
     * Group 2: else
     */
    public int getGroupTrophy(){
        if (totalTrophy < 4000)
            return 1;
        else
            return 2;
    }

    /**
     * Group 0: [...;5]
     * Group 1: [6;10]
     * Group 2: [11; 99]
     * Group 3: > 99
     * @return
     */
    public int getGroupWinStreak(){
        if(loseStreak >= 0 || winStreak <= 5) return 0;
        if(winStreak <= 10) return 1;
        if(winStreak <= 99) return 2;
        return 3;
    }

    public int getGroupHeroTrophy(){
        int winStreakGroup = getGroupWinStreak();
        switch (winStreakGroup) {
            case 0:
                if(heroTrophy <= 20) return -1;
                if(heroTrophy <= 100) return 0;
                if(heroTrophy <= 300) return 1;
                if(heroTrophy <= 500) return 2;
                if(heroTrophy <= 800) return 3;
                if(heroTrophy <= 1200) return 4;
                if(heroTrophy <= 9999) return 5;
                return 6;
            case 1:
            case 2:
                if(heroTrophy <= 20) return -2;
                if(heroTrophy <= 200) return 10;
                if(heroTrophy <= 300) return 11;
                if(heroTrophy <= 500) return 12;
                if(heroTrophy <= 800) return 13;
                if(heroTrophy <= 1200) return 14;
                if(heroTrophy <= 9999) return 15;
                return 16;
            default:
                return -3;
        }
    }

    /**
     * Kiểm tra heroLevel của đối thủ có phù hợp
     * @param opponentHeroLevel
     * @return
     */
    public boolean isOpponentHeroLevelInRange(int opponentHeroLevel) {
        if(opponentHeroLevel >= getMinOpponentHeroLevel()
                && opponentHeroLevel <= getMaxOpponentHeroLevel()) return true;
        return false;
    }

    /**
     * Theo file config
     * @return
     */
    private int getMinOpponentHeroLevel() {
        int winStreakGroup = getGroupWinStreak();
        int heroTrophyGroup = getGroupHeroTrophy();
        if(winStreakGroup == 0){
            if(heroTrophyGroup <= 2){
                return level -2;
            }
            return level -1;
        }
        else if(winStreakGroup == 1){
            if(heroTrophyGroup <= 12){
                return level -2;
            }
            return level -1;
        }
        else if(winStreakGroup == 2){
            if(heroTrophyGroup <= 12){
                return level -2;
            }
            return level -1;
        }
        return level;
    }

    /**
     * Theo file config
     * @return
     */
    private int getMaxOpponentHeroLevel() {
        int winStreakGroup = getGroupWinStreak();
        int heroTrophyGroup = getGroupHeroTrophy();
        if(winStreakGroup == 0){
            if(heroTrophyGroup <= 2){
                return level +2;
            }
            if(heroTrophyGroup == 3){
                return level +3;
            }
            if(heroTrophyGroup == 4){
                return level +4;
            }
            return level +99;
        }
        else if(winStreakGroup == 1 || winStreakGroup == 2){
            if(heroTrophyGroup <= 12){
                return level +2;
            }
            if(heroTrophyGroup == 13){
                return level +3;
            }
            if(heroTrophyGroup == 14){
                return level +4;
            }
            return level +99;
        }
        return level;
    }

    /**
     * Kiểm tra heroTrophy của đối thủ có phù hợp không
     * @param opponentHeroTrophy
     * @return
     */
    public boolean isOpponentHeroTrophyInRange(int opponentHeroTrophy) {
        if(opponentHeroTrophy >= getMinOpponentHeroTrophy()
                && opponentHeroTrophy <= getMaxOpponentHeroTrophy()) return true;
        return false;
    }

    /**
     * Theo file config
     * @return
     */
    private int getMinOpponentHeroTrophy() {
        int winStreakGroup = getGroupWinStreak();
        int heroTrophyGroup = getGroupHeroTrophy();
        if(winStreakGroup == 0){
            if(heroTrophyGroup <= 4){
                return heroTrophy -50;
            }
            return heroTrophy -100;
        }
        else if(winStreakGroup == 1 || winStreakGroup == 2){
            if(heroTrophyGroup <= 14){
                return heroTrophy -50;
            }
            return heroTrophy -100;
        }
        return heroTrophy;
    }

    /**
     * Theo file config
     * @return
     */
    private int getMaxOpponentHeroTrophy() {
        int winStreakGroup = getGroupWinStreak();
        int heroTrophyGroup = getGroupHeroTrophy();
        if(winStreakGroup == 0){
            if(heroTrophyGroup <= 2){
                return heroTrophy +50;
            }
            else if(heroTrophyGroup == 3){
                return heroTrophy +100;
            }
            else if(heroTrophyGroup == 4){
                return heroTrophy +200;
            }
            return heroTrophy +9999;
        }
        else if(winStreakGroup == 1 || winStreakGroup == 2){
            if(heroTrophyGroup <= 12){
                return heroTrophy +50;
            }
            else if(heroTrophyGroup == 13){
                return heroTrophy +100;
            }
            else if(heroTrophyGroup == 14){
                return heroTrophy +200;
            }
            return heroTrophy +9999;
        }
        return heroTrophy;
    }

    /**
     * Tìm độ khó của Bot (theo file config)
     * @return
     */
    public int getMinBotHardLevel(){
        if(heroTrophy <= 20 ) return 1;
        if(loseStreak >= 5 ) return 1;
        if(loseStreak >= 2) return 1;
        if(winStreak <= 2){
            if(totalTrophy <= 199) return 1;
            if(totalTrophy <= 599) return 2;
            return 2;
        }
        if(winStreak <= 5){
            if(totalTrophy <= 599) return 1;
            return 3;
        }
        if(winStreak <= 10){
            if(totalTrophy <= 599) return 2;
            return 4;
        }

        if(totalTrophy <= 599) return 4;
        return 5;
    }

    public int getMaxBotHardLevel(){
        if(heroTrophy <= 20 ) return 1;
        if(loseStreak >= 5 ) return 1;
        if(loseStreak >= 2) return 1;
        if(winStreak <= 2){
            if(totalTrophy <= 199) return 2;
            if(totalTrophy <= 599) return 3;
            return 4;
        }
        if(winStreak <= 5){
            if(totalTrophy <= 599) return 3;
            return 4;
        }
        if(winStreak <= 10){
            if(totalTrophy <= 599) return 4;
            return 5;
        }

        if(totalTrophy <= 599) return 6;
        return 7;
    }

    public int getMinBotHeroLevel(){
        if(heroTrophy <= 20) return level;
        if(loseStreak >= 5 ) return level -2;
        if(loseStreak >= 2) return level -1;
        if(winStreak <= 5){
            return level -1;
        }
        if(winStreak <= 10){
            return level;
        }
       return level +1;
    }

    public int getMaxBotHeroLevel(){
        if(heroTrophy <= 20) return level;
        if(loseStreak >= 5 ) return level -2;
        if(loseStreak >= 2) return level -1;
        if(winStreak <= 2) return level +1;
        return level +2;
    }
}
