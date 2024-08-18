/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.QuestDTO;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.*;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.QuestTrackingCmd;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.math.RandomUtils;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cc.bliss.match3.service.gamemanager.constant.GameConstant.*;

/**
 * @author Phong
 */
@Service
public class DailyQuestService extends QuestEventService {

    @Autowired
    private WatchAdsQuestService watchAdsQuestService;
    private Map<Integer, List<QuestEnt>> mapQuestPool = new NonBlockingHashMap<>();
    private List<QuestEnt> listQuestPool = new ArrayList<>();

    private static final String SET_USER_SURVEY = "set_user_survey";
    private static final String FIELD_QUEST_HERO_ID = "quest_hero_id";
    private static final String FIELD_QUEST_HERO_REQUIRE = "quest_hero_require";

    @Override
    protected JsonObject buildEventJson(long userID, EventEnt eventEnt) {
        JsonObject eventJson = new JsonObject();
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        TriggerEnt triggerEnt = triggerService.getTriggerByEventID(eventEnt.getId());
        EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

        List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
        JsonArray questJsonArr = buildListQuestJson(questEnts);

        eventJson.addProperty("id", eventEnt.getId());
        eventJson.addProperty("title", eventEnt.getTitle());
        eventJson.addProperty("remainTime", getEndTime(triggerEnt, eEventRecordType) - System.currentTimeMillis());
        eventJson.addProperty("endTime", getEndTime(triggerEnt, eEventRecordType));
        eventJson.add("quests", questJsonArr);
        eventJson.addProperty("type", eEventRecordType.name());
        return eventJson;
    }

    @Override
    public QuestDTO addQuestProgress(QuestDTO questRawData, int eventID, long userID, Map<String, String> mapData) {
        String fieldClaimedKey = getQuestField(questRawData, false);
        String fieldProgressKey = getQuestField(questRawData, true);
        int progress = (questRawData.getQuestType() == EQuestType.OVERVIEW_QUEST.getValue())
                ? questRawData.getProgress()
                : ConvertUtils.toInt(mapData.get(fieldProgressKey));

        int require = questRawData.getRequire();
        boolean isClaimed = mapData.containsKey(fieldClaimedKey);
        int status = isClaimed ? EQuestStatus.CLAIMED.getValue()
                : (progress < require) ? EQuestStatus.PROGRESS.getValue()
                : EQuestStatus.CLAIMABLE.getValue();

        questRawData.setProgress(Math.min(progress, require));
        questRawData.setRequire(require);
        questRawData.setStatus(status);

        return questRawData;
    }

    @Override
    public JsonArray getCurrentEvent(long userID) {
        JsonArray listEventArr = new JsonArray();
        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject eventJson = buildEventJson(userID, eventEnt);

            listEventArr.add(eventJson);
        }
        return listEventArr;
    }

    @Override
    protected List<EventEnt> getListEvent() {
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.DAILY_QUEST.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        List<EventEnt> listEvent = triggerService.getListEvent(listEventId);
        return listEvent;
    }

    @Override
    public List<QuestDTO> getEventQuest(long userID, int eventID, JsonObject customData, EEventRecordType eEventRecordType, Map<String, String> mapData, String type) {
        List<QuestDTO> questList = new ArrayList<>();
        String listQuestString = mapData.getOrDefault(FIELD_LIST_QUEST, "");

        if (listQuestString.isEmpty()) {
            listQuestString = initDailyQuest(userID, eventID, eEventRecordType);
        }

        // Load event data
        List<Integer> listQuestID = JSONUtil.DeSerialize(listQuestString, new TypeToken<List<Integer>>(){}.getType());

        JsonArray questJsonArr = new JsonArray();
        for (Integer questID : listQuestID) {
            QuestEnt questData = listQuestPool.stream().filter(e -> e.getId() == questID).findFirst().orElse(null);
            if (questData != null) {
                JsonObject questJson = JSONUtil.DeSerialize(questData.getCustomData(), JsonObject.class);
                questJson.addProperty("id", questID);
                questJson.addProperty("tier", questData.getTier());
                questJsonArr.add(questJson);
            }
        }

        for (JsonElement el : questJsonArr) {
            QuestDTO questRawData = jsonToSingleQuestRawData(el.getAsJsonObject(), userID);
            questRawData = addQuestProgress(questRawData, eventID, userID, mapData);
            questList.add(questRawData);
        }

        QuestDTO genericQuest = createGenericHeroQuest(eventID, userID, mapData, listQuestID);
        if (genericQuest != null){
            questList.add(genericQuest);
        }

        // Survey quest
        if (ModuleConfig.IS_TEST && !redisTemplateString.opsForSet().isMember(SET_USER_SURVEY, String.valueOf(userID))) {
            QuestDTO surveyQuest = createSurveyQuest(eventID, userID, mapData);
            questList.add(surveyQuest);
        }

        if (type.equalsIgnoreCase("quest")) {
            questList.sort(Comparator.comparingInt(QuestDTO::getPercentFinishProgress)
                    .reversed()
                    .thenComparingInt(QuestDTO::getId));
        }

        // Overview quest
        QuestDTO overviewQuest = createOverviewQuest(questList, eventID, userID, mapData);

        // Ads quest
        Map<String, String> mapDataAds = getMapEventData(userID, eventID, EEventRecordType.NONE);
        List<QuestDTO> finalQuestList = new ArrayList<>(watchAdsQuestService.getEventQuest(userID, eventID, customData, EEventRecordType.NONE, mapDataAds, type));

        finalQuestList.add(overviewQuest);
        finalQuestList.addAll(questList);

        return finalQuestList;
    }

    private QuestDTO createGenericHeroQuest(int eventID, long userID, Map<String, String> mapData, List<Integer> listQuestID) {
        int heroID = ConvertUtils.toInt(mapData.get(FIELD_QUEST_HERO_ID));
        int require = ConvertUtils.toInt(mapData.get(FIELD_QUEST_HERO_REQUIRE));
        Integer questID = null;

        if (listQuestID.contains(WIN_HERO_QUEST_ID)) {
            questID = WIN_HERO_QUEST_ID;
        } else if (listQuestID.contains(DEAL_DMG_HERO_QUEST_ID)) {
            questID = DEAL_DMG_HERO_QUEST_ID;
        }

        if (questID != null) {
            QuestDTO questDTO = new QuestDTO();
            questDTO.setId(questID);
            questDTO.setRequire(require);
            questDTO.setHeroID(heroID);
            return addQuestProgress(questDTO, eventID, userID, mapData);
        }

        return null;
    }

    private QuestDTO createSurveyQuest(int eventID, long userID, Map<String, String> mapData) {
        QuestDTO questRawData = new QuestDTO();
        questRawData.setId(QUEST_SURVEY_ID);
        questRawData.setTitle("Survey quest");
        questRawData.setQuestType(EQuestType.REDIRECT_LINK.getValue());
        questRawData.setRewardType(ERewardType.GOLD.getValue());
        questRawData.setRewardQuantity(200);
        questRawData.setRequire(1);
        questRawData.setButtonData("https://forms.gle/w3J7fy5dPJtVXh2t6");
        return addQuestProgress(questRawData, eventID, userID, mapData);
    }

    private QuestDTO createOverviewQuest(List<QuestDTO> questList, int eventID, long userID, Map<String, String> mapData) {
        QuestDTO questRawData = new QuestDTO();
        questRawData.setId(OVERVIEW_QUEST_ID);
        questRawData.setTitle("Complete daily quests");
        questRawData.setQuestType(EQuestType.OVERVIEW_QUEST.getValue());
        questRawData.setRewardType(ERewardType.GOLD.getValue());
        questRawData.setRewardQuantity(86);
        questRawData.setRequire(5);
        int progress = (int) questList.stream()
                .filter(e -> e.getStatus() == EQuestStatus.CLAIMED.getValue() || e.getStatus() == EQuestStatus.CLAIMABLE.getValue())
                .count();
        questRawData.setProgress(progress);
        return addQuestProgress(questRawData, eventID, userID, mapData);
    }


    @Override
    public String claimEvent(long userID, int eventID, int questID, boolean isReclaim) {
        if (questID == ADS_QUEST_ID){
            return watchAdsQuestService.claimEvent(userID, eventID, questID, isReclaim);
        }
        EventEnt eventEnt = triggerService.getEvent(eventID);
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

        List<QuestDTO> questEnts;
        if (questID > 0) {
            questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
        } else {
            questEnts = getEventQuest(userID, eventID, customData, eEventRecordType, mapData, "eventProgress");
        }
        QuestDTO questEnt = questEnts.stream().filter(e -> e.getId() == questID).findFirst().get();
        String questClaimField = getQuestField(questEnt, false);
        if (!(questEnt.getStatus() == EQuestStatus.CLAIMABLE.getValue())){
            return ResponseUtils.toErrorBody("Claimed !", NetWorkAPI.UNKNOWN);
        }

        if (questID == QUEST_SURVEY_ID){
            redisTemplateString.opsForSet().add(SET_USER_SURVEY, String.valueOf(userID));
        }
        recordQuest(userID, eventID, questClaimField, userID, eEventRecordType);

        List<RewardEnt> list = new ArrayList<>();
        list.addAll(inventoryService.claimItem(userID, ERewardType.findByValue(questEnt.getRewardType()), questEnt.getRewardQuantity(), EUpdateMoneyType.QUEST));
        JsonArray updateRewardBody = JsonBuilder.buildListReward(list);
        if (this.getClass().equals(DailyQuestService.class)) {
            reddotService.pushReddot(userID, EReddotFeature.DAILY_QUEST, getReddot(questEnts) - 1);
        }
        Profile profile = profileService.getProfileByID(userID);
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        JsonArray data = buildReceiveItemName(updateRewardBody);
        JsonObject eventInfo = buildEventJson(userID, eventEnt);
        GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, data, "CLAIM", redisTemplateString));
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), eventInfo, updateRewardBody, questEnt.getRewardType()).toString();
    }


    @Override
    public void listUpgradeEvent(long userID, EQuestType questType) {
        List<EventEnt> listEvent = getListEvent();
        Profile profile = profileService.getProfileByID(userID);
        int userTrophy = leaderboardService.getProfileTrophy(userID);

        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

            List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");

            for (QuestDTO questEnt : questEnts) {
                if (questEnt.getQuestType() != EQuestType.UPGRADE_HERO.getValue()) {
                    continue;
                }

                String questProgressField = getQuestField(questEnt, true);
                int progress = ConvertUtils.toInt(mapData.getOrDefault(questProgressField, "0"));

                if (progress >= questEnt.getRequire()) {
                    continue;
                }

                int after = recordQuest(userID, eventEnt.getId(), questProgressField, 1, eEventRecordType);
                if (after < questEnt.getRequire()) {
                    continue;
                }

                sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                reddotService.pushReddot(userID, EReddotFeature.DAILY_QUEST, getReddot(questEnts) + 1);

                JsonObject eventInfo = buildEventJson(userID, eventEnt);
                JsonArray updateRewardBody = buildQuestCompleteData(questEnt);
                JsonArray data = buildReceiveItemName(updateRewardBody);

                GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, data, "DONE", redisTemplateString));
            }
        }
    }
    public void recordClickSurveyLink(long userID) {
        Profile profile = profileService.getProfileByID(userID);
        int userTrophy = leaderboardService.getProfileTrophy(userID);

        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

            List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
            questEnts.stream()
                    .filter(questEnt -> questEnt.getId() == QUEST_SURVEY_ID)
                    .findFirst()
                    .ifPresent(questEnt -> processQuest(userID, eventEnt, questEnt, mapData, eEventRecordType, profile, userTrophy, questEnts));
        }
    }

    private void processQuest(long userID, EventEnt eventEnt, QuestDTO questEnt, Map<String, String> mapData,
                              EEventRecordType eEventRecordType, Profile profile, int userTrophy, List<QuestDTO> questEnts) {
        String questProgressField = getQuestField(questEnt, true);
        int progress = ConvertUtils.toInt(mapData.getOrDefault(questProgressField, "0"));

        if (progress >= questEnt.getRequire()) {
            return;
        }

        int after = recordQuest(userID, eventEnt.getId(), questProgressField, 1, eEventRecordType);
        if (after >= questEnt.getRequire()) {
            sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
            reddotService.pushReddot(userID, EReddotFeature.DAILY_QUEST, getReddot(questEnts) + 1);

            JsonObject eventInfo = buildEventJson(userID, eventEnt);
            JsonArray updateRewardBody = buildQuestCompleteData(questEnt);
            GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, updateRewardBody, "DONE", redisTemplateString));
        }
    }


    private void sendQuestCompleteData(Profile profile, EventEnt eventEnt, QuestDTO questEnt) {
        int userTrophy = leaderboardService.getProfileTrophy(profile.getId());
        JsonObject eventInfo = buildEventJson(profile.getId(), eventEnt);
        JsonArray updateRewardBody = buildQuestCompleteData(questEnt);
        JsonArray data = buildReceiveItemName(updateRewardBody);
        GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, data, "DONE", redisTemplateString));
    }


    @Override
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
                List<QuestDTO> questList = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");

                for (QuestDTO questEnt : questList) {
                    String questProgressField = getQuestField(questEnt, true);
                    int progress = ConvertUtils.toInt(mapData.get(questProgressField));

                    if (progress >= questEnt.getRequire()) return;

                    EQuestType questType = EQuestType.findByValue(questEnt.getQuestType());
                    switch (questType) {
                        case PLAY_GAME:
                            processPlayGameQuest(userID, eventEnt, profile, gameLog, questEnt, progress, eEventRecordType, winID);
                            break;
                        case MATCH_SPECIAL:
                            processMatchSpecialQuest(userID, eventEnt, questEnt, gameLog, eEventRecordType);
                            break;
                        case DEAL_1K_SKILL_DAME:
                            processSkillQuest(userID, eventEnt, gameLog, questEnt, eEventRecordType);
                            break;
                        case MATCH_GEM:
                        case MERGE_SPECIAL:
                            processMatchGemQuest(userID, eventEnt, questEnt, gameLog, eEventRecordType);
                            break;
                    }
                }
            }
        }
    }

    public void listenClaimFeature(long userID, EQuestType eQuestType, long quantity){
        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);
            List<QuestDTO> questList = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
            // filter
            questList = questList.stream().filter(e -> e.getQuestType() == eQuestType.getValue()).collect(Collectors.toList());
            for (QuestDTO questEnt : questList) {
                String questProgressField = getQuestField(questEnt, true);
                int progress = ConvertUtils.toInt(mapData.get(questProgressField));

                if (progress >= questEnt.getRequire()) return;

                EQuestType questType = EQuestType.findByValue(questEnt.getQuestType());
                switch (questType) {
                    case COLLECT_TROPHY:
                    case COLLECT_RARE_CARD:
                    case COLLECT_EPIC_CARD:
                    case COLLECT_GOLD:
                        processCollectQuest(userID, eventEnt, questEnt, eEventRecordType, quantity);
                        break;
                }
            }
        }
    }

    private void processCollectQuest(long userID, EventEnt eventEnt, QuestDTO questEnt, EEventRecordType eEventRecordType, long quantity) {
        int after = 0;

        after = recordQuest(userID, eventEnt.getId(), getQuestField(questEnt, true), quantity, eEventRecordType);
        if (after >= questEnt.getRequire()) {
            completeQuest(userID, eventEnt, questEnt);
        }
    }

    private void processSkillQuest(Long userID, EventEnt eventEnt, GameLog gameLog, QuestDTO questEnt, EEventRecordType eEventRecordType){
        int after = 0;
        JsonArray gameInfos = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
        JsonObject gameInfo = calculatePlayGameInfo(userID, gameInfos);
        JsonArray skillInfo = gameInfo.get("skillInfo").getAsJsonArray();
        AtomicInteger countSkillDmg = new AtomicInteger();
        skillInfo.forEach(e -> {
            if(e.getAsInt() > 1000) countSkillDmg.getAndIncrement();
        });
        if (questEnt.getQuestType() == EQuestType.DEAL_1K_SKILL_DAME.getValue() && countSkillDmg.get() == 0) {
            return;
        }

        after = recordQuest(userID, eventEnt.getId(), getQuestField(questEnt, true), countSkillDmg.get(), eEventRecordType);
        if (after >= questEnt.getRequire()) {
            completeQuest(userID, eventEnt, questEnt);
        }
    }

    private void processPlayGameQuest(Long userID, EventEnt eventEnt, Profile profile, GameLog gameLog, QuestDTO questEnt, int progress, EEventRecordType eEventRecordType, long winID) {
        int after = 0;

        if (questEnt.getQuestType() != EQuestType.PLAY_GAME.getValue()){
            return;
        }

        if (questEnt.isWinMultiple() && userID != winID) {
            if (progress < questEnt.getRequire()) {
                after = recordQuest(userID, eventEnt.getId(), getQuestField(questEnt, true), -progress, eEventRecordType);
            }
            return;
        }

        if (questEnt.isWin() && userID != winID ||
                questEnt.getHeroID() != 0 && questEnt.getHeroID() != profile.getSelectHero()) {
            return;
        }

        if (questEnt.isWinPerfect() && gameLog.getMatchRoundCount() != 2){
            return;
        }

        after = recordQuest(userID, eventEnt.getId(), getQuestField(questEnt, true), 1, eEventRecordType);
        if (after >= questEnt.getRequire()) {
            completeQuest(userID, eventEnt, questEnt);
        }
    }

    private void processMatchSpecialQuest(Long userID, EventEnt eventEnt, QuestDTO questEnt, GameLog gameLog, EEventRecordType eEventRecordType) {
        JsonArray gameInfos = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
        JsonObject gameInfo = calculateSpecialMatches(userID, gameInfos);

        checkAndUpdateQuest(userID, eventEnt, questEnt, gameInfo, eEventRecordType);
    }

    private void processMatchGemQuest(Long userID, EventEnt eventEnt, QuestDTO questEnt, GameLog gameLog, EEventRecordType eEventRecordType) {
        JsonArray gameInfos = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
        JsonObject gameInfo = calculateGemCounts(userID, gameInfos);

        checkAndUpdateQuest(userID, eventEnt, questEnt, gameInfo, eEventRecordType);
    }

    private JsonObject calculatePlayGameInfo(long userID, JsonArray gameInfos) {
        JsonObject gameInfo = new JsonObject();
        JsonArray skillInfo = new JsonArray();

        for (JsonElement e : gameInfos) {
            JsonArray roundsArr = e.getAsJsonObject().get("playerTurns").getAsJsonArray();
            for (JsonElement roundElement : roundsArr) {
                JsonObject roundJson = roundElement.getAsJsonObject();
                if (roundJson.has("playerId") && roundJson.get("playerId").getAsInt() == userID && roundJson.has("skillInfo")) {
                    skillInfo.addAll(roundJson.get("skillInfo").getAsJsonArray());
                }
            }
        }

        gameInfo.add("skillInfo", skillInfo);
        return gameInfo;
    }

    private JsonObject calculateSpecialMatches(Long userID, JsonArray gameInfos) {
        JsonObject gameInfo = new JsonObject();
        int lightMatch = 0, bombMatch = 0, rocketMatch = 0, lightXRocket = 0, lightXBomb = 0, rocketXBomb = 0, totalMerge = 0;

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
                    totalMerge += roundJson.get("mergeLightXHrocket").getAsInt()
                            + roundJson.get("mergeLightXVrocket").getAsInt()
                            + roundJson.get("mergeLightXBomb").getAsInt()
                            + roundJson.get("mergeRocketXRocket").getAsInt()
                            + roundJson.get("mergeBombXBomb").getAsInt()
                            + roundJson.get("mergeLightXLight").getAsInt()
                            + roundJson.get("mergeBombXVrocket").getAsInt()
                            + roundJson.get("mergeBombXHrocket").getAsInt();
                }
            }
        }

        gameInfo.addProperty("lightMatch", lightMatch);
        gameInfo.addProperty("bombMatch", bombMatch);
        gameInfo.addProperty("rocketMatch", rocketMatch);
        gameInfo.addProperty("lightXRocket", lightXRocket);
        gameInfo.addProperty("lightXBomb", lightXBomb);
        gameInfo.addProperty("rocketXBomb", rocketXBomb);
        gameInfo.addProperty("totalMerge", totalMerge);

        return gameInfo;
    }

    private JsonObject calculateGemCounts(Long userID, JsonArray gameInfos) {
        JsonObject gameInfo = new JsonObject();
        int redCount = 0, blueCount = 0, greenCount = 0, hpCount = 0, yellowCount = 0;

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

        return gameInfo;
    }

    private void checkAndUpdateQuest(Long userID, EventEnt eventEnt, QuestDTO questEnt, JsonObject gameInfo, EEventRecordType eEventRecordType) {
        Map<String, Integer> conditions = new HashMap<>();
        conditions.put("lightMatch", questEnt.getLightMatch());
        conditions.put("bombMatch", questEnt.getBombMatch());
        conditions.put("rocketMatch", questEnt.getRocketMatch());
        conditions.put("lightXRocket", questEnt.getLightRocket());
        conditions.put("lightXBomb", questEnt.getLightBomb());
        conditions.put("rocketXBomb", questEnt.getRocketBomb());
        conditions.put("redCount", questEnt.getRedCount());
        conditions.put("blueCount", questEnt.getBlueCount());
        conditions.put("greenCount", questEnt.getGreenCount());
        conditions.put("hpCount", questEnt.getHpCount());
        conditions.put("yellowCount", questEnt.getYellowCount());
        conditions.put("totalMerge", questEnt.getTotalMerge());

        for (Map.Entry<String, Integer> entry : conditions.entrySet()) {
            String key = entry.getKey();
            int required = entry.getValue();
            if (required != 0 && gameInfo.has(key) && gameInfo.get(key).getAsInt() > 0) {
                int after = recordQuest(userID, eventEnt.getId(), getQuestField(questEnt, true), gameInfo.get(key).getAsInt(), eEventRecordType);
                if (after >= questEnt.getRequire()) {
                    completeQuest(userID, eventEnt, questEnt);
                }
                return;
            }
        }
    }

    private void completeQuest(Long userID, EventEnt eventEnt, QuestDTO questEnt) {
        // Emit the next message
        sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);

        // Deserialize event custom data
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);

        // Get the event quest
        List<QuestDTO> questList = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");

        // Compute reddot
        int reddot = getReddot(questList) + 1;
        reddotService.pushReddot(userID, EReddotFeature.DAILY_QUEST, reddot);

        // Get profile and send quest complete data
        Map<Long, Profile> mapProfile = profileService.getMapByListId(Collections.singletonList(userID));
        Profile profile = mapProfile.get(userID);
        sendQuestCompleteData(profile, eventEnt, questEnt);
    }


    public String initDailyQuestFTUE(Profile profile){
        List<EventEnt> listEvent = getListEvent();
        List<Integer> listQuest = new ArrayList<>();
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
            // Batlle 5 matches
            listQuest.add(2);
            listQuest.add(4);
            listQuest.add(7);
            listQuest.add(10);
            listQuest.add(13);
            String listQuestString = JSONUtil.Serialize(listQuest);
            String hashEventKey = getHashEventKey(profile.getId(), eventEnt.getId(), eEventRecordType);
            redisTemplateString.opsForHash().put(hashEventKey, FIELD_LIST_QUEST, listQuestString);

            String questProgressField = getQuestField(2, true);
            long delta = 1 + (profile.getBattleWon() + profile.getLoseStreak());
            delta = delta < 5 ? delta : 4;
            recordQuest(profile.getId(), eventEnt.getId(), questProgressField, delta, eEventRecordType);
        }
        return JSONUtil.Serialize(listQuest);
    }

    private String initDailyQuest(long userID, int eventID, EEventRecordType eEventRecordType) {
        if (mapQuestPool.isEmpty()) {
            initQuestPool();
        }
        Profile profile = profileService.getMinProfileByID(userID);
        String hashEventKey = getHashEventKey(userID, eventID, eEventRecordType);
        if (profileService.isFUTEquest(profile)){
            return initDailyQuestFTUE(profile);
        }
        List<HeroEnt> listHero = inventoryService.getListOwnedHero(userID);
        List<Integer> listHeroOwned = listHero.stream().map(e -> e.getId()).collect(Collectors.toList());
        List<Integer> listQuest = new ArrayList<>();
        {
            List<QuestEnt> poolQuestClone = new ArrayList<>();
            poolQuestClone.addAll(mapQuestPool.get(1));
            poolQuestClone.removeIf(e -> e.getHeroId() != 0 && !listHeroOwned.contains(e.getHeroId()));
            Collections.shuffle(poolQuestClone);
            listQuest.add(poolQuestClone.get(0).getId());
            listQuest.add(poolQuestClone.get(1).getId());
        }
        {
            List<QuestEnt> poolQuestClone = new ArrayList<>();
            poolQuestClone.addAll(mapQuestPool.get(2));
            poolQuestClone.removeIf(e -> e.getHeroId() != 0 && !listHeroOwned.contains(e.getHeroId()));
            Collections.shuffle(poolQuestClone);
            listQuest.add(poolQuestClone.get(0).getId());
            listQuest.add(poolQuestClone.get(1).getId());
        }
        {
            boolean isGenericQuest = RandomUtils.nextBoolean();
            if (isGenericQuest){
                int questID = RandomUtils.nextBoolean() ? WIN_HERO_QUEST_ID : DEAL_DMG_HERO_QUEST_ID;
                int require;
                if (questID == WIN_HERO_QUEST_ID){
                    List<Integer> listRequire = Arrays.asList(10000,15000,20000);
                    require = listRequire.get(RandomUtils.nextInt(listRequire.size() - 1));
                } else {
                    List<Integer> listRequire = Arrays.asList(1,3,5);
                    require = listRequire.get(RandomUtils.nextInt(listRequire.size() - 1));
                }
                int heroID = listHeroOwned.get(RandomUtils.nextInt(listHeroOwned.size() - 1));
                redisTemplateString.opsForHash().put(hashEventKey, FIELD_QUEST_HERO_ID, heroID);
                redisTemplateString.opsForHash().put(hashEventKey, FIELD_QUEST_HERO_REQUIRE, require);
            } else {
                List<QuestEnt> poolQuestClone = new ArrayList<>();
                poolQuestClone.addAll(mapQuestPool.get(3));
                poolQuestClone.removeIf(e -> e.getHeroId() != 0 && !listHeroOwned.contains(e.getHeroId()));
                Collections.shuffle(poolQuestClone);
                listQuest.add(poolQuestClone.get(0).getId());
            }
        }
        String listQuestString = JSONUtil.Serialize(listQuest);
        redisTemplateString.opsForHash().put(hashEventKey, FIELD_LIST_QUEST, listQuestString);
        return JSONUtil.Serialize(listQuest);
    }
    
    public void initQuestPool() {
        // Đọc tất cả quest từ repository
        List<QuestEnt> questEnts = questPoolRepository.read().findAll();
        listQuestPool.addAll(questEnts);

        // Phân loại quest theo tier
        Map<Integer, List<QuestEnt>> tierMap = questEnts.stream()
                .collect(Collectors.groupingBy(QuestEnt::getTier));

        // Cập nhật mapQuestPool với các danh sách tier
        mapQuestPool.put(1, tierMap.getOrDefault(1, new ArrayList<>()));
        mapQuestPool.put(2, tierMap.getOrDefault(2, new ArrayList<>()));
        mapQuestPool.put(3, tierMap.getOrDefault(3, new ArrayList<>()));
    }

}
