/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import bliss.lib.framework.util.ConvertUtils;
import com.google.gson.JsonObject;
import lombok.Data;

/**
 * @author Phong
 */
@Data
public class QuestDTO implements Cloneable {

    // quest data
    private int id;

    /**
     * EXPIRED(0), CLAIMABLE(1), PROGRESS(2), CLAIMED(3)
     */
    private int status;

    /**
     * Loại quest: play game, nạp, ...
     */
    private int questType;
    private int questRequire;

    // play game rule
    private int heroID = 0;

    /**
     * Chơi với bạn
     */
    private boolean isPlayWithFriend = false;

    /**
     * Chơi thắng liên tiếp
     */
    private boolean isWinMultiple = false;

    /**
     * Chơi thắng
     */
    private boolean isWin = false;

    /**
     * Chơi thắng dưới 45s
     */
    private boolean isWinPerfect = false;

    /**
     * Quest reset mỗi ngày
     */
    private boolean isDaily = false;
    private int rocketMatch = 0;
    private int bombMatch = 0;
    private int lightMatch = 0;

    private int rocketBomb = 0;
    private int lightRocket = 0;
    private int lightBomb = 0;
    private int totalMerge = 0;

    private int hpCount = 0;
    private int redCount = 0;
    private int yellowCount = 0;
    private int blueCount = 0;
    private int greenCount = 0;

    private int require;
    private int progress;
    private int tier;

    private int buttonType;
    private String buttonData = "";

    private int claimedCount;
    private int limitClaimCount;
    // reward data
    private String rewardTitle = "";
    private String rewardDescription = "";
    private String rewardImage = "";
    private String title = "";
    private int rewardQuantity;
    private int rewardType;
    private int rewardRefId;
    private JsonObject rewardData;
    private long price;
    private long cashback;
    private int moneyType;

    //daily reward
    private long nextClaim;

    private long nextWatchAds;
    private String questDetail = "";

    @Override
    public QuestDTO clone() throws CloneNotSupportedException {
        return (QuestDTO) super.clone();
    }

    public int getPercentFinishProgress(){
        return ConvertUtils.toInt(progress * 100 / require);
    }

}
