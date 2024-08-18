package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.QuestDTO;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.QuestTrackingCmd;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static cc.bliss.match3.service.gamemanager.constant.GameConstant.ADS_QUEST_ID;
import static cc.bliss.match3.service.gamemanager.constant.GameConstant.QUEST_SURVEY_ID;

@Service
public class WatchAdsQuestService extends QuestEventService{

    @Autowired
    private DailyQuestService dailyQuestService;

    @Override
    public List<QuestDTO> getEventQuest(long userID, int eventID, JsonObject customData, EEventRecordType eEventRecordType, Map<String, String> mapData, String type) {
        // Ads quest
        List<QuestDTO> finalQuestList = new ArrayList<>();
        if (ModuleConfig.IS_TEST){
            QuestDTO questRawData = new QuestDTO();
            questRawData.setId(ADS_QUEST_ID);
            questRawData.setTitle("LEGENDARY HERO!");
            questRawData.setQuestDetail("localize key");
            questRawData.setQuestType(EQuestType.ADS_QUEST.getValue());
            questRawData.setRewardType(ERewardType.LEGENDARY_CARD.getValue());
            questRawData.setRewardQuantity(1);
            questRawData.setRequire(5);
            questRawData.setButtonData("");
            questRawData = addQuestProgress(questRawData, eventID, userID, mapData);
            finalQuestList.add(questRawData);
        }
        return finalQuestList;
    }

    @Override
    protected JsonObject buildEventJson(long userID, EventEnt eventEnt) {
        JsonObject eventJson = new JsonObject();
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        TriggerEnt triggerEnt = triggerService.getTriggerByEventID(eventEnt.getId());
        EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), EEventRecordType.NONE);

        List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, EEventRecordType.NONE, mapData, "quest");
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
        String fieldProgressKey = getQuestField(questRawData, true);
        String fieldNextWatchAds = FIELD_NEXT_WATCH_ADS;
        int progress = ConvertUtils.toInt(mapData.get(fieldProgressKey));
        long nextWatchAds = ConvertUtils.toLong(mapData.get(fieldNextWatchAds));
        int require = questRawData.getRequire();
        int status = (progress < require)
                ? EQuestStatus.PROGRESS.getValue()
                : EQuestStatus.CLAIMABLE.getValue();

        if (progress > require) {
            progress = require;
        }
        questRawData.setProgress(progress);
        questRawData.setRequire(require);
        questRawData.setStatus(status);
        questRawData.setNextWatchAds(nextWatchAds);
        return questRawData;
    }

    @Override
    protected List<EventEnt> getListEvent() {
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.DAILY_QUEST.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        List<EventEnt> listEvent = triggerService.getListEvent(listEventId);
        return listEvent;
    }

    public String recordWatchAds(long userID){
        List<EventEnt> listEvent = getListEvent();
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), EEventRecordType.NONE);

            List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, EEventRecordType.NONE, mapData, "quest");
            Optional<QuestDTO> optional = questEnts.stream().filter(e -> e.getId() == ADS_QUEST_ID).findFirst();
            questEnts.forEach(e -> System.out.println(e.getId() + ": " + e.getTitle()));
            if (optional.isPresent()) {
                QuestDTO questEnt = optional.get();
                String questProgressField = getQuestField(questEnt, true);
                if (questEnt.getProgress() >= questEnt.getRequire()) {
                    return ResponseUtils.toErrorBody("Need claim reward", NetWorkAPI.UNKNOWN);
                }
                if (questEnt.getNextWatchAds() > System.currentTimeMillis()){
                    return ResponseUtils.toErrorBody("Waiting for next ads", NetWorkAPI.UNKNOWN);
                }
                int after = recordQuest(userID, eventEnt.getId(), questProgressField, 1, EEventRecordType.NONE);
                String hashEventKey = getHashEventKey(userID, eventEnt.getId(), EEventRecordType.NONE);
                redisTemplateString.opsForHash().put(hashEventKey, FIELD_NEXT_WATCH_ADS, String.valueOf(System.currentTimeMillis() + Duration.ofSeconds(5).toMillis()));
                JsonObject eventInfo = buildEventJson(userID, eventEnt);
                if (after >= questEnt.getRequire()) {
                    sseService.emitNextMsg(buildSSEQuestJson(questEnt), userID);
                    reddotService.pushReddot(userID, EReddotFeature.DAILY_QUEST, getReddot(questEnts) + 1);
                    // log
                    Profile profile = profileService.getProfileByID(userID);
                    int userTrophy = leaderboardService.getProfileTrophy(userID);
                    JsonArray updateRewardBody = buildQuestCompleteData(questEnt);
                    GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, updateRewardBody, "DONE", redisTemplateString));
                }
                List<RewardEnt> rewardEntList = inventoryService.updateMoney(userID,50, EUpdateMoneyType.QUEST);
                JsonArray updateRewardBody = JsonBuilder.buildListReward(rewardEntList);
                GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, userID, EQuestType.WATCH_ADS_QUEST, 1));
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), eventInfo, updateRewardBody, questEnt.getRewardType()).toString();
            }
        }
        return ResponseUtils.toErrorBody(HttpStatus.NOT_FOUND.name(), NetWorkAPI.UNKNOWN);
    }

    @Override
    public String claimEvent(long userID, int eventID, int questID, boolean isReclaim) {
        EventEnt eventEnt = triggerService.getEvent(eventID);
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        EEventRecordType eEventRecordType = EEventRecordType.NONE;
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), eEventRecordType);
        List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest");
        questEnts.forEach(e -> System.out.println(e.getId() + ": " + e.getTitle() + ": " + e.getStatus() + ": " + e.getProgress()));
        QuestDTO questEnt = questEnts.stream().filter(e -> e.getId() == questID).findFirst().get();
        String questProgressField = getQuestField(questEnt, true);
        if (!(questEnt.getStatus() == EQuestStatus.CLAIMABLE.getValue())){
            return ResponseUtils.toErrorBody("Claimed !", NetWorkAPI.UNKNOWN);
        }

        // reset progress
        String hashEventKey = getHashEventKey(userID, eventEnt.getId(), eEventRecordType);
        redisTemplateString.opsForHash().put(hashEventKey, questProgressField, String.valueOf(0));
        // build reward response
        List<RewardEnt> list = new ArrayList<>();
        list.addAll(inventoryService.claimItem(userID, ERewardType.findByValue(questEnt.getRewardType()), questEnt.getRewardQuantity(), EUpdateMoneyType.QUEST));
        JsonArray updateRewardBody = JsonBuilder.buildListReward(list);
        JsonObject eventInfo = buildEventJson(userID, eventEnt);
        // update reddot
        reddotService.pushReddot(userID, EReddotFeature.DAILY_QUEST, getReddot(questEnts) - 1);
        // >>> Tracking: start
        Profile profile = profileService.getProfileByID(userID);
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        JsonArray data = buildReceiveItemName(updateRewardBody);
        GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, userTrophy, eventInfo, questEnt, data, "CLAIM", redisTemplateString));
        // >>> Tracking: end
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), eventInfo, updateRewardBody, questEnt.getRewardType()).toString();
    }
}
