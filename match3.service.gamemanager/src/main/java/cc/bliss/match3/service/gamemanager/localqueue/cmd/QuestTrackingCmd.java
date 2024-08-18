package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.common.QuestDTO;
import cc.bliss.match3.service.gamemanager.ent.data.QuestDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

@AllArgsConstructor
public class QuestTrackingCmd implements QueueCommand {
    Producer producer;
    Profile user;
    int userTrophy;
    JsonObject eventInfo;
    QuestDTO questDTO;
    JsonArray rewards;
    String actionType;
    RedisTemplate<String, String> redisTemplateString;
    @Override
    public void execute() {
        JsonArray data = buildReward(rewards);
        UserDetect userDetect = TrackingDataUtil.getUserDetect(user.getId(), redisTemplateString);

        // add default value if quest is not belongs to any event
        if(eventInfo != null && !eventInfo.has("id")) {
            eventInfo.addProperty("id", -1);
        }
        if(eventInfo != null && !eventInfo.has("title")) {
            eventInfo.addProperty("title", "-");
        }

        QuestDataEnt questDataEnt = QuestDataEnt.builder()
                .userId(user.getId())
                .userDisplayName(user.getUsername())
                .questName(questDTO.getTitle())
                .questId(questDTO.getId())
                .questExpiredAtMs(eventInfo.get("endTime").getAsLong())
                .actionType(actionType)
                .actionAtMs(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .userGold(user.getMoney())
                .userEmerald(user.getEmerald())
                .userTrophy(userTrophy)
                .userWinLoseStreak(user.getWinStreak())
                .userCreatedAtMs(user.getDateCreated() != null ? user.getDateCreated().getTime() : System.currentTimeMillis())
                .userIsNew(user.getIsNew())
                .userPlatform("-")
                .userVersion("-")
                .userApp("-")
                .userDevice(user.getDeviceID())
                .userIp(userDetect.getIp())
                .userCountryCode(userDetect.getCountryCode())
                .userCurrencyCode("VND")
                .userUtmSource("-")
                .userUtmCampaign("-")
                .botHardMode(EBotType.fromValue(user.getBotType()).name())
                .listItemReceive(data)
                .questTypeId(eventInfo.get("id").getAsInt())
                .questTypeName(eventInfo.get("title").getAsString())
                .build();
        producer.sendQuestMessage(questDataEnt);
    }

    private JsonArray buildReward(JsonArray rewards) {
        JsonArray result = new JsonArray();
        for (JsonElement item : rewards) {
            JsonObject data = new JsonObject();
            JsonObject reward = item.getAsJsonObject();
            data.addProperty("item_receive_id", reward.get("rewardType").getAsInt());
            data.addProperty("item_receive_amount", reward.get("delta").getAsInt());
            data.addProperty("item_receive_name", reward.get("rewardTitle").getAsString());
            result.add(data);
        }
        return result;
    }
}
