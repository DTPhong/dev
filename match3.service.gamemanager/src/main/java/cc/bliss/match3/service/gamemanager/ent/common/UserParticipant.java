package cc.bliss.match3.service.gamemanager.ent.common;

import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import lombok.Data;

@Data
public class UserParticipant {
    private long userId;
    private String userName;
    private int trophy;
    private int score;
    private int rank;
    private ERewardType rewardType;
    private int rewardQuantity;
    private String displayName;
    private String email;
    private String password;
    private String avatarPath;
    private long money;
    private String googleId;
    private String googleName;
    private String googleAvatar;
    private String gmail;
    private String facebookID;
    private String facebookName;
    private String facebookAvatar;
    private String appleName;
    private String appleID;
    private String appleEmail;
    private String appleAvatar;
    private String deviceID;
    private String deviceToken;
    private int selectHero;
    private double exp;
    private int battleWon;
    private int dominateWin;
    private int godLikeWin;
    private int winStreak;
    private int highestTrophy;
    private int highestStreak;
    private long emerald;
    private long diamond;
    private long amethyst;
    private long royalAmethyst;
    private int frame;
    private float timeZone;
    private int clanID = -1;
    private int botType;
    private long timeReachedScore;
    private int botLevel;
    private boolean isClaimed = false;
}
