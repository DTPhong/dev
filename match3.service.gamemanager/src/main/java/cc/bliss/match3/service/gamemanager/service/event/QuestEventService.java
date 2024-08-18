/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.QuestDTO;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.DailyRewardTrackingCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.QuestTrackingCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.common.ReddotService;
import cc.bliss.match3.service.gamemanager.service.shop.ChestService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.SSEService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
@Primary
public class QuestEventService extends BaseService {

    /**
     * quest_{questID}_progress
     */
    protected static final String FIELD_QUEST_PROGRESS = "quest_%s_progress";
    /**
     * quest_{questID}_is_claim
     */
    protected static final String FIELD_QUEST_IS_CLAIM = "quest_%s_is_claim";
    /**
     * hash_event_info_{eventID}_{userID}_{date}
     */
    protected static final String HASH_USER_EVENT_INFO = "hash_event_info_%s_%s_%s";
    protected static final String FIELD_LIST_QUEST = "list_quest";
    protected static final String FIELD_NEXT_WATCH_ADS = "next_watch_ads";
    @Autowired
    protected ProfileService profileService;
    @Autowired
    protected InventoryService inventoryService;
    @Autowired
    protected SSEService sseService;
    @Autowired
    protected ReddotService reddotService;
    @Autowired
    protected TriggerService triggerService;
    @Autowired
    protected ChestService chestService;
    @Autowired
    protected LeaderboardService leaderboardService;
    @Autowired
    protected HeroService heroService;
    @Autowired
    private DailyQuestService dailyQuestService;

    public JsonArray getCurrentEvent(long userID) {
        JsonArray listEventArr = new JsonArray();
        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject eventJson = buildEventJson(userID, eventEnt);

            listEventArr.add(eventJson);
        }
        return listEventArr;
    }

    protected JsonObject buildEventJson(long userID, EventEnt eventEnt) {
        JsonObject eventJson = new JsonObject();
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        TriggerEnt triggerEnt = triggerService.getTriggerByEventID(eventEnt.getId());
        EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

        List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
        JsonArray questJsonArr = buildListQuestJson(questEnts);
        JsonObject eventMilestone = buildEventMileStone(userID, eventEnt.getId(), customData, eEventRecordType, mapData, questEnts);

        eventJson.addProperty("id", eventEnt.getId());
        eventJson.addProperty("title", eventEnt.getTitle());
        eventJson.addProperty("remainTime", getEndTime(triggerEnt, eEventRecordType) - System.currentTimeMillis());
        eventJson.addProperty("endTime", getEndTime(triggerEnt, eEventRecordType));
        eventJson.add("quests", questJsonArr);
        eventJson.add("eventMilestone", eventMilestone);
        eventJson.addProperty("type", eEventRecordType.name());
        return eventJson;
    }

    protected JsonObject buildEventMileStone(long userID, int eventID, JsonObject customData, EEventRecordType eEventRecordType, Map<String, String> mapData, List<QuestDTO> questEnts) {
        JsonObject jsonObject = new JsonObject();
        List<QuestDTO> milestone = getEventQuest(userID, eventID, customData, eEventRecordType, mapData, "eventProgress");
        JsonArray questJsonArr = buildListQuestJson(milestone);
        long progress = questEnts.stream().filter(e -> e.getStatus() == EQuestStatus.CLAIMED.getValue()).count();
        long totalProgress = questEnts.size();

        jsonObject.addProperty("progress", progress);
        jsonObject.addProperty("total", totalProgress);
        jsonObject.add("milestone", questJsonArr);

        return jsonObject;
    }

    protected JsonArray buildListQuestJson(List<QuestDTO> questEnts) {
        JsonArray jsonArray = new JsonArray();
        for (QuestDTO questEnt : questEnts) {
            JsonObject questJson = buildQuestJson(questEnt);
            jsonArray.add(questJson);
        }
        return jsonArray;
    }

    protected JsonObject buildQuestJson(QuestDTO questEnt) {
        JsonObject questJson = new JsonObject();
        questJson.addProperty("id", questEnt.getId());
        questJson.addProperty("title", questEnt.getTitle());
        questJson.addProperty("type", questEnt.getQuestType());
        questJson.addProperty("progress", questEnt.getProgress());
        questJson.addProperty("status", questEnt.getStatus());
        questJson.addProperty("require", questEnt.getRequire());
        questJson.addProperty("rewardType", questEnt.getRewardType());
        questJson.addProperty("rewardRefId", questEnt.getRewardRefId());
        questJson.addProperty("rewardTitle", questEnt.getRewardTitle());
        questJson.addProperty("rewardQuantity", questEnt.getRewardQuantity());
        questJson.addProperty("heroID", questEnt.getHeroID());
        questJson.addProperty("buttonData", questEnt.getButtonData());
        questJson.addProperty("nextWatchAds", questEnt.getNextWatchAds());
        questJson.addProperty("questDetail", questEnt.getQuestDetail());
        return questJson;
    }

    protected JsonObject buildQuestJsonComplete(QuestDTO questEnt) {
        JsonObject questJson = buildQuestJson(questEnt);
        questJson.addProperty("beforeProgress", questEnt.getProgress());
        questJson.addProperty("progress", questEnt.getRequire());
        questJson.addProperty("status", EQuestStatus.CLAIMABLE.getValue());
        return questJson;
    }

    protected String buildSSEQuestJson(QuestDTO quest) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("toastType", ESseToast.QUEST_COMPLETE.getValue());
        jsonObject.add("content", buildQuestJsonComplete(quest));
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.SSE_TOAST);
    }

    protected long getEndTime(TriggerEnt triggerEnt, EEventRecordType eEventRecordType) {
        switch (eEventRecordType) {
            case DAILY: {
                Date beginDate = new Date(getStartTime(triggerEnt, eEventRecordType));
                Date endDate = DateTimeUtils.addDays(beginDate, 1);
                return endDate.getTime();
            }
            case WEEKLY: {
                Date beginDate = new Date(getStartTime(triggerEnt, eEventRecordType));
                Date endDate = DateTimeUtils.addDays(beginDate, 7);
                return endDate.getTime();
            }
            case MONTHLY: {
                Date beginDate = new Date(getStartTime(triggerEnt, eEventRecordType));
                Date endDate = DateTimeUtils.addMonths(beginDate, 1);
                return endDate.getTime();
            }
            default:
                return triggerEnt.getEndTime().getTime();
        }
    }

    protected long getStartTime(TriggerEnt triggerEnt, EEventRecordType eEventRecordType) {
        switch (eEventRecordType) {
            case DAILY: {
                Date beginDate = DateTimeUtils.getBeginDate(new Date());
                return beginDate.getTime();
            }
            case WEEKLY: {
                Date beginDate = DateTimeUtils.getBeginWeek(new Date());
                return beginDate.getTime();
            }
            case MONTHLY: {
                Date beginDate = DateTimeUtils.getBeginMonth(new Date());
                return beginDate.getTime();
            }
            default:
                return triggerEnt.getEndTime().getTime();
        }
    }

    public List<QuestDTO> getEventQuest(long userID, int eventID, JsonObject customData, EEventRecordType eEventRecordType, Map<String, String> mapData, String type) {
        List<QuestDTO> questList = new ArrayList<>();
        // load event data
        JsonArray questJsonArr = customData.get(type).getAsJsonArray();
        if (questJsonArr == null) {
            return questList;
        }
        for (JsonElement el : questJsonArr) {
            QuestDTO questRawData = jsonToSingleQuestRawData(el.getAsJsonObject(), userID);
            questRawData = addQuestProgress(questRawData, eventID, userID, mapData);
            questList.add(questRawData);
        }
        if (type.equalsIgnoreCase("quest")) {
            Comparator<QuestDTO> comparator = Comparator.comparingInt(QuestDTO::getTier).thenComparingInt(QuestDTO::getId);
            questList.sort(comparator);
        }
        return questList;
    }

    public Map<String, String> getMapEventData(long userId, int eventID, EEventRecordType eEventRecordType) {
        String key = getHashEventKey(userId, eventID, eEventRecordType);
        if (key.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        HashOperations<String, String, String> hashOperations = redisTemplateString.opsForHash();
        return hashOperations.entries(key);
    }

    public QuestDTO jsonToSingleQuestRawData(JsonObject jsonObject, long userID) {
        QuestDTO questEnt = new QuestDTO();
        questEnt.setId(jsonObject.get("id").getAsInt());
        questEnt.setTitle(jsonObject.get("title").getAsString());
        questEnt.setRequire(jsonObject.get("require").getAsInt());
        questEnt.setRewardQuantity(jsonObject.get("rewardQuantity").getAsInt());
        questEnt.setQuestType(jsonObject.has("questType") ? jsonObject.get("questType").getAsInt() : 0);
        questEnt.setHeroID(jsonObject.has("heroID") ? jsonObject.get("heroID").getAsInt() : 0);
        questEnt.setDaily(jsonObject.get("isDaily").getAsBoolean());
        questEnt.setPlayWithFriend(jsonObject.has("playWithFriend") && jsonObject.get("playWithFriend").getAsBoolean());
        questEnt.setWin(jsonObject.has("isWin") && jsonObject.get("isWin").getAsBoolean());
        questEnt.setWinPerfect(jsonObject.has("isWinPerfect") && jsonObject.get("isWinPerfect").getAsBoolean());
        questEnt.setWinMultiple(jsonObject.has("isWinMultiple") && jsonObject.get("isWinMultiple").getAsBoolean());
        questEnt.setTier(jsonObject.has("tier") ? jsonObject.get("tier").getAsInt() : 0);

        questEnt.setRocketMatch(jsonObject.has("rocketMatch") ? jsonObject.get("rocketMatch").getAsInt() : 0);
        questEnt.setBombMatch(jsonObject.has("bombMatch") ? jsonObject.get("bombMatch").getAsInt() : 0);
        questEnt.setLightMatch(jsonObject.has("lightMatch") ? jsonObject.get("lightMatch").getAsInt() : 0);

        questEnt.setLightRocket(jsonObject.has("lightXRocket") ? jsonObject.get("lightXRocket").getAsInt() : 0);
        questEnt.setLightBomb(jsonObject.has("lightXBomb") ? jsonObject.get("lightXBomb").getAsInt() : 0);
        questEnt.setRocketBomb(jsonObject.has("rocketXBomb") ? jsonObject.get("rocketXBomb").getAsInt() : 0);
        questEnt.setTotalMerge(jsonObject.has("totalMergeSpecial") ? jsonObject.get("totalMergeSpecial").getAsInt() : 0);

        questEnt.setHpCount(jsonObject.has("hpCount") ? jsonObject.get("hpCount").getAsInt() : 0);
        questEnt.setRedCount(jsonObject.has("redCount") ? jsonObject.get("redCount").getAsInt() : 0);
        questEnt.setBlueCount(jsonObject.has("blueCount") ? jsonObject.get("blueCount").getAsInt() : 0);
        questEnt.setGreenCount(jsonObject.has("greenCount") ? jsonObject.get("greenCount").getAsInt() : 0);
        questEnt.setYellowCount(jsonObject.has("yellowCount") ? jsonObject.get("yellowCount").getAsInt() : 0);

        JsonObject rewardData = jsonObject.has("rewardData") ? jsonObject.get("rewardData").getAsJsonObject() : new JsonObject();
        rewardData.addProperty("userId", userID);
        questEnt.setRewardData(rewardData);
        questEnt.setRewardType(jsonObject.has("rewardType") ? jsonObject.get("rewardType").getAsInt() : 0);
        questEnt.setRewardRefId(jsonObject.has("rewardRefId") ? jsonObject.get("rewardRefId").getAsInt() : 0);
        return questEnt;
    }

    public QuestDTO addQuestProgress(QuestDTO questRawData, int eventID, long userID, Map<String, String> mapData) {

        String fieldClaimedKeyKey = getQuestField(questRawData, false);
        String fieldProgressKey = getQuestField(questRawData, true);
        boolean isClaimed = mapData.containsKey(fieldClaimedKeyKey);
        int progress = ConvertUtils.toInt(mapData.get(fieldProgressKey));
        int require = questRawData.getRequire();
        int status = 0;

        if (isClaimed) {
            status = EQuestStatus.CLAIMED.getValue();
        } else {
            status = (progress < require)
                    ? EQuestStatus.PROGRESS.getValue()
                    : EQuestStatus.CLAIMABLE.getValue();
        }

        if (progress > require) {
            progress = require;
        }
        questRawData.setProgress(progress);
        questRawData.setRequire(require);
        questRawData.setStatus(status);
        return questRawData;
    }

    public String getQuestField(QuestDTO quest, boolean isProgressField) {
        return getQuestField(quest.getId(), isProgressField);
    }

    public String getQuestField(int quest, boolean isProgressField) {
        if (isProgressField) {
            return String.format(FIELD_QUEST_PROGRESS, quest);
        } else {
            return String.format(FIELD_QUEST_IS_CLAIM, quest);
        }
    }

    protected List<EventEnt> getListEvent() {
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.QUEST_EVENT.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        List<EventEnt> listEvent = triggerService.getListEvent(listEventId);
        return listEvent;
    }

    private void sendQuestCompleteData(Profile profile, EventEnt eventEnt, QuestDTO questEnt) {
        int userTrophy = leaderboardService.getProfileTrophy(profile.getId());
        JsonObject eventInfo = buildEventJson(profile.getId(), eventEnt);
        JsonArray updateRewardBody = buildQuestCompleteData(questEnt);
        JsonArray data = buildReceiveItemName(updateRewardBody);
        GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, data, "DONE", redisTemplateString));
    }

    //only use for tracking
    public JsonArray buildReceiveItemName(JsonArray data) {
        for (JsonElement jsonElement : data) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            int value = jsonObject.get("rewardType").getAsInt();
            ERewardType rewardType = ERewardType.findByValue(value);
            RewardEnt rewardEnt = new RewardEnt();
            rewardEnt.setERewardType(rewardType);
            jsonObject.addProperty("rewardTitle", rewardEnt.getTitle());
        }
        return data;
    }


    public void listenEndGame(GameLog gameLog) {
        List<EventEnt> listEvent = getListEvent();

        List<Long> userIDs = gameLog.getListUserID();
        Map<Long, Profile> mapProfile = profileService.getMapByListId(userIDs);
        long winID = gameLog.getWinID();
        for (Long userID : userIDs) {
            Profile profile = mapProfile.get(userID);
            for (EventEnt eventEnt : listEvent) {
                JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
                EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
                Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

                List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
                for (QuestDTO questEnt : questEnts) {
                    String questProgressField = getQuestField(questEnt, true);
                    int progress = ConvertUtils.toInt(mapData.get(questProgressField));
                    int after = 0;
                    if (progress >= questEnt.getRequire()) {
                        continue;
                    }
                    EQuestType questType = EQuestType.findByValue(questEnt.getQuestType());
                    switch (questType) {
                        case PLAY_GAME: {
                            if (questEnt.isWinMultiple() && userID != winID) {
                                if (progress < questEnt.getRequire()) {
                                    after = recordQuest(userID, eventEnt.getId(), questProgressField, -progress, eEventRecordType);
                                }
                                continue;
                            }
                            if (questEnt.isWin() && userID != winID) {
                                continue;
                            }
                            if (questEnt.isWinPerfect() && userID != winID && gameLog.getDuration() < 60) {
                                continue;
                            }
                            if (questEnt.getHeroID() != 0 && questEnt.getHeroID() != profile.getSelectHero()) {
                                continue;
                            }
                            after = recordQuest(userID, eventEnt.getId(), questProgressField, 1, eEventRecordType);
                            if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                sendQuestCompleteData(profile, eventEnt, questEnt);
                            }
                        }
                        break;
                        case MATCH_SPECIAL: {
                            JsonArray gameInfos = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
                            JsonObject gameInfo = new JsonObject();
                            int lightMatch = 0;
                            int bombMatch = 0;
                            int rocketMatch = 0;
                            int lightXRocket = 0;
                            int lightXBomb = 0;
                            int rocketXBomb = 0;
                            for (JsonElement e : gameInfos) {
                                JsonArray roundsArr = e.getAsJsonObject().get("playerTurns").getAsJsonArray();
                                for (JsonElement roundElement : roundsArr) {
                                    JsonObject roundJson = roundElement.getAsJsonObject();
                                    if (roundJson.has("playerId") && roundJson.get("playerId").getAsInt() == userID) {
                                        lightMatch += roundJson.get("makeSpecial5Count").getAsInt();
                                        bombMatch += roundJson.get("makeBoomCount").getAsInt();
                                        rocketMatch += roundJson.get("makeHorizontalRocketCount").getAsInt() + roundJson.get("makeVerticalRocketCount").getAsInt();
                                        lightXRocket += roundJson.get("mergeLightXHrocket").getAsInt() + roundJson.get("mergeLightXVrocket").getAsInt();
                                        lightXBomb += roundJson.get("mergeLightXBomb").getAsInt();
                                        rocketXBomb += roundJson.get("mergeBombXVrocket").getAsInt() + roundJson.get("mergeBombXHrocket").getAsInt();
                                    }
                                }
                            }
                            gameInfo.addProperty("lightMatch", lightMatch);
                            gameInfo.addProperty("bombMatch", bombMatch);
                            gameInfo.addProperty("rocketMatch", rocketMatch);
                            gameInfo.addProperty("lightXRocket", lightXRocket);
                            gameInfo.addProperty("lightXBomb", lightXBomb);
                            gameInfo.addProperty("rocketXBomb", rocketXBomb);
                            if (questEnt.getLightMatch() != 0 && gameInfo.get("lightMatch").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("lightMatch").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getBombMatch() != 0 && gameInfo.get("bombMatch").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("bombMatch").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getRocketMatch() != 0 && gameInfo.get("rocketMatch").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("rocketMatch").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getLightRocket() != 0 && gameInfo.get("lightXRocket").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("lightXRocket").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getLightBomb() != 0 && gameInfo.get("lightXBomb").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("lightXBomb").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getRocketBomb() != 0 && gameInfo.get("rocketXBomb").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("rocketXBomb").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                        }
                        break;
                        case MATCH_GEM: {
                            JsonArray gameInfos = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
                            JsonObject gameInfo = new JsonObject();
                            int redCount = 0;
                            int blueCount = 0;
                            int greenCount = 0;
                            int hpCount = 0;
                            int yellowCount = 0;
                            for (JsonElement e : gameInfos) {
                                JsonArray roundsArr = e.getAsJsonObject().get("playerTurns").getAsJsonArray();
                                for (JsonElement roundElement : roundsArr) {
                                    JsonObject roundJson = roundElement.getAsJsonObject();
                                    if (roundJson.has("playerId") && roundJson.get("playerId").getAsInt() == userID) {
                                        redCount += roundJson.get("gemRedCount").getAsInt();
                                        blueCount += roundJson.get("gemBlueCount").getAsInt();
                                        greenCount += roundJson.get("gemGreenCount").getAsInt();
                                        hpCount += roundJson.get("gemPinkCount").getAsInt();
                                        yellowCount += roundJson.get("gemYellowCount").getAsInt();
                                    }
                                }
                            }
                            gameInfo.addProperty("redCount", redCount);
                            gameInfo.addProperty("blueCount", blueCount);
                            gameInfo.addProperty("greenCount", greenCount);
                            gameInfo.addProperty("hpCount", hpCount);
                            gameInfo.addProperty("yellowCount", yellowCount);
                            if (questEnt.getRedCount() != 0 && gameInfo.get("redCount").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("redCount").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getBlueCount() != 0 && gameInfo.get("blueCount").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("blueCount").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getGreenCount() != 0 && gameInfo.get("greenCount").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("greenCount").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getHpCount() != 0 && gameInfo.get("hpCount").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("hpCount").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                            if (questEnt.getYellowCount() != 0 && gameInfo.get("yellowCount").getAsInt() > 0) {
                                after = recordQuest(userID, eventEnt.getId(), questProgressField, gameInfo.get("yellowCount").getAsInt(), eEventRecordType);
                                if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                                    reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, 1);
                                    sendQuestCompleteData(profile, eventEnt, questEnt);
                                }
                                continue;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    public void listUpgradeEvent(long userID, EQuestType questType) {
        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

            List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
            for (QuestDTO questEnt : questEnts) {
                String questProgressField = getQuestField(questEnt, true);
                int progress = ConvertUtils.toInt(mapData.get(questProgressField));
                int after = 0;
                if (progress >= questEnt.getRequire()) {
                    continue;
                }
                if (questEnt.getQuestType() == EQuestType.UPGRADE_HERO.getValue()) {
                    after = recordQuest(userID, eventEnt.getId(), questProgressField, 1, eEventRecordType);
                    if (after >= questEnt.getRequire() && this.getClass().equals(QuestEventService.class)) {
                        sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                        Profile profile = profileService.getProfileByID(userID);
                        int userTrophy = leaderboardService.getProfileTrophy(userID);
                        JsonObject eventInfo = buildEventJson(userID, eventEnt);
                        JsonArray updateRewardBody = buildQuestCompleteData(questEnt);
                        JsonArray data = buildReceiveItemName(updateRewardBody);
                        GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, data, "DONE", redisTemplateString));
                    }
                }
            }
        }
    }

    public JsonArray buildQuestCompleteData(QuestDTO questDTO) {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("rewardType", questDTO.getRewardType());
        jsonObject.addProperty("delta", questDTO.getRewardQuantity());
        jsonObject.addProperty("title", questDTO.getTitle());
        jsonArray.add(jsonObject);
        return jsonArray;
    }

    public void listenLogin(Profile session) {
        long userID = session.getId();
        // Find Events
        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

            List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
            for (QuestDTO questEnt : questEnts) {
                String questProgressField = getQuestField(questEnt, true);
                int progress = ConvertUtils.toInt(mapData.get(questProgressField));
                if (progress >= questEnt.getRequire()) {
                    continue;
                }
                if (questEnt.getQuestType() == EQuestType.LOGIN.getValue()) {
                    recordQuest(userID, eventEnt.getId(), questProgressField, 1, eEventRecordType);
                }
                if (questEnt.getQuestType() == EQuestType.LOGIN_BY_DATE_ID.getValue() && DateTimeUtils.getDay(new Date()) == questEnt.getId()) {
                    recordQuest(userID, eventEnt.getId(), questProgressField, 1, eEventRecordType);
                }
            }
        }
    }

    public int recordQuest(long userId, int eventID, String field, long delta, EEventRecordType eEventRecordType) {
        String key = getHashEventKey(userId, eventID, eEventRecordType);
        if (key.isEmpty()) {
            return 0;
        }
        redisTemplateString.expire(key, Duration.ofDays(60));
        return redisTemplateString.opsForHash().increment(key, field, delta).intValue();
    }

    protected String getHashEventKey(long userId, int eventID, EEventRecordType eEventRecordType) {
        String key = "";
        switch (eEventRecordType) {
            case DAILY: {
                key = String.format(HASH_USER_EVENT_INFO, eventID, userId, DateTimeUtils.getNow(DATE_FORMAT));
                break;
            }
            case WEEKLY: {
                key = String.format(HASH_USER_EVENT_INFO, eventID, userId, DateTimeUtils.getNow(DATE_WEEK_FORMAT));
                break;
            }
            case MONTHLY: {
                key = String.format(HASH_USER_EVENT_INFO, eventID, userId, DateTimeUtils.getNow(DATE_MONTH_FORMAT));
                break;
            }
            default: {
                key = String.format(HASH_USER_EVENT_INFO, eventID, userId, "");
            }
        }
        return key;
    }

    public String claimEvent(long userID, int eventID, int questID, boolean isReclaim) {
        EventEnt eventEnt = triggerService.getEvent(eventID);
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);
        Profile profile = profileService.getProfileByID(userID);
        long goldBeforeAction = profile.getMoney();
        long emeraldBeforeAction = profile.getEmerald();
        List<QuestDTO> questEnts = Collections.EMPTY_LIST;
        if (questID > 0) {
            questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
        } else {
            questEnts = getEventQuest(userID, eventID, customData, eEventRecordType, mapData, "eventProgress");
        }
        QuestDTO questEnt = questEnts.stream().filter(e -> e.getId() == questID).findFirst().get();
        String questClaimField = getQuestField(questEnt, false);
        if (mapData.containsKey(questClaimField)){
            return ResponseUtils.toErrorBody("Claimed !", NetWorkAPI.UNKNOWN);
        }
        recordQuest(userID, eventID, questClaimField, userID, eEventRecordType);

        JsonObject eventInfo = buildEventJson(userID, eventEnt);

        List<RewardEnt> list = new ArrayList<>();
        if (ERewardType.isChest(questEnt.getRewardType())){
            list.addAll(chestService.randomChestReward(userID, questEnt.getRewardType()));
        } else {
            list.addAll(inventoryService.claimItem(userID, ERewardType.findByValue(questEnt.getRewardType()), questEnt.getRewardQuantity(), EUpdateMoneyType.QUEST));
        }
        JsonArray updateRewardBody = JsonBuilder.buildListReward(list);
        if (this.getClass().equals(QuestEventService.class)) {
            reddotService.updateReddot(userID, EReddotFeature.DAILY_QUEST, -1);
        }

        int userTrophy = leaderboardService.getProfileTrophy(userID);
        String actionType = getActionType(questEnt.getId(), isReclaim);
        if (isReclaim){
            GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, profile.getId(), EQuestType.WATCH_ADS_QUEST, 1));
        }
        GMLocalQueue.addQueue(new DailyRewardTrackingCmd(producer, profile, userTrophy, actionType, questEnt,null, eventInfo.get("endTime").getAsLong(), goldBeforeAction, emeraldBeforeAction, getRewardName(list), redisTemplateString));
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), eventInfo, updateRewardBody, questEnt.getRewardType()).toString();
    }

    private String getActionType(int id, boolean isReclaim) {
        if (!isReclaim) {
            switch (id) {
                case 7:
                    return "CLAIM_STREAK_7D";
                case 14:
                    return "CLAIM_STREAK_14D";
                case 21:
                    return "CLAIM_STREAK_21D";
                case 28:
                    return "CLAIM_STREAK_28D";
                default:
                    return "CLAIM_DAY";
            }
        } else {
            return "RECLAIM_DAY";
        }
    }

    private String getRewardName(List<RewardEnt> listReward) {
        String rewardName = "";
        for (RewardEnt item : listReward) {
            String title = item.getTitle();
            if (ERewardType.HERO_CARD.equals(item.getERewardType())) {
                rewardName = title + " " +heroService.getHero(item.getRef()).getTitle();
            } else {
                rewardName = title;
            }
        }
        return rewardName;
    }

    public int getReddot(List<QuestDTO> questEnts) {
        return ConvertUtils.toInt(questEnts.stream().filter(e -> e.getStatus() == EQuestStatus.CLAIMABLE.getValue()).count());
    }

    public int getReddot(long userID) {
        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

            List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
            return getReddot(questEnts);
        }
        return 0;
    }
}
