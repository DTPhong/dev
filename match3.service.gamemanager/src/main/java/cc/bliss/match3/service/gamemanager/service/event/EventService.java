/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.event;

import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.ETriggerType;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Phong
 */
@Service
public class EventService extends BaseService {

    @Autowired
    TriggerService triggerService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private QuestEventService questEventService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private DailyRewardService dailyRewardService;
    @Autowired
    private DailyQuestService dailyQuestService;
    @Autowired
    private WatchAdsQuestService watchAdsQuestService;

    public String recordSurvey(){
        SessionObj session = adminService.getSession();
        dailyQuestService.recordClickSurveyLink(session.getId());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UNKNOWN);
    }

    public String getCurrentEvent() {
        SessionObj session = adminService.getSession();
        JsonArray jsonArray = dailyQuestService.getCurrentEvent(session.getId());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonArray, NetWorkAPI.UNKNOWN);
    }

    public String claimEvent(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        String response;
        try {
            if (!lockUtil.acquireLock(session.getId(), NetWorkAPI.CLAIM_DAILY_EVENT)){
                return ResponseUtils.toErrorBody("Too many request !!!", NetWorkAPI.CLAIM_DAILY_EVENT);
            }
            JsonObject jsonObject = RequestUtils.requestToJson(request);
            int eventID = jsonObject.get("eventID").getAsInt();
            int questID = jsonObject.get("questID").getAsInt();
            response = dailyQuestService.claimEvent(session.getId(), eventID, questID, false);
        } catch (Exception e){
            response = ResponseUtils.toErrorBody(e.getMessage(), NetWorkAPI.CLAIM_DAILY_EVENT);
        } finally {
            lockUtil.releaseLock(session.getId(), NetWorkAPI.CLAIM_DAILY_EVENT);
        }
        return response;
    }

    public String watchAds(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        return watchAdsQuestService.recordWatchAds(session.getId());
    }

    public String getDailyReward() {
        SessionObj session = adminService.getSession();
        JsonObject jsonObject = dailyRewardService.getCurrentDailyReward(session.getId());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.UNKNOWN);
    }

    public String claimDailyReward(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        TriggerEnt triggerEnt = triggerService.getCurrentSingleTrigger(ETriggerType.DAILY_REWARD.getValue());
        if (triggerEnt == null) {
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), new JsonObject(), NetWorkAPI.UNKNOWN);
        }
        int eventID = triggerEnt.getRefId();
        int questID = jsonObject.get("questID").getAsInt();
        boolean isReclaim = false;
        if (jsonObject.get("isReclaim") != null) {
            isReclaim = jsonObject.get("isReclaim").getAsBoolean();
        }
        return dailyRewardService.claimEvent(session.getId(), eventID, questID, isReclaim);
    }

    public List<EventEnt> getListEventByTriggerType(ETriggerType eTriggerType){
        List<TriggerEnt> triggerEnts = triggerService.getCurrentListTrigger(eTriggerType.getValue());
        List<Integer> listEventIDs = new ArrayList<>();
        for (int i = 0; i < triggerEnts.size(); i++) {
            listEventIDs.add(triggerEnts.get(i).getRefId());
        }
        return triggerService.getListEvent(listEventIDs);
    }
}
