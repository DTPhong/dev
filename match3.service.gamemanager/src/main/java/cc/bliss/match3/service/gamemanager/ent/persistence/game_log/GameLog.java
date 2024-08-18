/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.game_log;

import bliss.lib.framework.util.ConvertUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Entity
@Table(name = "gamelogs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private int roomID;
    private long duration;
    private long matchEndAtMs;
    @Lob
    private String rounds;
    private String playerIDS;
    private long winID;
    private String userJoinType;
    private String userSubJoinType;
    private int matchRoundCount;
    private int score;
    private int gameMode;
    private long startTime;
    private String playerInfo;
    private String winInfo;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime;
    @Transient
    private Map<Integer, Integer> hpInfo = new HashMap<>();

    public GameLog(int roomID, long duration, String rounds, String playerIDS, long winID,
                   int score, long startTime, String playerInfo, int gameMode, Map<Integer, Integer> hpInfo,
                   long matchEndAtMs, String userJoinType, String userSubJoinType, int matchRoundCount) {
        this.roomID = roomID;
        this.duration = duration;
        this.rounds = rounds;
        this.playerIDS = playerIDS;
        this.winID = winID;
        this.score = score;
        this.startTime = startTime;
        this.playerInfo = playerInfo;
        this.gameMode = gameMode;
        this.matchEndAtMs = matchEndAtMs;
        this.userJoinType = userJoinType;
        this.userSubJoinType = userSubJoinType;
        this.matchRoundCount = matchRoundCount;
    }

    public List<Long> getListUserID() {
        return Arrays.asList(playerIDS.split(",")).stream().map(e -> ConvertUtils.toLong(e)).collect(Collectors.toList());
    }

    public Date getCreatedTime() {
        return createdTime == null ? new Date() : createdTime;
    }
}
