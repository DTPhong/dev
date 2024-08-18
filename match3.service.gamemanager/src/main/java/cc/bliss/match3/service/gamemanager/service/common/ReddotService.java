/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import bliss.lib.framework.util.ConvertUtils;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.EReddotFeature;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.SSEService;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Phong
 */
@Service
public class ReddotService extends BaseService {

    private static final String HASH_REDDOT_USER_INFO = "reddot:%s";
    @Autowired
    private AdminService adminService;
    @Autowired
    private SSEService sSEService;
    @Autowired
    private DailyQuestService dailyQuestService;

    private String getRedisKey(long userID) {
        return String.format(HASH_REDDOT_USER_INFO, userID);
    }

    public String getReddot() {
        SessionObj session = adminService.getSession();
        String redisKey = getRedisKey(session.getId());
        HashOperations<String, String, String> hashOperations = redisTemplateString.opsForHash();
        Map<String, String> mapReddot = hashOperations.entries(redisKey);
        JsonArray response = buildReddotArrJson(mapReddot, session.getId());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.GET_REDDOT);
    }

    private JsonArray buildReddotArrJson(Map<String, String> mapReddot, long userID) {
        JsonArray reddotJsonArr = new JsonArray();
        for (EReddotFeature e : EReddotFeature.values()) {
            int value = 0;
            if (e.equals(EReddotFeature.DAILY_QUEST)) {
                value = dailyQuestService.getReddot(userID);
            } else {
                value = ConvertUtils.toInt(mapReddot.get(String.valueOf(e.getValue())));
                if (value < 0) {
                    value = 0;
                }
            }
            JsonObject reddotJson = buildReddotJson(e.getValue(), value);
            reddotJsonArr.add(reddotJson);
        }
        return reddotJsonArr;
    }

    private JsonObject buildReddotJson(int feature, int value) {
        JsonObject reddotJson = new JsonObject();
        reddotJson.addProperty("feature", feature);
        reddotJson.addProperty("value", value);
        return reddotJson;
    }

    public void updateReddot(long userID, EReddotFeature feature, int value) {
        String redisKey = getRedisKey(userID);
        HashOperations<String, String, String> hashOperations = redisTemplateString.opsForHash();
        int cur = hashOperations.increment(redisKey, String.valueOf(feature.getValue()), value).intValue();
        if (cur < 0) {
            cur = 0;
            hashOperations.delete(redisKey, String.valueOf(feature.getValue()));
        }
        JsonObject response = buildReddotJson(feature.getValue(), cur);
        String msg = ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UPDATE_REDDOT);
        sSEService.emitNextMsg(msg, userID);
    }

    public void pushReddot(long userID, EReddotFeature feature, int value) {
        JsonObject response = buildReddotJson(feature.getValue(), value);
        String msg = ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UPDATE_REDDOT);
        sSEService.emitNextMsg(msg, userID);
    }
}
