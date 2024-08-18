/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.system;

import bliss.lib.framework.util.NetworkUtils;
import cc.bliss.match3.service.gamemanager.config.RestTemplateConfig;
import cc.bliss.match3.service.gamemanager.util.GoogleUtils;
import cc.bliss.match3.service.gamemanager.util.UserActivityUtil;
import com.google.gson.JsonObject;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Phong
 */
@Service
public class SSEService {

    public static final String GLOBAL_CHANNEL = "globallChannel";
    public static final String USER_CHANNEL = "sse:%s";
    private final Map<String, Sinks.Many<String>> mapChannelProducer = new NonBlockingHashMap<>();

    /**
     * emit msg to specific channel
     *
     * @param msg
     * @param channelID
     */
    public void emitNextMsg(String msg, String channelID) {
        pushSSE(channelID,msg);
    }

    /**
     * emit msg to user channel
     *
     * @param msg
     */
    public void emitNextMsg(String msg, long userID) {
        String channelID = String.format(USER_CHANNEL, userID);
        pushSSE(channelID,msg);
    }

    public void checkSSEExpire() {
        UserActivityUtil userActivityUtil = UserActivityUtil.getInstance();
        Map<Long, Long> userMap = userActivityUtil.lastActivityCache().asMap();

        for (Map.Entry<Long, Long> entry : userMap.entrySet()) {
            long userID = entry.getKey();
            if (userActivityUtil.isInactive(userID)) {
                deleteSSE(userID);
            }
        }
    }

    private String getSSEInternalUrl() {
        String gkeConfig = "GKE_" + GoogleUtils.getRegionName();
        JsonObject data = RestTemplateConfig.k8sConfigMap.get(gkeConfig);
        return data.get("sse_internal_url").getAsString();
    }

    private void deleteSSE(long userID) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userID", userID);
        String sseInternalIp = getSSEInternalUrl();
        NetworkUtils.doPost(sseInternalIp + "/sse/stream/delete-user", new HashMap<>(), jsonObject);
    }

    private void pushSSE(String channelID, String msg) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("chatChannel", channelID);
        jsonObject.addProperty("msg", msg);
        String sseInternalIp = getSSEInternalUrl();
        NetworkUtils.doPost(sseInternalIp + "/sse/stream/post", new HashMap<>(), jsonObject);
    }
}
