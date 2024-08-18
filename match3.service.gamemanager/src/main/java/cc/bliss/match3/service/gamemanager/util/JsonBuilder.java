/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.util;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.*;
import cc.bliss.match3.service.gamemanager.ent.enums.EBadge;
import cc.bliss.match3.service.gamemanager.ent.enums.ELinkSocialStatus;
import cc.bliss.match3.service.gamemanager.ent.enums.ERank;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TrophyRoadMileStoneEnt;
import cc.bliss.match3.service.gamemanager.service.system.SSEService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
public class JsonBuilder {

    public static String buildSSEUpdateMoney(long delta, long before, long after, int type) {
        JsonObject jsonObject = buildSSEUpdateMoneyJson(delta, before, after, type);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.UPDATE_MONEY);
    }

    public static JsonObject buildSSEUpdateMoneyJson(RewardEnt rewardEnt){
        return buildSSEUpdateMoneyJson(rewardEnt.getDelta(), rewardEnt.getBefore(), rewardEnt.getAfter(), rewardEnt.getERewardType().getValue());
    }

    private static JsonObject buildSSEUpdateMoneyJson(long delta, long before, long after, int type){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", type);
        jsonObject.addProperty("delta", delta);
        jsonObject.addProperty("before", before);
        jsonObject.addProperty("after", after);
        return jsonObject;
    }

    public static JsonObject buildLinkSocialResponse(ELinkSocialStatus linkSocialStatus, Profile... profiles) {
        JsonObject response = new JsonObject();
        response.addProperty("status", linkSocialStatus.ordinal());

        JsonArray data = new JsonArray();
        for (Profile profile : profiles) {
            data.add(profileToJson(profile));
        }
        response.add("data", data);
        return response;
    }

    public static JsonObject buildGameLogJson(GameLog gameLog, Map<Long, Profile> mapProfile, boolean setJson) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", gameLog.getId());
        jsonObject.addProperty("roomID", gameLog.getRoomID());
        jsonObject.addProperty("duration", gameLog.getDuration());
        jsonObject.addProperty("playerIDS", gameLog.getPlayerIDS());
        jsonObject.addProperty("winID", gameLog.getWinID());
        jsonObject.addProperty("score", gameLog.getScore());
        jsonObject.addProperty("startTime", gameLog.getStartTime());
        jsonObject.addProperty("winInfo", gameLog.getWinInfo());
        jsonObject.addProperty("gameMode", gameLog.getGameMode());
        jsonObject.addProperty("createdTime", gameLog.getCreatedTime().getTime());

        if (setJson) {
            JsonArray jsonArray;
            if (gameLog.getRounds().startsWith("[") && gameLog.getRounds().endsWith("]")) {
                jsonArray = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
            } else {
                jsonArray = new JsonArray();
            }
            jsonObject.add("rounds", jsonArray);
        } else {
            jsonObject.addProperty("rounds", gameLog.getRounds());
        }

        JsonArray playerInfo = new JsonArray();
        JsonArray heroInfo = new JsonArray();
        for (Long userID : gameLog.getListUserID()) {
            Profile profile = mapProfile.get(userID);
            if (profile == null) {
                continue;
            }
            JsonObject jo = JsonBuilder.profileToJson(profile);
            playerInfo.add(jo);

            JsonObject hero = new JsonObject();
            hero.addProperty("userId", userID);
            hero.addProperty("hero", profile.getSelectHero());
            hero.add("skill", new JsonArray());
            heroInfo.add(hero);
        }
        jsonObject.addProperty("playerInfo", playerInfo.toString());
        jsonObject.addProperty("heroInfo", heroInfo.toString());
        return jsonObject;
    }

    public static JsonArray buildListReward(List<RewardEnt> rewardEnts) {
        JsonArray jsonArray = new JsonArray();
        for (RewardEnt rewardEnt : rewardEnts) {
            jsonArray.add(buildReward(rewardEnt));
        }
        return jsonArray;
    }

    public static JsonObject buildReward(RewardEnt rewardEnt) {
        JsonObject jsonObject = new JsonObject();
        if (rewardEnt.getERewardType().getValue() == ERewardType.BEGINNER_CHEST.getValue()){
            jsonObject.addProperty("rewardType", ERewardType.WOODEN_CHEST.getValue());
        } else {
            jsonObject.addProperty("rewardType", rewardEnt.getERewardType().getValue());
        }
        jsonObject.addProperty("title", rewardEnt.getTitle());
        jsonObject.addProperty("description", rewardEnt.getDescription());
        jsonObject.addProperty("img", rewardEnt.getImg());
        jsonObject.addProperty("delta", rewardEnt.getDelta());
        jsonObject.addProperty("after", rewardEnt.getAfter());
        jsonObject.addProperty("before", rewardEnt.getBefore());
        jsonObject.addProperty("refID", rewardEnt.getRef());
        return jsonObject;
    }

    public static JsonObject buildTrophyRoad(
            List<TrophyRoadMileStoneEnt> listTrophyRoadMileStone,
            List<TrophyRoadMileStoneEnt> listTrophyRoadAds,
            Profile profile,
            Map<String, String> mapTrophyRoad) {
        int curTrophy = profile.getTrophy();
        int highestTrophy = profile.getHighestTrophy();
        JsonObject body = new JsonObject();
        JsonArray trophyRoad = new JsonArray();
        for (TrophyRoadMileStoneEnt trophyRoadMileStoneEnt : listTrophyRoadMileStone) {
            JsonObject trophyRoadMileStoneJson = new JsonObject();
            trophyRoadMileStoneJson.addProperty("id", trophyRoadMileStoneEnt.getId());
            trophyRoadMileStoneJson.addProperty("milestone", trophyRoadMileStoneEnt.getMilestone());
            trophyRoadMileStoneJson.addProperty("isClaimed", mapTrophyRoad.containsKey(String.valueOf(trophyRoadMileStoneEnt.getId())));
            trophyRoadMileStoneJson.add("rewards", buildListReward(trophyRoadMileStoneEnt.getRewards()));
            trophyRoad.add(trophyRoadMileStoneJson);
        }
        body.add("trophyRoad", trophyRoad);
        body.addProperty("curTrophy", curTrophy);
        body.addProperty("highestTrophy", highestTrophy);
        body.addProperty("start", ModuleConfig.TROPHY_ROAD_START_TIME_MILLISECOND);
        body.addProperty("end", ModuleConfig.TROPHY_ROAD_END_TIME_MILLISECOND);
        JsonArray trophyRoadAds = new JsonArray();
        for (TrophyRoadMileStoneEnt trophyRoadMileStoneEnt : listTrophyRoadAds) {
            JsonObject trophyRoadMileStoneJson = new JsonObject();
            trophyRoadMileStoneJson.addProperty("id", trophyRoadMileStoneEnt.getId());
            trophyRoadMileStoneJson.addProperty("milestone", trophyRoadMileStoneEnt.getMilestone());
            trophyRoadMileStoneJson.addProperty("isClaimed", mapTrophyRoad.containsKey(String.valueOf(trophyRoadMileStoneEnt.getId())));
            trophyRoadMileStoneJson.add("rewards", buildListReward(trophyRoadMileStoneEnt.getRewards()));
            trophyRoadAds.add(trophyRoadMileStoneJson);
        }
        body.add("trophyRoadAds", trophyRoadAds);
        body.addProperty("removeAdsEndTime", profile.getTrophyRoadTicketExpired() != null ?
                profile.getTrophyRoadTicketExpired().getTime():
                0);

        JsonObject removeAdsPack = new JsonObject();
        removeAdsPack.addProperty("id", ERewardType.REMOVE_ADS_TROPHY_ROAD_PACK.getValue());
        removeAdsPack.addProperty("instantReward", 1000);
        removeAdsPack.addProperty("dailyReward", 90);
        removeAdsPack.addProperty("deltaTime", Duration.ofDays(30).toMillis());
        removeAdsPack.addProperty("price", 10);
        body.add("removeAdsPack", removeAdsPack);
        body.add("endTrophy", buildEndTrophy());
        body.addProperty("isShowEndTrophy", false);
        return body;
    }

    public static JsonObject buildEndTrophy(){
        JsonObject jsonObject = new JsonObject();
        {
            jsonObject.addProperty("startTime", System.currentTimeMillis() + Duration.ofHours(10).toMillis());
            jsonObject.addProperty("endTime", System.currentTimeMillis() + Duration.ofHours(20).toMillis());
        }
        {
            jsonObject.addProperty("seasonTrophy", 535);
            jsonObject.addProperty("battleEngaged", 10);
            jsonObject.addProperty("winTreak", 5);
            jsonObject.addProperty("winRate", 50.55f);
        }
        {
            jsonObject.addProperty("strongestHeroID", 1);
            jsonObject.addProperty("strongestHeroTrophy", 222);
            jsonObject.addProperty("strongestHeroName", "Aaron");
        }
        {
            jsonObject.addProperty("bestPerformanceHeroID", 2);
            jsonObject.addProperty("bestPerformanceHeroWinRate", 60.66f);
            jsonObject.addProperty("bestPerformanceHeroName", "Lily");
        }
        {
            jsonObject.addProperty("newSeasonNumber", 2);
            jsonObject.addProperty("newSeasonYear", 2024);
            JsonArray heroResetTrophy = new JsonArray();
            {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("id", 1);
                jsonObject1.addProperty("trophy", 90);
                heroResetTrophy.add(jsonObject1);
            }
            {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("id", 2);
                jsonObject1.addProperty("trophy", 90);
                heroResetTrophy.add(jsonObject1);
            }
            {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("id", 3);
                jsonObject1.addProperty("trophy", 90);
                heroResetTrophy.add(jsonObject1);
            }
            {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("id", 4);
                jsonObject1.addProperty("trophy", 90);
                heroResetTrophy.add(jsonObject1);
            }
            {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("id", 5);
                jsonObject1.addProperty("trophy", 90);
                heroResetTrophy.add(jsonObject1);
            }
            {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("id", 6);
                jsonObject1.addProperty("trophy", 90);
                heroResetTrophy.add(jsonObject1);
            }
            jsonObject.add("listHeroReset", heroResetTrophy);
        }
        {
            jsonObject.addProperty("rewardType", ERewardType.EMERALD.getValue());
            jsonObject.addProperty("rewardQuantity", 100);
        }
        return jsonObject;
    }

    public static JsonObject buildHero(HeroEnt heroEnt) {
        JsonObject hero = new JsonObject();
        hero.addProperty("id", heroEnt.getId());
        hero.addProperty("name", heroEnt.getTitle());
        hero.addProperty("hp", heroEnt.getHp());
        hero.addProperty("curPiece", heroEnt.getCurPiece());
        hero.addProperty("isOwned", heroEnt.isOwned());
        hero.addProperty("isUsed", heroEnt.isUsed());
        hero.addProperty("tag", -1);
        hero.addProperty("power", heroEnt.getPower());
        hero.addProperty("level", heroEnt.getLevel());
        hero.addProperty("skillLevel", heroEnt.getSkillLevel());
        hero.addProperty("powerPercent", heroEnt.getAtkLevelPercent());
        hero.addProperty("arPercent", heroEnt.getArmorLevelPercent());
        hero.addProperty("heroClass", heroEnt.getHeroClass().ordinal());
        hero.addProperty("rarity", heroEnt.getRarity().ordinal());
        hero.addProperty("trophy", heroEnt.getTrophy());
        if (heroEnt.getLevel() == GameConstant.HERO_MAX_LEVEL) {
            hero.addProperty("nextLevel", heroEnt.getLevel());
            hero.addProperty("upgradedHp", 0);
            hero.addProperty("upgradedPower", 0);
            hero.addProperty("requiredRainbow", 0);
            hero.addProperty("requiredMythic", 0);
            hero.addProperty("requiredPiece", 0);
            hero.addProperty("requiredGold", 0);
        } else {
            hero.addProperty("nextLevel", heroEnt.getLevel() + 1);
            hero.addProperty("upgradedHp", heroEnt.getHp() * 5 / 100);
            hero.addProperty("upgradedPower", heroEnt.getPower() * 5 / 100);
            UpgradeConfigEnt upgradeConfigEnt = heroEnt.getLevelUpgradeConfig(heroEnt.getLevel() + 1);

            hero.addProperty("requireRainbow", upgradeConfigEnt.getRainbow());
            hero.addProperty("requireMythic", upgradeConfigEnt.getMythic());
            hero.addProperty("requiredPiece", upgradeConfigEnt.getShard());
            hero.addProperty("requiredGold", upgradeConfigEnt.getGold());
        }
        if (heroEnt.getSkillLevel() == GameConstant.HERO_MAX_SKILL) {
            hero.addProperty("nextSkillLevel", heroEnt.getSkillLevel());
            hero.addProperty("skillUpgradeConditionLevel", 0);
            hero.addProperty("skillUpgradeConditionTrophy", 0);
            hero.addProperty("skillUpgradeMoney", 0);
        } else {
            UpgradeConfigEnt upgradeConfigEnt = heroEnt.getSkillUpgradeConfig(heroEnt.getSkillLevel() + 1);
            hero.addProperty("nextSkillLevel", heroEnt.getSkillLevel() + 1);
            hero.addProperty("skillUpgradeConditionLevel", upgradeConfigEnt.getLevelRequire());
            hero.addProperty("skillUpgradeConditionTrophy", upgradeConfigEnt.getTrophy());
            hero.addProperty("skillUpgradeMoney", upgradeConfigEnt.getGold());
        }

        return hero;
    }

    public static JsonObject ticketToJson(TicketEnt ticketEnt) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userID", ticketEnt.getUserID());
        jsonObject.addProperty("status", ticketEnt.getStatus().ordinal());
        jsonObject.addProperty("roomID", ticketEnt.getRoomID());
        jsonObject.addProperty("ip", ticketEnt.getIp());
        jsonObject.addProperty("port", ticketEnt.getPort());
        jsonObject.addProperty("currentRoom", ticketEnt.getCurrentRoom());
        jsonObject.addProperty("maxRoom", ticketEnt.getMaxRoom());
        jsonObject.addProperty("gameServerId", ticketEnt.getGameServerId());
        jsonObject.addProperty("deltaTrophy", ticketEnt.getDeltaTrophy());
        jsonObject.addProperty("trophy", ticketEnt.getHeroTrophy());
        jsonObject.addProperty("deltaLevel", ticketEnt.getDeltaLevel());
        jsonObject.addProperty("level", ticketEnt.getLevel());
        jsonObject.addProperty("deltaWinStreak", ticketEnt.getWinStreakTier());
        jsonObject.addProperty("winStreak", ticketEnt.getWinStreak());
        jsonObject.addProperty("deltaTrophyTime", DateTimeUtils.toString(new Date(ticketEnt.getInitTime())));
        jsonObject.addProperty("refreshTime", DateTimeUtils.toString(new Date(ticketEnt.getMatchTime())));
        jsonObject.addProperty("initTime", DateTimeUtils.toString(new Date(ticketEnt.getInitTime())));
        jsonObject.addProperty("deltaTime", ticketEnt.getDeltaTime());
        return jsonObject;
    }

    public static JsonObject eventTicketToJson(TicketEvent ticketEvent, Profile profile, int trophy) {
        JsonObject jsonObject = new JsonObject();
        JsonObject profileJson = profileToJson(profile, trophy);
        jsonObject.addProperty("roomId", ticketEvent.getRoomId());
        jsonObject.addProperty("eventType", ticketEvent.getEventType());
        jsonObject.addProperty("startTime", ticketEvent.getEventStartTime());
        jsonObject.addProperty("endTime", ticketEvent.getEventEndTime());
        jsonObject.addProperty("status", ticketEvent.getStatus().ordinal());
        //jsonObject.add("user", profileJson);
        return jsonObject;
    }

    public static JsonObject eventRoomToJson(RoomEvent roomEvent) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("roomID", roomEvent.getId());
        jsonObject.addProperty("eventType", roomEvent.getEventType());
        jsonObject.addProperty("roomStartTime", roomEvent.getStartRoomTime());
        jsonObject.addProperty("roomEndTime", roomEvent.getEndRoomTime());
        JsonArray listUser = new JsonArray();
        for (UserParticipant userParticipant : roomEvent.getListUser()) {
            JsonObject user = userParticipantToJson(userParticipant);
            listUser.add(user);
        }
        jsonObject.add("listUser", listUser);
        return jsonObject;
    }

    public static JsonObject userParticipantToJson(UserParticipant userParticipant) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userId", userParticipant.getUserId());
        jsonObject.addProperty("userName", userParticipant.getUserName());
        jsonObject.addProperty("trophy", userParticipant.getTrophy());
        jsonObject.addProperty("clanId", userParticipant.getClanID());
        jsonObject.addProperty("score", userParticipant.getScore());
        jsonObject.addProperty("rank", userParticipant.getRank());
        jsonObject.addProperty("rewardQuantity", userParticipant.getRewardQuantity());
        jsonObject.addProperty("isClaimed", userParticipant.isClaimed());
        jsonObject.addProperty("timeReachedScore", userParticipant.getTimeReachedScore());
        return jsonObject;
    }

    public static JsonObject gameLogToJson(GameLog gameLog) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("logID", gameLog.getId());
        jsonObject.addProperty("roomID", gameLog.getRoomID());
        jsonObject.addProperty("duration", gameLog.getDuration());
        jsonObject.addProperty("rounds", gameLog.getRounds());
        jsonObject.addProperty("playerIDS", gameLog.getPlayerIDS());
        jsonObject.addProperty("createdTime", gameLog.getCreatedTime().getTime());
        jsonObject.addProperty("startAt", gameLog.getStartTime());
        jsonObject.addProperty("winID", gameLog.getWinID());
        jsonObject.addProperty("score", gameLog.getScore());
        jsonObject.addProperty("gameMode", gameLog.getGameMode());
        return jsonObject;
    }

    public static JsonObject gameLogToJson(GameLog gameLog, List<Profile> profiles, int cur) {
        JsonObject jsonObject = gameLogToJson(gameLog);
        for (Profile profile : profiles) {
            if (profile.getId() == cur) {
                jsonObject.add("curPlayerInfo", profileToJson(profile));
            } else {
                jsonObject.add("enemyInfo", profileToJson(profile));
            }
        }
        return jsonObject;
    }

    public static JsonObject buildLoginResponse(JsonObject profile, JsonObject hero, String jwt, int deleteAccountStatus, long deleteTime){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", deleteAccountStatus);
        jsonObject.addProperty("deleteTime", deleteTime);
        jsonObject.addProperty("token", jwt);
        jsonObject.addProperty("serverTime", System.currentTimeMillis());
        jsonObject.add("profile", profile);
        jsonObject.add("hero", hero);
        return jsonObject;
    }

    public static JsonObject profileToJson(Profile profile) {

        JsonObject data = new JsonObject();
        data.addProperty("id", profile.getId());
        data.addProperty("gmail", profile.getGmail());
        data.addProperty("username", profile.getUsername());
        data.addProperty("displayName", profile.getDisplayName());
        data.addProperty("avatar", profile.getAvatarPath());
        // remove later
        data.addProperty("level", 0);
        data.addProperty("exp", 0);
        data.addProperty("expRequire", 0);
        // end remove later
        data.addProperty("dominateWin", profile.getDominateWin());
        data.addProperty("godlike", profile.getGodLikeWin());
        data.addProperty("winStreak", profile.getWinStreak());
        data.addProperty("highestStreak", profile.getHighestStreak());
        data.addProperty("battleWon", profile.getBattleWon());
        data.addProperty("maxRank", ERank.findByTrophy(profile.getHighestTrophy()).getValue());
        data.addProperty("highestTrophies", profile.getHighestTrophy());
        data.addProperty("emerald", profile.getEmerald());
        data.addProperty("amethyst", profile.getAmethyst());
        data.addProperty("royalAmethyst", profile.getRoyalAmethyst());
        data.addProperty("gold", profile.getMoney());
        data.addProperty("selectHero", profile.getSelectHero());
        data.addProperty("clanID", profile.getClanID());
        data.addProperty("frame", profile.getFrame());
        data.addProperty("profileSse", ModuleConfig.SSE_URL + "/sse/stream?chatChannel=" + String.format(SSEService.USER_CHANNEL, profile.getId()));
        data.addProperty("globalSse", ModuleConfig.SSE_URL + "/sse/stream?chatChannel=" + SSEService.GLOBAL_CHANNEL);
        data.addProperty("totalHeroes", profile.getTotalHeroes());
        data.addProperty("tutorial", profile.getTutorial());
        data.addProperty("selectHeroTrophy", profile.getSelectHeroTrophy());
        data.addProperty("googleID", profile.getGoogleId());
        data.addProperty("googleName", profile.getGoogleName());
        data.addProperty("googleAvatar", profile.getGoogleAvatar());
        data.addProperty("gmail", profile.getGmail());
        data.addProperty("appleID", profile.getAppleID());
        data.addProperty("appleName", profile.getAppleName());
        data.addProperty("appleAvatar", profile.getAppleAvatar());
        data.addProperty("appleMail", profile.getAppleEmail());
        data.addProperty("createdDate", profile.getDateCreated().getTime());
        return data;
    }

    public static JsonObject editProfileToJson(Profile profile, int profileTrophy, int errorEnum) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("error", errorEnum);
        jsonObject.add("profile", profileToJson(profile,profileTrophy));
        return jsonObject;
    }

    public static JsonObject profileToJson(Profile profile, int profileTrophy) {

        JsonObject data = profileToJson(profile);
        data.addProperty("rankTitle", ERank.findByTrophy(profileTrophy).getValue());
        data.addProperty("rankBadge", EBadge.findByTrophy(profileTrophy).getValue());
        data.addProperty("trophy", profileTrophy);
        return data;
    }

    public static JsonObject profileToJson(Profile profile, int profileTrophy, Map<String, Statistic> mapStatistic, int maxHero, int ownedHero) {
        JsonObject data = profileToJson(profile, profileTrophy);
        data.addProperty("maxChars", maxHero);
        data.addProperty("ownedChars", ownedHero);
        data.add("statistic", buildListStatistic(mapStatistic.values()));
        data.add("listAva", buildAvatarData(profile.getListAvatar()));
        data.add("listFrame", buildFrameData(profile.getListFrame()));
        data.addProperty("totalHand", mapStatistic.values().stream().map(e -> e.getTotalHand()).collect(Collectors.summingInt(Integer::intValue)));
        return data;
    }

    private static JsonArray buildAvatarData(String avatars) {
        JsonArray avaJsonArr = JSONUtil.DeSerialize(avatars, JsonArray.class);
        JsonArray jsonArray = new JsonArray();
        for (JsonElement item : avaJsonArr) {
            JsonObject jsonObject = new JsonObject();
            JsonObject ava = item.getAsJsonObject();
            jsonObject.addProperty("avaId", ava.get("avaId").getAsString());
            jsonObject.addProperty("avaPath", ava.get("avaPath").getAsString());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private static JsonArray buildFrameData(String frames) {
        JsonArray frameJsonArr = JSONUtil.DeSerialize(frames, JsonArray.class);
        JsonArray jsonArray = new JsonArray();
        for (JsonElement item : frameJsonArr) {
            JsonObject jsonObject = new JsonObject();
            JsonObject frame = item.getAsJsonObject();
            jsonObject.addProperty("frameId", frame.get("frameId").getAsInt());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    public static JsonArray buildListStatistic(Collection<Statistic> collection) {
        List<Statistic> statistics = new ArrayList<>();
        statistics.addAll(collection);
        Comparator<Statistic> comparator = Comparator.comparingInt(e -> e.getTrophy());
        comparator = comparator.reversed();
        statistics.sort(comparator);

        JsonArray jsonArray = new JsonArray();
        for (Statistic statistic : statistics) {
            jsonArray.add(buildStatistic(statistic));
        }
        return jsonArray;
    }

    public static JsonObject buildStatistic(Statistic statistic) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("heroID", statistic.getHeroID());
        jsonObject.addProperty("heroName", statistic.getHeroName());
        jsonObject.addProperty("totalHand", statistic.getTotalHand());
        jsonObject.addProperty("winHand", statistic.getWinHand());
        jsonObject.addProperty("trophy", statistic.getTrophy());
        jsonObject.addProperty("winRate", statistic.getWinRate());
        return jsonObject;
    }

    public static GameLog jsonToGameLog(JsonObject jsonObject) {
        int roomID = jsonObject.get("roomID").getAsInt();
        long duration = jsonObject.get("duration").getAsLong();
        String rounds = jsonObject.get("rounds").getAsString();
        String playerIDs = jsonObject.get("playerIDS").getAsString();
        long startAt = jsonObject.get("startAt").getAsLong();
        long winID = jsonObject.get("winID").getAsLong();
        int gameMode = jsonObject.get("winID").getAsInt();
        String playerInfo = "";
        if (jsonObject.has("playerInfo")) {
            JsonArray array = jsonObject.get("playerInfo").getAsJsonArray();
            playerInfo = array.toString();
        }
        Map<Integer, Integer> hpInfo = new HashMap<>();
        if (jsonObject.has("player1HP")) {
            String data = jsonObject.get("player1HP").getAsString();
            String[] splitData = data.split("_");
            hpInfo.put(ConvertUtils.toInt(splitData[0]), ConvertUtils.toInt(splitData[1]));
        }
        if (jsonObject.has("player2HP")) {
            String data = jsonObject.get("player2HP").getAsString();
            String[] splitData = data.split("_");
            hpInfo.put(ConvertUtils.toInt(splitData[0]), ConvertUtils.toInt(splitData[1]));
        }
        long matchEndAtMs = 0;
        String userJoinType = "MANUALLY";
        String userSubJoinType = "-";
        int matchRoundCount = 0;
        if (jsonObject.has("matchEndAtMs")) {
            matchEndAtMs = jsonObject.get("matchEndAtMs").getAsLong();
        }
        if (jsonObject.has("userJoinType")) {
            userJoinType = jsonObject.get("userJoinType").getAsString();
        }
        if (jsonObject.has("userSubJoinType")) {
            userSubJoinType = jsonObject.get("userSubJoinType").getAsString();
        }
        if (jsonObject.has("matchRoundCount")) {
            matchRoundCount = jsonObject.get("matchRoundCount").getAsInt();
        }
        GameLog gameLog = new GameLog(roomID, duration, rounds, playerIDs, winID, 0, startAt, playerInfo, gameMode, hpInfo,
                matchEndAtMs, userJoinType, userSubJoinType, matchRoundCount);
        return gameLog;
    }

    public static <E> JsonObject toSearchResponseData(Page<E> data) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("data", ModuleConfig.GSON_BUILDER.toJsonTree(data.getContent()));
        jsonObject.addProperty("total_count", data.getTotalElements());
        jsonObject.addProperty("total_page", data.getTotalPages());
        jsonObject.addProperty("limit", data.getNumberOfElements());
        jsonObject.addProperty("current_page", data.getNumber());
        return jsonObject;
    }

    public static String toErrorBody(String error, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        jsonObject.addProperty("data", error);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

    public static JsonObject toSearchResponseData(JsonArray data, long totalCount, int totalPage, int currentPage, int limit) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("data", data);
        jsonObject.addProperty("total_count", totalCount);
        jsonObject.addProperty("total_page", totalPage);
        jsonObject.addProperty("limit", limit);
        jsonObject.addProperty("current_page", currentPage);
        return jsonObject;
    }

    public static JsonObject toEmptySearchResponseData() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("data", new JsonArray());
        jsonObject.addProperty("total_count", 0);
        jsonObject.addProperty("total_page", 0);
        jsonObject.addProperty("limit", ModuleConfig.DATA_LIMIT);
        jsonObject.addProperty("current_page", 0);
        return jsonObject;
    }
}
