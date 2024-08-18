/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.ent.common.QuestDTO;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.EEventRecordType;
import cc.bliss.match3.service.gamemanager.ent.enums.EQuestStatus;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.ent.enums.ETriggerType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.DailyRewardTrackingCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class DailyRewardService extends QuestEventService {

    private final String DAILY_COUNT = "daily_count";
    private final String IS_RECORDED = "is_recorded_%s";

    @Autowired
    private ProfileService profileService;
    @Autowired
    private LeaderboardService leaderboardService;

    @Override
    public void listenLogin(Profile session) {
        long userID = session.getId();
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.DAILY_REWARD.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        List<EventEnt> listEvent = triggerService.getListEvent(listEventId);
        for (EventEnt eventEnt : listEvent) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            JsonArray questJsonArr = customData.get("quest").getAsJsonArray();
            Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), EEventRecordType.NONE);
            String isRecorded = String.format(IS_RECORDED, DateTimeUtils.getNow(DATE_FORMAT));
            if (!mapData.containsKey(isRecorded)) {
                //for tracking data
                Calendar calendar = Calendar.getInstance();
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                if (dayOfMonth == 7 || dayOfMonth == 14 || dayOfMonth == 21 || dayOfMonth == 28) {
                    Profile profile = profileService.getMinProfileByID(userID);
                    int userTrophy = leaderboardService.getProfileTrophy(userID);
                    long goldBeforeAction = profile.getMoney();
                    long emeraldBeforeAction = profile.getEmerald();
                    String actionType = "DONE_STREAK_" + dayOfMonth + "D";
                    JsonObject dailyRewardLogin = questJsonArr.get(dayOfMonth - 1).getAsJsonObject();
                    GMLocalQueue.addQueue(new DailyRewardTrackingCmd(producer, profile, userTrophy, actionType, null, dailyRewardLogin, System.currentTimeMillis(), goldBeforeAction, emeraldBeforeAction, getRewardName(dailyRewardLogin), redisTemplateString));
                }

                //----------------
                String hashKey = getHashEventKey(userID, eventEnt.getId(),EEventRecordType.NONE);
                redisTemplateString.opsForHash().put(hashKey,isRecorded,"1");
                int dailyCount = recordQuest(userID, eventEnt.getId(), DAILY_COUNT, 1, EEventRecordType.NONE);
                if (dailyCount > questJsonArr.size()){
                    // reset daily reward
                    redisTemplateString.delete(hashKey);
                    recordQuest(userID, eventEnt.getId(), DAILY_COUNT, 1, EEventRecordType.NONE);
                }
            }
        }
    }

    private String getRewardName(JsonObject dailyRewardLogin) {
        ERewardType rewardType = ERewardType.findByValue(dailyRewardLogin.get("rewardType").getAsInt());
        RewardEnt rewardEnt = new RewardEnt();
        rewardEnt.setERewardType(rewardType);
        return rewardEnt.getTitle();
    }

    public void deleteRedisKey(long userID){
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.DAILY_REWARD.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        for (int eventID : listEventId) {
            String key = getHashEventKey(userID, eventID, EEventRecordType.NONE);
            redisTemplateString.delete(key);
        }
    }

    public JsonObject getCurrentDailyReward(long userID) {
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.DAILY_REWARD.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        List<EventEnt> listEvent = triggerService.getListEvent(listEventId);
        for (EventEnt eventEnt : listEvent) {
            JsonObject eventJson = buildEventJson(userID, eventEnt);

            return eventJson;
        }
        return new JsonObject();
    }

    @Override
    protected JsonObject buildEventJson(long userID, EventEnt eventEnt) {
        JsonObject eventJson = new JsonObject();
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        TriggerEnt triggerEnt = triggerService.getTriggerByEventID(eventEnt.getId());
        EEventRecordType eEventRecordType = EEventRecordType.findByName(customData.get("recordType").getAsString());
        Map<String, String> mapData = getMapEventData(userID, eventEnt.getId(), EEventRecordType.NONE);
        List<QuestDTO> questEnts = getEventQuest(userID, eventEnt.getId(), customData, eEventRecordType, mapData, "quest", 0);
        JsonArray questJsonArr = buildListQuestJson(questEnts);
        JsonObject eventMilestone = buildEventMileStone(userID, eventEnt.getId(), customData, eEventRecordType, mapData, questEnts);

        eventJson.addProperty("id", eventEnt.getId());
        eventJson.addProperty("title", eventEnt.getTitle());
        eventJson.addProperty("endTime", getEndTime(triggerEnt, eEventRecordType));
        eventJson.addProperty("startTime", getStartTime(triggerEnt, eEventRecordType));
        eventJson.add("dailyReward", questJsonArr);
        eventJson.add("eventMilestone", eventMilestone);
        return eventJson;
    }

    @Override
    protected JsonArray buildListQuestJson(List<QuestDTO> questEnts) {
        JsonArray jsonArray = new JsonArray();
        int monthLength = DateTimeUtils.getLengthOfMonth();
        for (QuestDTO questEnt : questEnts) {
            JsonObject questJson = new JsonObject();
            questJson.addProperty("id", questEnt.getId());
            questJson.addProperty("title", questEnt.getTitle());
            questJson.addProperty("status", questEnt.getStatus());
            questJson.addProperty("rewardType", questEnt.getRewardType());
            questJson.addProperty("rewardTitle", questEnt.getRewardTitle());
            questJson.addProperty("rewardQuantity", questEnt.getRewardQuantity());
            questJson.addProperty("progress", questEnt.getProgress());
            questJson.addProperty("require", questEnt.getRequire());
            questJson.addProperty("nextClaim", questEnt.getNextClaim());
            jsonArray.add(questJson);
            if (jsonArray.size() == monthLength) {
                break;
            }
        }
        return jsonArray;
    }

    protected JsonArray buildListQuestJson(List<QuestDTO> questEnts, long progress) {
        JsonArray jsonArray = new JsonArray();
        int monthLength = DateTimeUtils.getLengthOfMonth();
        for (QuestDTO questEnt : questEnts) {
            JsonObject questJson = new JsonObject();
            questJson.addProperty("id", questEnt.getId());
            questJson.addProperty("title", questEnt.getTitle());
            questJson.addProperty("status", questEnt.getStatus());
            questJson.addProperty("rewardType", questEnt.getRewardType());
            questJson.addProperty("rewardTitle", questEnt.getRewardTitle());
            questJson.addProperty("rewardQuantity", questEnt.getRewardQuantity());
            questJson.addProperty("progress", questEnt.getProgress());
            questJson.addProperty("require", questEnt.getRequire());
            questJson.addProperty("nextClaim", questEnt.getNextClaim());
            jsonArray.add(questJson);
            if (jsonArray.size() == monthLength) {
                break;
            }
        }
        return jsonArray;
    }

    @Override
    protected List<EventEnt> getListEvent() {
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.DAILY_REWARD.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        List<EventEnt> listEvent = triggerService.getListEvent(listEventId);
        return listEvent;
    }

    protected JsonObject buildEventMileStone(long userID, int eventID, JsonObject customData, EEventRecordType eEventRecordType, Map<String, String> mapData, List<QuestDTO> questEnts) {
        JsonObject jsonObject = new JsonObject();
        int progress = ConvertUtils.toInt(questEnts.stream().filter(e -> e.getStatus() == EQuestStatus.CLAIMED.getValue()).count());
        long totalProgress = questEnts.size();
        List<QuestDTO> milestone = getEventQuest(userID, eventID, customData, eEventRecordType, mapData, "eventProgress", progress);
        JsonArray questJsonArr = buildListQuestJson(milestone, progress);

        jsonObject.addProperty("progress", progress);
        jsonObject.addProperty("total", totalProgress);
        jsonObject.add("milestone", questJsonArr);

        return jsonObject;
    }

    public List<QuestDTO> getEventQuest(long userID, int eventID, JsonObject customData, EEventRecordType eEventRecordType, Map<String, String> mapData, String type, int progress) {
        List<QuestDTO> questList = new ArrayList<>();
        // load event data
        JsonArray questJsonArr = customData.get(type).getAsJsonArray();
        if (questJsonArr == null) {
            return questList;
        }
        int currentDate = ConvertUtils.toInt(mapData.get(DAILY_COUNT), 1);

        for (JsonElement el : questJsonArr) {
            QuestDTO questRawData = jsonToSingleQuestRawData(el.getAsJsonObject(), userID);
            if (type.contentEquals("eventProgress")) {
                questRawData = addMileStoneProgress(questRawData, eventID, userID, mapData, progress);
            } else {
                questRawData = addQuestProgress(questRawData, eventID, userID, mapData, currentDate, type);
            }
            questList.add(questRawData);
        }
        return questList;
    }

    public QuestDTO addMileStoneProgress(QuestDTO questRawData, int eventID, long userID, Map<String, String> mapData, int progress) {

        String fieldClaimedKeyKey = getQuestField(questRawData, false);
        boolean isClaimed = mapData.containsKey(fieldClaimedKeyKey);
        int require = questRawData.getRequire();
        int status = 0;

        if (isClaimed) {
            status = EQuestStatus.CLAIMED.getValue();
        } else {
            status = (progress > require)
                    ? EQuestStatus.CLAIMABLE.getValue()
                    : EQuestStatus.PROGRESS.getValue();
        }

        if (progress > require) {
            progress = require;
        }
        questRawData.setProgress(progress);
        questRawData.setRequire(require);
        questRawData.setStatus(status);
        return questRawData;
    }

    public QuestDTO addQuestProgress(QuestDTO questRawData, int eventID, long userID, Map<String, String> mapData, int currentDate, String type) {
        if (type.contentEquals("eventProgress")) {
            return super.addQuestProgress(questRawData, eventID, userID, mapData);
        }
        String fieldClaimedKeyKey = getQuestField(questRawData, false);
        boolean isClaimed = mapData.containsKey(fieldClaimedKeyKey);
        int require = questRawData.getRequire();
        int status = 0;

        if (isClaimed) {
            status = EQuestStatus.CLAIMED.getValue();
        } else {
            if (questRawData.getId() == currentDate) {
                status = EQuestStatus.CLAIMABLE.getValue();
            } else if (questRawData.getId() < currentDate) {
                status = EQuestStatus.RECLAIM.getValue();
            } else {
                status = EQuestStatus.PROGRESS.getValue();
            }
            if (questRawData.getId() - currentDate == 1) {
                questRawData.setNextClaim(DateTimeUtils.getBeginDate(DateTimeUtils.addDays(1)).getTime());
            }
        }
        questRawData.setRequire(require);
        questRawData.setStatus(status);
        return questRawData;
    }

    @Override
    public String claimEvent(long userID, int eventID, int questID, boolean isReclaim) {
        return super.claimEvent(userID, eventID, questID, isReclaim); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

}
