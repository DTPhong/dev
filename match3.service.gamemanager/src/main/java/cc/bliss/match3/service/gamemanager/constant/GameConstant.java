/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.constant;

import bliss.lib.framework.util.DateTimeUtils;

import java.time.Duration;

/**
 * @author Phong
 */
public class GameConstant {

    //agones game server count room key
    public static String GAME_SERVER_COUNT_ROOM_KEY = "gameserver_count_room_%s";

    //user detect
    public static String CLIENT_INFO = "client_info_%s";

    //rush_event
    public static int RUSH_EVENT_RANK_1 = 400;
    public static int RUSH_EVENT_RANK_2 = 300;
    public static int RUSH_EVENT_RANK_3 = 200;
    public static int RUSH_EVENT_RANK_4_5 = 100;

    // gacha
    public static int GACHA_RARE_QUANTITY = 5;
    public static int GACHA_EPIC_QUANTITY = 1;
    public static int GACHA_LEGENDARY_QUANTITY = 1;

    // leaderboard constant
    public static int DEFAULT_WIN_TROPHY = 12;
    public static int DEFAULT_LOSE_TROPHY = 10;
    public static int HIGH_BONUS_WIN_TROPHY = 17;
    public static int HIGH_BONUS_LOSE_TROPHY = 17;
    public static int LOW_BONUS_WIN_TROPHY = 5;
    public static int LOW_BONUS_LOSE_TROPHY = 5;
    public static int DELTA_TROPHY_BONUS = 30;
    public static int PAGE_SIZE_LEADERBOARD = 300;
    public static int LEADERBOARD_TROPHY_REQUIRE = 100;
    public static int PERCENT_RESET_GRAND_MASTER = 100 - 40;
    public static int PERCENT_RESET_MASTER = 100 - 45;
    public static int PERCENT_RESET_REMAIN = 50;
    public static long START_SS_MILIS = DateTimeUtils.getMiliseconds(2024, 06, 10);
    public static long END_SS_MILIS = DateTimeUtils.getMiliseconds(2024, 07, 20);

    // profile constant
    public static long DOMINATING_TIME = 30000l;
    public static int MAX_BEAR = 40;

    // authen server secret key
    public static String AUTHORIZE_SECRET_TOKEN = "e10adc3949ba59abbe56e057f20f883e";
    public static int SERVER_ID = 1;

    // friend constant
    public static int FRIEND_LIMIT = 50;
    public static long FRIEND_BATTLE_EXPIRE = 60000l;

    // end game
    public static int EXP_WIN = 5;
    public static int EXP_LOSE = 1;
    public static int EXP_DRAW = 3;

    // game log
    public static int GAME_LOG_PAGE_SIZE = 50;
    public static int MAX_HP = 2400;

    // hero
    public static int HERO_MAX_LEVEL = 10;
    public static int HERO_MAX_SKILL = 3;

    // match maker
    public static int INIT_TICKET_DELTA_TROPHY = 50;
    public static int INCR_DELTA_TROPHY = 50;
    public static long JUNIOR_TROPHY = 100;
    public static int JUNIOR_INIT_TICKET_DELTA_TROPHY = 20;
    public static int JUNIOR_INCR_DELTA_TROPHY = 20;
    /**
     * Delta trophy tối đa có thể tăng
     */
    public static int MAX_INCR_DELTA_TROPHY = 125;
    /**
     * Cách 2 phút thì tăng delta trophy lên 5
     */
    public static long DELTA_TIME_INCR_DELTA_TROPHY = 100l;
    /**
     * khi user trong room chơi, ko request update thì ticket sẽ lưu trạng thái
     * 6 phút
     */
    public static long DELTA_TIME_EXPIRED_ON_ROOM_TICKET = Duration.ofMinutes(10).toMillis();
    public static long DELTA_TIME_EXPIRED_MATCHED_TICKET = Duration.ofSeconds(15).toMillis();
    public static long DELTA_TIME_MATCHING_BOT = Duration.ofSeconds(10).toMillis();
    public static long DELTA_TIME_MATCHING_EVENT_BOT = Duration.ofSeconds(25).toMillis();
    /**
     * 30s ko có request update ticket thì ticket bị expired
     */
    public static long DELTA_TIME_EXPIRED_FINDING_TICKET = DELTA_TIME_MATCHING_BOT + Duration.ofSeconds(5).toMillis();
    public static long GAME_ROUND_REWARD = 2;

    public static long CLAN_CREATE_MONEY = 10000;

    public static int MAX_NAME_LENGTH = 15;
    public static final int QUEST_SURVEY_ID = -100;
    public static final int ADS_QUEST_ID = -200;
    public static final int OVERVIEW_QUEST_ID = 0;
    public static final int WIN_HERO_QUEST_ID = 1000;
    public static final int DEAL_DMG_HERO_QUEST_ID = 2000;
}
