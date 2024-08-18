/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.system;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.EndGameCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.common.ProfileStatisticService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.event.QuestEventService;
import cc.bliss.match3.service.gamemanager.service.event.RushArenaService;
import cc.bliss.match3.service.gamemanager.service.event.WinBattleService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.var;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.*;

/**
 * @author Phong
 */
@Service
public class GameLogService extends BaseService {

    public static Map<Long, Integer> MAP_GAME_COUNT = new NonBlockingHashMap<>();
    @Autowired
    LeaderboardService leaderboardService;
    @Autowired
    AdminService adminService;
    @Autowired
    ProfileService profileService;
    @Autowired
    TicketService ticketService;
    @Autowired
    RoomService roomService;
    @Autowired
    ProfileStatisticService profileStatisticService;
    @Autowired
    QuestEventService questEventService;
    @Autowired
    DailyQuestService dailyQuestService;
    @Autowired
    private RushArenaService rushArenaService;
    @Autowired
    private WinBattleService winBattleService;

    private final String CREATE_TABLE_QUERY_FORMAT = "create table if not exists gamelogs_%s_%s\n" +
            "(\n" +
            "    id                 bigint auto_increment\n" +
            "        primary key,\n" +
            "    created_time       datetime      null,\n" +
            "    duration           bigint        not null,\n" +
            "    game_mode          int           not null,\n" +
            "    playerids          varchar(255)  null,\n" +
            "    player_info        varchar(255)  null,\n" +
            "    player_turn        varchar(2550) null,\n" +
            "    roomid             int           not null,\n" +
            "    score              int           not null,\n" +
            "    start_time         bigint        not null,\n" +
            "    winid              bigint        not null,\n" +
            "    win_info           varchar(255)  null,\n" +
            "    match_end_at_ms    bigint        null,\n" +
            "    match_round_count  int           null,\n" +
            "    rounds             longtext      charset utf8 collate utf8_general_ci  null,\n" +
            "    user_join_type     varchar(255)  null,\n" +
            "    user_sub_join_type varchar(255)  null\n" +
            ") collate = utf8_general_ci;";
    private final String INSERT_QUERY = "insert into gamelogs_%s_%s (created_time, duration, game_mode, playerids, player_info, player_turn, roomid, score,\n" +
            "                      start_time, winid, win_info, match_end_at_ms, match_round_count, rounds, user_join_type, user_sub_join_type)\n" +
            "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    private final String GET_BY_WIN_ID_QUERY = "select id from gamelogs_%s_%s where winid = ? order by created_time desc limit 1";

    public void createTable(){
        EntityTransaction transaction = null;
        EntityManager em = null;
        try {
            Date now = new Date();
            int month = DateTimeUtils.getMonth(now);
            int year = DateTimeUtils.getYear(now);
            String createTableQuery = String.format(CREATE_TABLE_QUERY_FORMAT, month, year);

            em = logEntityManagerFactory.createEntityManager();
            transaction = em.getTransaction();

            transaction.begin();
            var query = em.createNativeQuery(createTableQuery);
            query.executeUpdate();
            transaction.commit();
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, GameLogService.class));
        } finally {
            if (em != null){
                em.close();
            }
        }
    }

    public long getLatestId(long winId) {
        EntityManager em = null;
        try {
            Date now = new Date();
            int month = DateTimeUtils.getMonth(now);
            int year = DateTimeUtils.getYear(now);
            String getByWinIdQuery = String.format(GET_BY_WIN_ID_QUERY, month, year);
            em = logEntityManagerFactory.createEntityManager();
            Query query = em.createNativeQuery(getByWinIdQuery);
            query.setParameter(1, winId);
            return ((BigInteger) query.getSingleResult()).longValue();
        } catch (Exception ex) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, GameLogService.class));
        } finally {
            if (em != null){
                em.close();
            }
        }
        return -1;
    }

    public GameLog insertRecord(GameLog gameLog) {
        EntityTransaction transaction = null;
        EntityManager em = null;
        try {
            Date now = new Date();
            int month = DateTimeUtils.getMonth(now);
            int year = DateTimeUtils.getYear(now);
            String createTableQuery = String.format(INSERT_QUERY, month, year);

            em = logEntityManagerFactory.createEntityManager();
            transaction = em.getTransaction();

            transaction.begin();
            Query query = em.createNativeQuery(createTableQuery);
            query.setParameter(1, gameLog.getCreatedTime(), TemporalType.TIMESTAMP);
            query.setParameter(2, gameLog.getDuration());
            query.setParameter(3, gameLog.getGameMode());
            query.setParameter(4, gameLog.getPlayerIDS());
            query.setParameter(5, gameLog.getPlayerInfo());
            query.setParameter(6, "");
            query.setParameter(7, gameLog.getRoomID());
            query.setParameter(8, gameLog.getScore());
            query.setParameter(9, gameLog.getStartTime());
            query.setParameter(10, gameLog.getWinID());
            query.setParameter(11, gameLog.getWinInfo());
            query.setParameter(12, gameLog.getMatchEndAtMs());
            query.setParameter(13, gameLog.getMatchRoundCount());
            query.setParameter(14, gameLog.getRounds());
            query.setParameter(15, gameLog.getUserJoinType());
            query.setParameter(16, gameLog.getUserSubJoinType());
            query.executeUpdate();
            // Retrieve the generated ID
            Query idQuery = em.createNativeQuery("SELECT LAST_INSERT_ID()");
            long generatedId = ((Number) idQuery.getSingleResult()).longValue();
            gameLog.setId(generatedId); // assuming you have a setId method in your GameLog class
            transaction.commit();
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, GameLogService.class));
        } finally {
            if (em != null){
                em.close();
            }
            return gameLog;
        }
    }

    public String create(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        GameLog gameLog = JsonBuilder.jsonToGameLog(jsonObject);
        for (Long playerID : gameLog.getListUserID()) {
            if (!profileRepository.read().existsById(playerID)){
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), "Invalid data !", NetWorkAPI.BATTLE_LOG_POST);
            }
        }
        calculateGem(gameLog);
        leaderboardService.calculateEndGameData(gameLog);
        GMLocalQueue.addQueue(new EndGameCmd(gameLog, roomService,
                profileStatisticService, questEventService,
                dailyQuestService, rushArenaService, winBattleService));

        Set<Long> listID = new HashSet<>();
        listID.addAll(gameLog.getListUserID());
        Map<Long, Profile> mapProfile = profileService.getMapByListId(listID);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), JsonBuilder.buildGameLogJson(gameLog, mapProfile, false), NetWorkAPI.BATTLE_LOG_POST);
    }

    private void calculateGem(GameLog gameLog) {
        JsonArray jsonArray = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonArray playerTurnArr = jsonArray.get(i).getAsJsonObject().get("playerTurns").getAsJsonArray();
            for (JsonElement playerTurnEle : playerTurnArr) {
                JsonObject playerTurnJson = playerTurnEle.getAsJsonObject();
                int totalNormalGem = playerTurnJson.get("gemGreenCount").getAsInt() + playerTurnJson.get("gemPinkCount").getAsInt() + playerTurnJson.get("gemBlueCount").getAsInt()
                        + playerTurnJson.get("gemRedCount").getAsInt() + playerTurnJson.get("gemYellowCount").getAsInt();
                int totalSpecialGem = playerTurnJson.get("makeBoomCount").getAsInt() + playerTurnJson.get("makeHorizontalRocketCount").getAsInt()
                        + playerTurnJson.get("makeSpecial5Count").getAsInt() + playerTurnJson.get("makeVerticalRocketCount").getAsInt();
                int totalHealRegen = playerTurnJson.get("userHpRegen").getAsInt() + playerTurnJson.get("userHpUltiRegen").getAsInt();
                playerTurnJson.addProperty("totalNormalGem", totalNormalGem);
                playerTurnJson.addProperty("totalSpecialGem", totalSpecialGem);
                playerTurnJson.addProperty("totalHealRegen", totalHealRegen);
            }
        }
        gameLog.setRounds(jsonArray.toString());
    }

}
