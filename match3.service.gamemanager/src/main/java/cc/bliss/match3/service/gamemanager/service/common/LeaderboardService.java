/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.ent.common.LeaderBoardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.LeaderBoardMainTab;
import cc.bliss.match3.service.gamemanager.ent.common.LeaderBoardSubTab;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.enums.ELeaderBoardMainTab;
import cc.bliss.match3.service.gamemanager.ent.enums.EQuestType;
import cc.bliss.match3.service.gamemanager.ent.enums.ERank;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.*;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.PlayGameTrackingCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TrophyRoadTrackingCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.system.*;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class LeaderboardService extends BaseService {

    private static final String LEADER_BOARD_REDIS_KEY = "leaderboard";
    private static final String LEADER_BOARD_HERO_REDIS_KEY = "leaderboard_%s";
    private static final String LEADER_BOARD_CLAN_REDIS_KEY = "leaderboard_clan";
    private static final String HIGHEST_STREAK = "highest_streak";
    private static final String TIME_REACHED_TROPHY_KEY =  "time_reached_trophy";
    private static final String HASH_TROPHYROAD_DONE_USER = "trophyroad_done_%s";
    @Autowired
    private AdminService adminService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private ClanService clanService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private BotService botService;
    @Autowired
    private GameLogService gameLogService;
    @Autowired
    private DailyQuestService dailyQuestService;

    private String getLeaderboardHeroKey(int heroID) {
        return String.format(LEADER_BOARD_HERO_REDIS_KEY, heroID);
    }

    public void resetTrophy() {
        List<Integer> listHeroID = heroRepository.read().findAll().stream().map(e -> e.getId()).collect(Collectors.toList());
        resetTrophy(listHeroID);
    }

    public String editTrophy(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        Profile profile = profileService.getProfileByID(sessionObj.getId());
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int heroID = jsonObject.get("heroID").getAsInt();
        int delta = jsonObject.get("delta").getAsInt();
        if (profile.getHighestTrophy() < delta) {
            profile.setHighestTrophy(delta);
            profileRepository.write().updateHighestTrophy(profile.getId(), profile.getHighestTrophy());
        }
        redisTemplateString.opsForZSet().add(getLeaderboardHeroKey(heroID), String.valueOf(sessionObj.getId()), delta);
        redisTemplateString.opsForZSet().add(LEADER_BOARD_REDIS_KEY, String.valueOf(sessionObj.getId()), delta);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.LEADERBOARD_LIST);
    }

    public void resetTrophy(List<Integer> listHeroID) {
        for (Integer heroID : listHeroID) {
            String leaderboardHeroKey = getLeaderboardHeroKey(heroID);
            Set<TypedTuple<String>> typedTuples = redisTemplateString.opsForZSet().reverseRangeWithScores(leaderboardHeroKey, 0, -1);
            for (TypedTuple<String> typedTuple : typedTuples) {
                ERank eRank = ERank.findByTrophy(typedTuple.getScore().intValue());
                int delta = 0;
                switch (eRank) {
                    case CHALLENGER:
                        delta = typedTuple.getScore().intValue() - 980;
                        break;
                    case GRAND_MASTER:
                        delta = typedTuple.getScore().intValue() - ConvertUtils.toInt(typedTuple.getScore() * GameConstant.PERCENT_RESET_GRAND_MASTER / 100);
                        break;
                    case MASTER:
                        delta = typedTuple.getScore().intValue() - ConvertUtils.toInt(typedTuple.getScore() * GameConstant.PERCENT_RESET_MASTER / 100);
                        break;
                    default:
                        delta = typedTuple.getScore().intValue() - ConvertUtils.toInt(typedTuple.getScore() * GameConstant.PERCENT_RESET_REMAIN / 100);
                        break;
                }
                redisTemplateString.opsForZSet().incrementScore(leaderboardHeroKey, typedTuple.getValue(), -delta);
                redisTemplateString.opsForZSet().incrementScore(LEADER_BOARD_REDIS_KEY, typedTuple.getValue(), -delta);
            }
        }
    }

    private List<LeaderBoardEnt> getLeaderboardEnts(int mainTab, int subTab, long currentUserID) {
        Set<TypedTuple<String>> typedTuples = Collections.EMPTY_SET;
        if (mainTab == ELeaderBoardMainTab.HERO.ordinal()) {
            typedTuples = redisTemplateString.opsForZSet().reverseRangeWithScores(getLeaderboardHeroKey(subTab), 0, GameConstant.PAGE_SIZE_LEADERBOARD);
        } else if (mainTab == ELeaderBoardMainTab.PLAYER.ordinal()) {
            typedTuples = redisTemplateString.opsForZSet().reverseRangeWithScores(LEADER_BOARD_REDIS_KEY, 0, GameConstant.PAGE_SIZE_LEADERBOARD);
        } else if (mainTab == ELeaderBoardMainTab.CLAN.ordinal()) {
            typedTuples = redisTemplateString.opsForZSet().reverseRangeWithScores(LEADER_BOARD_CLAN_REDIS_KEY, 0, GameConstant.PAGE_SIZE_LEADERBOARD);
        }

        List<LeaderBoardEnt> leaderBoardEnts = new ArrayList<>();
        for (TypedTuple<String> typedTuple : typedTuples) {
            if (typedTuple.getScore().intValue() == 0 || typedTuple.getScore().intValue() < GameConstant.LEADERBOARD_TROPHY_REQUIRE) {
                continue;
            }
            long userId = ConvertUtils.toLong(typedTuple.getValue());
            long timeReachedTrophy = 0;
            if (redisTemplateString.opsForZSet().score(TIME_REACHED_TROPHY_KEY, String.valueOf(userId)) != null) {
                timeReachedTrophy = redisTemplateString.opsForZSet().score(TIME_REACHED_TROPHY_KEY, String.valueOf(userId)).longValue();
            }
            leaderBoardEnts.add(new LeaderBoardEnt(userId, typedTuple.getScore().intValue(), leaderBoardEnts.size(), timeReachedTrophy));
        }
        int currentRank;
        LeaderBoardEnt currentUser;
        if (leaderBoardEnts.stream().anyMatch(e -> e.getUserID() == currentUserID)) {
            currentUser = leaderBoardEnts.stream().filter(e -> e.getUserID() == currentUserID).findFirst().get();
        } else {
            currentRank = getRank(currentUserID, mainTab, subTab);
            Profile profile = profileService.getMinProfileByID(currentUserID);
            int clanID = profile.getClanID();
            if (mainTab == ELeaderBoardMainTab.CLAN.ordinal()) {
                int clanTrophy = getClanTrophy(ConvertUtils.toInt(currentUserID));
                currentRank = clanTrophy <= 0 ? -1 : currentRank;
                currentUser = new LeaderBoardEnt(clanID, clanTrophy, currentRank);
            } else if (mainTab == ELeaderBoardMainTab.PLAYER.ordinal()) {
                int trophy = getProfileTrophy(currentUserID);
                long timeReachedTrophy = timeReachedTrophy(currentUserID);
                currentRank = trophy <= 0 ? -1 : currentRank;
                currentUser = new LeaderBoardEnt(currentUserID, trophy, currentRank, timeReachedTrophy);
            } else {
                int trophy = getHeroTrophy(currentUserID, subTab);
                currentRank = trophy <= 0 ? -1 : currentRank;
                currentUser = new LeaderBoardEnt(currentUserID, trophy, currentRank);
            }
        }
        List<LeaderBoardEnt> sortedLeaderBoards =  leaderBoardEnts.stream().sorted(Comparator.comparing(LeaderBoardEnt::getTrophy, Comparator.reverseOrder()).
                thenComparing(LeaderBoardEnt::getTimeReachedTrophy)).collect(Collectors.toList());
        sortedLeaderBoards.add(currentUser);
        return sortedLeaderBoards;
    }

    private long timeReachedTrophy(long userId) {
        long timeReachedTrophy = 0;
        if (redisTemplateString.opsForZSet().score(TIME_REACHED_TROPHY_KEY, String.valueOf(userId)) != null) {
            timeReachedTrophy = redisTemplateString.opsForZSet().score(TIME_REACHED_TROPHY_KEY, String.valueOf(userId)).longValue();
        }
        return timeReachedTrophy;
    }

    public String getClanLeaderboard(int mainTab, int subTab, long currentUserID) {
        JsonArray jsonArray = new JsonArray();
        List<LeaderBoardEnt> leaderboards = getLeaderboardEnts(mainTab, subTab, currentUserID);
        Set<Integer> listID = leaderboards.stream().map(e -> ConvertUtils.toInt(e.getUserID())).collect(Collectors.toSet());
        Map<Integer, ClanInfo> map = clanService.getListClan(listID);
//        for (LeaderBoardEnt leaderboard : leaderboards) {
//            JsonObject object = buildLeaderBoardEnt(
//                    map.get(ConvertUtils.toInt(leaderboard.getUserID())), leaderboards.indexOf(leaderboard) + 1, leaderboard.getTrophy());
//            jsonArray.add(object);
//        }
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        response.addProperty("from", GameConstant.START_SS_MILIS);
        response.addProperty("to", GameConstant.END_SS_MILIS);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.LEADERBOARD_LIST);
    }

    private String getAllHeroLeaderBoard(long currentUserId) {
        List<LeaderBoardEnt> leaderBoardHeroes = getAllHeroLeaderBoardData(currentUserId);
        Set<Long> listID = leaderBoardHeroes.stream().map(e -> e.getUserID()).collect(Collectors.toSet());
        listID.add(currentUserId);
        Map<Long, Profile> mapProfile = profileService.getMapByListId(listID);
        JsonArray jsonArray = new JsonArray();
        for (LeaderBoardEnt leaderboard : leaderBoardHeroes) {
            JsonObject object = buildLeaderBoardEnt(leaderboard,
                    mapProfile.get(leaderboard.getUserID()), 1);
            jsonArray.add(object);
        }
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        response.addProperty("from", GameConstant.START_SS_MILIS);
        response.addProperty("to", GameConstant.END_SS_MILIS);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.LEADERBOARD_LIST);
    }

    private List<LeaderBoardEnt> getAllHeroLeaderBoardData(long currentUserID) {
        List<LeaderBoardEnt> leaderBoardEnts = new ArrayList<>();
        Set<TypedTuple<String>> typedTuples = Collections.EMPTY_SET;
        List<HeroEnt> heroEnts = heroRepository.read().findAll();
        for (HeroEnt heroEnt : heroEnts) {
            int heroId = heroEnt.getId();
            typedTuples = redisTemplateString.opsForZSet().reverseRangeWithScores(getLeaderboardHeroKey(heroId), 0, GameConstant.PAGE_SIZE_LEADERBOARD);
            for (TypedTuple<String> typedTuple : typedTuples) {
                if (typedTuple.getScore().intValue() > 0) {
                    long userId = ConvertUtils.toLong(typedTuple.getValue());
                    long timeReachedTrophy;
                    if (redisTemplateString.opsForZSet().score(TIME_REACHED_TROPHY_KEY, String.valueOf(userId)) != null) {
                        timeReachedTrophy = redisTemplateString.opsForZSet().score(TIME_REACHED_TROPHY_KEY, String.valueOf(userId)).longValue();
                        int currentRank = 0;
                        int trophy = getHeroTrophy(currentUserID, heroId);
                        currentRank = trophy <= 0 ? -1 : currentRank;
                        LeaderBoardEnt currentUser = new LeaderBoardEnt(currentUserID, trophy, currentRank);
                        leaderBoardEnts.add(new LeaderBoardEnt(userId, typedTuple.getScore().intValue(), leaderBoardEnts.size(), timeReachedTrophy, currentUser.getTrophy(), heroId));
                        break;
                    }
                }
            }
        }
        return leaderBoardEnts;
    }

    public String getLeaderboard(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int mainTab = jsonObject.get("mainTab").getAsInt();
        int subTab = jsonObject.get("subTab").getAsInt();
        if (mainTab == ELeaderBoardMainTab.CLAN.ordinal()) {
            return getClanLeaderboard(mainTab, subTab, session.getId());
        }
        if (mainTab == ELeaderBoardMainTab.HERO.ordinal() && subTab == 0) {
            return getAllHeroLeaderBoard(session.getId());
        }
        List<LeaderBoardEnt> leaderboards = getLeaderboardEnts(mainTab, subTab, session.getId());

        Set<Long> listID = leaderboards.stream().map(e -> e.getUserID()).collect(Collectors.toSet());
        listID.add(session.getId());
        Map<Long, Profile> mapProfile = profileService.getMapByListId(listID);

        JsonArray jsonArray = new JsonArray();
        for (LeaderBoardEnt leaderboard : leaderboards) {
            JsonObject object = buildLeaderBoardEnt(leaderboard,
                    mapProfile.get(leaderboard.getUserID()), leaderboard.getRank() == -1 ? -1 : leaderboards.indexOf(leaderboard) + 1);
            jsonArray.add(object);
        }
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        response.addProperty("from", GameConstant.START_SS_MILIS);
        response.addProperty("to", GameConstant.END_SS_MILIS);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.LEADERBOARD_LIST);
    }

    public List<LeaderBoardEnt> getListByIds(Collection<Long> longs) {
        List<LeaderBoardEnt> boardEnts = new ArrayList<>();
        for (Long l : longs) {
            LeaderBoardEnt boardEnt = new LeaderBoardEnt(l,
                    getProfileTrophy(l)
            );
            boardEnts.add(boardEnt);
        }
        return boardEnts;
    }

    public JsonObject buildLeaderBoardEnt(LeaderBoardEnt leaderboard, Profile profile, int rank) {
        JsonObject jsonObject = JsonBuilder.profileToJson(profile, leaderboard.getTrophy());
        jsonObject.addProperty("rank", rank);
        jsonObject.addProperty("myTrophy", leaderboard.getMyTrophy());
        jsonObject.addProperty("heroId", leaderboard.getHeroId());

        return jsonObject;
    }

    public JsonObject buildLeaderBoardEnt(ClanInfo clanInfo, int rank, int trophy) {
        if (clanInfo == null) {
            clanInfo = new ClanInfo();
            clanInfo.setId(-1);
        }
        JsonObject jsonObject = clanService.clanToJson(clanInfo, clanInfo.getSize(), 0);
        jsonObject.addProperty("rank", rank);
        jsonObject.addProperty("trophy", trophy);
        return jsonObject;
    }

    public void calculateEndGameData(GameLog gameLog) {
        long winID = gameLog.getWinID();
        if (winID == -1) {
            JsonArray winInfo = new JsonArray();
            for (Long playerID : gameLog.getListUserID()) {
                Profile profile = profileService.getMinProfileByID(playerID);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("userID", playerID);
                jsonObject.addProperty("trophy", getHeroTrophy(profile.getId(), profile.getSelectHero()));
                jsonObject.addProperty("deltaTrophy", 0);
                jsonObject.addProperty("bonusTrophy", 0);
                jsonObject.addProperty("winStreak", 0);
                jsonObject.addProperty("heroID", profile.getSelectHero());
                winInfo.add(jsonObject);
            }
            gameLog.setWinInfo(winInfo.toString());
            GameLog savedGameLog = gameLogService.insertRecord(gameLog);
            sendPlayGameMessage(gameLog, savedGameLog.getId(), 0, 0);
            return;
        }
        long duration = gameLog.getDuration();
        List<Long> listPlayer = gameLog.getListUserID();
        if (listPlayer.size() != 2) {
            return;
        }
        Profile winner = null;
        Profile loser = null;

        for (Long playerID : listPlayer) {
            if (playerID == winID) {
                winner = profileService.getMinProfileByID(playerID);
            } else {
                loser = profileService.getMinProfileByID(playerID);
            }
        }

        int deltaWinTrophy;
        int deltaLoserTrophy;
        int winTrophy = getHeroTrophy(winner.getId(), winner.getSelectHero());
        int loseTrophy = getHeroTrophy(loser.getId(), loser.getSelectHero());
        int bonusTrophy = 0;
        int winStreak = 0;
        int deltaTrophy = winTrophy - loseTrophy;
        deltaTrophy = deltaTrophy > 0 ? -deltaTrophy : deltaTrophy;
        boolean lowerTrophyWin = winTrophy > loseTrophy;
        if (lowerTrophyWin) {
            deltaWinTrophy = getWinTrophy(winTrophy, deltaTrophy, true);
            deltaLoserTrophy = getLoseTrophy(loseTrophy, deltaTrophy, true);
        } else {
            deltaWinTrophy = getWinTrophy(winTrophy, deltaTrophy, false);
            deltaLoserTrophy = getLoseTrophy(loseTrophy, deltaTrophy, false);
        }
        if (deltaLoserTrophy > loseTrophy) {
            deltaLoserTrophy = loseTrophy;
        }
        // update profile
        if (duration <= GameConstant.DOMINATING_TIME) {
            winner.setDominateWin(winner.getDominateWin() + 1);
        }
        winner.setBattleWon(winner.getBattleWon() + 1);
        winner.setLoseStreak(0);
        boolean isNotTutorial = !botService.isBotTutorial(loser.getId());
        if (isNotTutorial){
            winStreak = winner.getWinStreak() + 1;
            winner.setWinStreak(winStreak);
        }
        bonusTrophy = getBonusTrophy(winStreak);
        if (gameLog.getHpInfo().getOrDefault(winner.getId(), 0) == GameConstant.MAX_HP) {
            winner.setGodLikeWin(winner.getGodLikeWin() + 1);
        }
        GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, winner.getId(), EQuestType.COLLECT_TROPHY, deltaWinTrophy + bonusTrophy));
        recordLeaderboard(winner.getId(), deltaWinTrophy + bonusTrophy, loser.getId(), deltaLoserTrophy, winStreak);
        int totalWinTrophy = getProfileTrophy(winID);
        if (winner.getHighestTrophy() < totalWinTrophy) {
            winner.setHighestTrophy(totalWinTrophy);
        }
        int totalStreak = getProfileHighestStreak(winID);
        if (winner.getHighestStreak() < totalStreak) {
            winner.setHighestStreak(totalStreak);
        }
        loser.setWinStreak(0);
        int loseStreak = loser.getLoseStreak() + 1;
        loser.setLoseStreak(loseStreak);
        JsonArray winInfo = new JsonArray();
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("userID", winID);
            jsonObject.addProperty("trophy", winTrophy);
            jsonObject.addProperty("deltaTrophy", deltaWinTrophy);
            jsonObject.addProperty("bonusTrophy", bonusTrophy);
            jsonObject.addProperty("winStreak", winStreak);
            winInfo.add(jsonObject);
        }
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("userID", loser.getId());
            jsonObject.addProperty("trophy", loseTrophy);
            jsonObject.addProperty("deltaTrophy", -deltaLoserTrophy);
            jsonObject.addProperty("bonusTrophy", 0);
            jsonObject.addProperty("winStreak", 0);
            winInfo.add(jsonObject);
        }
        gameLog.setWinInfo(winInfo.toString());
        if (winner.getBotType() == 0) {
            profileRepository.write().updateEndGame(winner.getId(),
                    winner.getDominateWin(),
                    winner.getBattleWon(),
                    winner.getLoseStreak(),
                    winner.getWinStreak(),
                    winner.getGodLikeWin(),
                    winner.getHighestTrophy(),
                    winner.getHighestStreak());
        }
        if (loser.getBotType() == 0){
            profileRepository.write().updateEndGame(loser.getId(),
                    loser.getDominateWin(),
                    loser.getBattleWon(),
                    loser.getLoseStreak(),
                    loser.getWinStreak(),
                    loser.getGodLikeWin(),
                    loser.getHighestTrophy(),
                    loser.getHighestStreak());
        }
        GameLog savedGameLog = gameLogService.insertRecord(gameLog);
        sendTrophyRoadDoneAction(winner, totalWinTrophy);
        sendPlayGameMessage(gameLog, savedGameLog.getId(), deltaWinTrophy, deltaLoserTrophy);
    }

    //only use for tracking data, consider if reuse this function
    private void sendPlayGameMessage(GameLog gameLog, long gameLogId, int deltaWinTrophy, int deltaLoserTrophy) {
        JsonArray jsonArray = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonArray playerTurnArr = jsonArray.get(i).getAsJsonObject().get("playerTurns").getAsJsonArray();
            for (JsonElement playerTurnEle : playerTurnArr) {
                JsonObject playerTurnJson = playerTurnEle.getAsJsonObject();
                long winID = gameLog.getWinID();
                int playerId = playerTurnJson.get("playerId").getAsInt();
                //get component info
                long componentUserId = 0;
                String componentUserDisplayName = "";
                String componentBotType = "";
                List<Long> listPlayer = gameLog.getListUserID();
                for (Long playerID : listPlayer) {
                    if (playerId != playerID) {
                        Profile user = profileService.getMinProfileByID(playerID);
                        componentUserDisplayName = user.getDisplayName();
                        componentUserId = user.getId();
                        componentBotType = EBotType.fromValue(user.getBotType()).name();
                    }
                }

                Profile user = profileService.getMinProfileByID(playerId);
                int userTrophy = getProfileTrophy(playerId);
                int matchResult = winID == -1 ? 0 : (winID == playerId ? 1 : -1);
                int userWinLoseTrophy = winID == -1 ? 0 : (winID == playerId ? deltaWinTrophy : -deltaLoserTrophy);
                JsonObject trackingGameLog = new JsonObject();
//                trackingGameLog.addProperty("componentUserDisplayName", playerTurnJson.get("componentUserDisplayName").getAsString());
//                trackingGameLog.addProperty("componentUserId", playerTurnJson.get("componentUserId").getAsString());
                trackingGameLog.addProperty("componentUserDisplayName", componentUserDisplayName);
                trackingGameLog.addProperty("componentUserId", componentUserId);
                trackingGameLog.addProperty("explodeBoomCount", playerTurnJson.get("explodeBoomCount").getAsInt());
                trackingGameLog.addProperty("explodeDoubleSpecialCount", playerTurnJson.get("explodeDoubleSpecialCount").getAsInt());
                trackingGameLog.addProperty("explodeHorizontalRocketCount", playerTurnJson.get("explodeHorizontalRocketCount").getAsInt());
                trackingGameLog.addProperty("explodeSpecial5Count", playerTurnJson.get("explodeSpecial5Count").getAsInt());
                trackingGameLog.addProperty("explodeVerticalRocketCount", playerTurnJson.get("explodeVerticalRocketCount").getAsInt());
                trackingGameLog.addProperty("gemGreenCount", playerTurnJson.get("gemGreenCount").getAsInt());
                trackingGameLog.addProperty("gemPinkCount", playerTurnJson.get("gemPinkCount").getAsInt());
                trackingGameLog.addProperty("gemBlueCount", playerTurnJson.get("gemBlueCount").getAsInt());
                trackingGameLog.addProperty("gemRedCount", playerTurnJson.get("gemRedCount").getAsInt());
                trackingGameLog.addProperty("gemYellowCount", playerTurnJson.get("gemYellowCount").getAsInt());
                trackingGameLog.addProperty("makeBoomCount", playerTurnJson.get("makeBoomCount").getAsInt());
                trackingGameLog.addProperty("makeHorizontalRocketCount", playerTurnJson.get("makeHorizontalRocketCount").getAsInt());
                trackingGameLog.addProperty("makeSpecial5Count", playerTurnJson.get("makeSpecial5Count").getAsInt());
                trackingGameLog.addProperty("makeVerticalRocketCount", playerTurnJson.get("makeVerticalRocketCount").getAsInt());
                trackingGameLog.addProperty("matchDurationMs", gameLog.getDuration() * 1000L);
                trackingGameLog.addProperty("matchEndAtMs", gameLog.getMatchEndAtMs());
                trackingGameLog.addProperty("matchFindDurationMs", playerTurnJson.get("matchFindDurationMs").getAsLong());
                trackingGameLog.addProperty("matchId", gameLogId);
                trackingGameLog.addProperty("merge3Count", playerTurnJson.get("merge3Count").getAsInt());
                trackingGameLog.addProperty("sessionId", playerId);
                trackingGameLog.addProperty("userComboCount", playerTurnJson.get("userComboCount").getAsInt());
                trackingGameLog.addProperty("userComboDmg", playerTurnJson.get("userComboDmg").getAsInt());
                trackingGameLog.addProperty("userDmg", playerTurnJson.get("userDmg").getAsInt());
                trackingGameLog.addProperty("userHardLockCause", playerTurnJson.get("userHardLockCause").getAsInt());
                trackingGameLog.addProperty("userHardLockUnlock", playerTurnJson.get("userHardLockUnlock").getAsInt());
                trackingGameLog.addProperty("userHardLockedCount", playerTurnJson.get("userHardLockedCount").getAsInt());
                trackingGameLog.addProperty("userHighestComboCount", playerTurnJson.get("userHighestComboCount").getAsInt());
                trackingGameLog.addProperty("userHeroId", playerTurnJson.get("userHeroId").getAsInt());
                trackingGameLog.addProperty("userHeroLevel", playerTurnJson.get("userHeroLevel").getAsInt());
                trackingGameLog.addProperty("userHeroName", playerTurnJson.get("userHeroName").getAsString());
                trackingGameLog.addProperty("userHeroSkillLevel", playerTurnJson.get("userHeroSkillLevel").getAsInt());
                trackingGameLog.addProperty("userHeroTrophy", playerTurnJson.get("userHeroTrophy").getAsInt());
                trackingGameLog.addProperty("userHpRegen", playerTurnJson.get("userHpRegen").getAsInt());
                trackingGameLog.addProperty("userHpRemain", playerTurnJson.get("userHpRemain").getAsInt());
                trackingGameLog.addProperty("userHpUltiRegen", playerTurnJson.get("userHpUltiRegen").getAsInt());
                trackingGameLog.addProperty("userJoinType", gameLog.getUserJoinType());
                trackingGameLog.addProperty("userManaRegen", playerTurnJson.get("userManaRegen").getAsInt());
                trackingGameLog.addProperty("userMediumLockCause", playerTurnJson.get("userMediumLockCause").getAsInt());
                trackingGameLog.addProperty("userMediumLockUnlock", playerTurnJson.get("userMediumLockUnlock").getAsInt());
                trackingGameLog.addProperty("userMediumLockedCount", playerTurnJson.get("userMediumLockedCount").getAsInt());
                trackingGameLog.addProperty("matchResult", matchResult);
                trackingGameLog.addProperty("userSubJoinType", gameLog.getUserSubJoinType());
                trackingGameLog.addProperty("userUltiCount", playerTurnJson.get("userUltiCount").getAsInt());
                trackingGameLog.addProperty("userUltiDmg", playerTurnJson.get("userUltiDmg").getAsInt());
                trackingGameLog.addProperty("userWinLoseTrophy", userWinLoseTrophy);
                trackingGameLog.addProperty("roundId", playerTurnJson.get("roundId").getAsInt());
                trackingGameLog.addProperty("roundResult", playerTurnJson.get("roundResult").getAsInt());
                trackingGameLog.addProperty("roundDurationMs", playerTurnJson.get("roundDurationMs").getAsLong());
                trackingGameLog.addProperty("matchRoundCount", gameLog.getMatchRoundCount());
                trackingGameLog.addProperty("componentBotType", componentBotType);
                trackingGameLog.addProperty("mergeLightXHrocket", playerTurnJson.get("mergeLightXHrocket").getAsInt());
                trackingGameLog.addProperty("mergeLightXVrocket", playerTurnJson.get("mergeLightXVrocket").getAsInt());
                trackingGameLog.addProperty("mergeLightXBomb", playerTurnJson.get("mergeLightXBomb").getAsInt());
                trackingGameLog.addProperty("mergeRocketXRocket", playerTurnJson.get("mergeRocketXRocket").getAsInt());
                trackingGameLog.addProperty("mergeBombXBomb", playerTurnJson.get("mergeBombXBomb").getAsInt());
                trackingGameLog.addProperty("mergeLightXLight", playerTurnJson.get("mergeLightXLight").getAsInt());
                trackingGameLog.addProperty("mergeBombXVrocket", playerTurnJson.get("mergeBombXVrocket").getAsInt());
                trackingGameLog.addProperty("mergeBombXHrocket", playerTurnJson.get("mergeBombXHrocket").getAsInt());
                GMLocalQueue.addQueue(new PlayGameTrackingCmd(producer, user, userTrophy, trackingGameLog, redisTemplateString));
            }
        }
    }

    private void sendTrophyRoadDoneAction(Profile user, int userTrophy) {
        List<TrophyRoadMileStoneEnt> listTrophyRoadMileStone = trophyRoadRepository.read().findAll(Sort.by("milestone"));
        for (TrophyRoadMileStoneEnt roadMileStoneEnt : listTrophyRoadMileStone) {
            Map<String, String> mapTrophyRoad = hashOperations.entries(getTrophyRoadKey(user.getId()));
            if (userTrophy >= roadMileStoneEnt.getMilestone()) {
                if (!mapTrophyRoad.containsKey(String.valueOf(roadMileStoneEnt.getId()))) {
                    hashOperations.increment(getTrophyRoadKey(user.getId()), String.valueOf(roadMileStoneEnt.getId()), 1);
                    GMLocalQueue.addQueue(new TrophyRoadTrackingCmd(producer, user, userTrophy, roadMileStoneEnt.getMilestone(), 4, roadMileStoneEnt.getRewards(), redisTemplateString));
                }
            }
        }
    }

    private String getTrophyRoadKey(long userID){
        return String.format(HASH_TROPHYROAD_DONE_USER, userID);
    }

    public int getProfileTrophy(long id) {
        Double l = redisTemplateString.opsForZSet().score(LEADER_BOARD_REDIS_KEY, String.valueOf(id));
        return l == null ? 0 : l.intValue();
    }

    public int getProfileHighestStreak(long id) {
        Double l = redisTemplateString.opsForZSet().score(HIGHEST_STREAK, String.valueOf(id));
        return l == null ? 0 : l.intValue();
    }

    public int getHeroTrophy(long id, int heroID) {
        String leaderboardHeroKey = getLeaderboardHeroKey(heroID);
        Double l = redisTemplateString.opsForZSet().score(leaderboardHeroKey, String.valueOf(id));
        return l == null ? 0 : l.intValue();
    }

    public Map<Integer, Integer> getHeroTrophy(long id, Collection<Integer> heroIDs) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Integer heroID : heroIDs) {
            String leaderboardHeroKey = getLeaderboardHeroKey(heroID);
            Double l = redisTemplateString.opsForZSet().score(leaderboardHeroKey, String.valueOf(id));
            map.put(heroID, l == null ? 0 : l.intValue());
        }
        return map;
    }

    public int getRank(long id, int mainTab, int subTab) {
        if (mainTab == ELeaderBoardMainTab.HERO.ordinal()) {
            return ConvertUtils.toInt(redisTemplateString.opsForZSet().reverseRank(getLeaderboardHeroKey(subTab), String.valueOf(id)));
        } else if (mainTab == ELeaderBoardMainTab.PLAYER.ordinal()) {
            return ConvertUtils.toInt(redisTemplateString.opsForZSet().reverseRank(LEADER_BOARD_REDIS_KEY, String.valueOf(id)));
        } else if (mainTab == ELeaderBoardMainTab.CLAN.ordinal()) {
            return ConvertUtils.toInt(redisTemplateString.opsForZSet().reverseRank(LEADER_BOARD_CLAN_REDIS_KEY, String.valueOf(id)));
        }
        return -1;
    }

    public TypedTuple<String> getTopTrophy(int heroID) {
        String leaderboardHeroKey = getLeaderboardHeroKey(heroID);
        Set<TypedTuple<String>> set = redisTemplateString.opsForZSet().reverseRangeWithScores(leaderboardHeroKey, 0, 1);
        for (TypedTuple<String> typedTuple : set) {
            return typedTuple;
        }
        return TypedTuple.of("0", 0d);
    }

    public void recordLeaderboard(long winner, int winTrophy, long loser, int loserTrophy, int highestStreak) {
        long currentTime = System.nanoTime();
        redisTemplateString.opsForZSet().incrementScore(LEADER_BOARD_REDIS_KEY, String.valueOf(winner), winTrophy);
        redisTemplateString.opsForZSet().add(HIGHEST_STREAK, String.valueOf(winner), highestStreak);
        redisTemplateString.opsForZSet().add(TIME_REACHED_TROPHY_KEY, String.valueOf(winner), currentTime);
        {
            Profile profile = profileService.getProfileByID(winner);
            redisTemplateString.opsForZSet().incrementScore(getLeaderboardHeroKey(profile.getSelectHero()), String.valueOf(winner), winTrophy);
        }
        redisTemplateString.opsForZSet().incrementScore(LEADER_BOARD_REDIS_KEY, String.valueOf(loser), -loserTrophy);
        redisTemplateString.opsForZSet().add(TIME_REACHED_TROPHY_KEY, String.valueOf(loser), currentTime);
        {
            Profile profile = profileService.getProfileByID(loser);
            redisTemplateString.opsForZSet().incrementScore(getLeaderboardHeroKey(profile.getSelectHero()), String.valueOf(loser), -loserTrophy);
        }
    }

    public void deleteLeaderboard(List<HeroEnt> listHero, long userID){
        redisTemplateString.opsForZSet().remove(LEADER_BOARD_REDIS_KEY, String.valueOf(userID));
        for (HeroEnt heroEnt : listHero) {
            redisTemplateString.opsForZSet().remove(getLeaderboardHeroKey(heroEnt.getId()), String.valueOf(userID));
        }
    }

    /**
     * Refresh clan trophy in leaderboard
     */
    public void updateLeaderboardClan() {
        List<LeaderBoardEnt> leaderBoardEnts = new ArrayList<>();
        List<ClanInfo> clanInfos = clanRepository.read().findAll();
        for (ClanInfo clanInfo : clanInfos) {
            int clanID = clanInfo.getId();
            int score = getClanTrophy(clanID);
            leaderBoardEnts.add(new LeaderBoardEnt(clanID, score));
        }
        redisTemplateString.delete(LEADER_BOARD_CLAN_REDIS_KEY);
        for (LeaderBoardEnt leaderBoardEnt : leaderBoardEnts) {
            redisTemplateString.opsForZSet().add(LEADER_BOARD_CLAN_REDIS_KEY, String.valueOf(leaderBoardEnt.getUserID()), leaderBoardEnt.getTrophy());
        }
    }

    private int getClanTrophy(int clanID) {
        List<ClanMember> clanMembers = clanMemberRepository.read().findByClanID(clanID);
        int score = 0;
        for (ClanMember clanMember : clanMembers) {
            score += getProfileTrophy(clanMember.getUserID());
        }
        return score;
    }

    private int getWinTrophy(int trophy, int delta, boolean isHigh) {
        int result = 0;
        if (isHigh) {
            result = 10 + Math.round(delta / 100);
        } else {
            result = 10 - Math.round(delta / 100);
        }
        return result < 0 ? 0 : result;
    }

    private int getLoseTrophy(int trophy, int delta, boolean isHigh) {
        int result = 0;
        if (trophy < 200) {
            if (isHigh) {
                result = 1 + Math.round(delta / 100);
            } else {
                result = 1 - Math.round(delta / 100);
            }
        } else if (trophy >= 200 && trophy < 500) {
            if (isHigh) {
                result = 4 + Math.round(delta / 100);
            } else {
                result = 4 - Math.round(delta / 100);
            }
        } else if (trophy >= 500 && trophy < 1000) {
            if (isHigh) {
                result = 6 + Math.round(delta / 100);
            } else {
                result = 6 - Math.round(delta / 100);
            }
        } else {
            if (isHigh) {
                result = 8 + Math.round(delta / 100);
            } else {
                result = 8 - Math.round(delta / 100);
            }
        }
        return result < 0 ? 0 : result;
    }

    private int getBonusTrophy(int winStreak) {
        if (winStreak >= 2 && winStreak < 5) {
            return 1;
        } else if (winStreak >= 5 && winStreak < 8) {
            return 2;
        } else if (winStreak >= 8 && winStreak < 11) {
            return 3;
        } else if (winStreak >= 11 && winStreak < 16) {
            return 4;
        } else if (winStreak >= 16) {
            return 5;
        }
        return 0;
    }

    public String getLeaderboardByID(String id) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public String getLeaderboardInfo() {
        SessionObj session = adminService.getSession();
        JsonArray jsonArray = new JsonArray();
        List<LeaderBoardMainTab> list = new ArrayList<>();
        {
            LeaderBoardMainTab boardMainTab = new LeaderBoardMainTab();
            boardMainTab.setId(ELeaderBoardMainTab.PLAYER.ordinal());
            boardMainTab.setTitle(ELeaderBoardMainTab.PLAYER.name());
            list.add(boardMainTab);
        }
        {
            LeaderBoardMainTab boardMainTab = new LeaderBoardMainTab();
            boardMainTab.setId(ELeaderBoardMainTab.CLAN.ordinal());
            boardMainTab.setTitle(ELeaderBoardMainTab.CLAN.name());
            list.add(boardMainTab);
        }
        {
            LeaderBoardMainTab boardMainTab = new LeaderBoardMainTab();
            boardMainTab.setId(ELeaderBoardMainTab.HERO.ordinal());
            boardMainTab.setTitle(ELeaderBoardMainTab.HERO.name());
            List<HeroEnt> listHero = heroRepository.read().findAll();
            List<LeaderBoardSubTab> boardSubTabs = new ArrayList<>();
            for (HeroEnt heroEnt : listHero) {
                LeaderBoardSubTab boardSubTab = new LeaderBoardSubTab();
                boardSubTab.setId(heroEnt.getId());
                boardSubTab.setHeroImg("");
                boardSubTab.setHeroName(heroEnt.getTitle());
                TypedTuple<String> typedTuple = getTopTrophy(heroEnt.getId());
                // top
                int topId = ConvertUtils.toInt(typedTuple.getValue());
                int topTrophy = ConvertUtils.toInt(typedTuple.getScore());
                if (topId > 0) {
                    Profile top = profileService.getMinProfileByID(topId);
                    boardSubTab.setTopTrophy(topTrophy);
                    boardSubTab.setTopImg(top.getAvatarPath());
                    boardSubTab.setTopName(top.getUsername());
                } else {
                    boardSubTab.setTopTrophy(0);
                    boardSubTab.setTopImg("");
                    boardSubTab.setTopName("");
                }
                // current
                long currentID = session.getId();
                int currentTrophy = getHeroTrophy(currentID, heroEnt.getId());
                boardSubTab.setCurrentTrophy(currentTrophy);
                boardSubTab.setMainGem(heroEnt.getHeroClass().ordinal());
                boardSubTabs.add(boardSubTab);
            }
            Comparator<LeaderBoardSubTab> comparator = Comparator.comparingInt(e -> e.getCurrentTrophy());
            Comparator<LeaderBoardSubTab> comparator1 = Comparator.comparingInt(e -> e.getId());
            Collections.sort(boardSubTabs, comparator.reversed().thenComparing(comparator1));
            boardMainTab.setSubTab(boardSubTabs);
            list.add(boardMainTab);
        }
        for (LeaderBoardMainTab leaderBoardMainTab : list) {
            jsonArray.add(buildLeaderboardInfo(leaderBoardMainTab));
        }
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.LEADERBOARD_LIST);
    }

    private JsonObject buildLeaderboardInfo(LeaderBoardMainTab boardMainTab) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", boardMainTab.getId());
        jsonObject.addProperty("title", boardMainTab.getTitle());
        jsonObject.add("subTab", buildLeaderboardSubTab(boardMainTab.getSubTab()));
        return jsonObject;
    }

    private JsonArray buildLeaderboardSubTab(List<LeaderBoardSubTab> list) {
        JsonArray jsonArray = new JsonArray();
        for (LeaderBoardSubTab leaderBoardSubTab : list) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", leaderBoardSubTab.getId());
            jsonObject.addProperty("heroName", leaderBoardSubTab.getHeroName());
            jsonObject.addProperty("heroImg", leaderBoardSubTab.getHeroImg());

            jsonObject.addProperty("topTrophy", leaderBoardSubTab.getTopTrophy());
            jsonObject.addProperty("topImg", leaderBoardSubTab.getTopImg());
            jsonObject.addProperty("topName", leaderBoardSubTab.getTopName());

            jsonObject.addProperty("currentTrophy", leaderBoardSubTab.getCurrentTrophy());
            jsonObject.addProperty("mainGem", leaderBoardSubTab.getMainGem());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

}
