package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.common.QuestDTO;
import cc.bliss.match3.service.gamemanager.ent.data.DailyRewardDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@AllArgsConstructor
public class DailyRewardTrackingCmd implements QueueCommand {
    Producer producer;
    Profile user;
    int userTrophy;
    String actionType;
    QuestDTO questDTO;
    JsonObject loginAction;
    long endTime;
    long goldBeforeAction;
    long emeraldBeforeAction;
    String rewardName;
    RedisTemplate<String, String> redisTemplateString;

    @Override
    public void execute() {
        JsonObject rewardBody = buildDailyRewardBody(questDTO, loginAction);
        UserDetect userDetect = TrackingDataUtil.getUserDetect(user.getId(), redisTemplateString);
        DailyRewardDataEnt dailyRewardDataEnt = DailyRewardDataEnt.builder()
                .actionAtMs(System.currentTimeMillis())
                .actionType(actionType)
                .dailyRewardId(buildDailyRewardId(questDTO, loginAction))
                .rewardAmount(rewardBody.get("quantity").getAsInt())
                .rewardId(rewardBody.get("id").getAsInt())
                .rewardName(rewardName)
                .rewardType(ERewardType.findByValue(rewardBody.get("type").getAsInt()).name())
                .userId(user.getId())
                .userDisplayName(user.getUsername())
                .userGold(goldBeforeAction)
                .userEmerald(emeraldBeforeAction)
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
                .build();
        producer.sendDailyRewardMessage(dailyRewardDataEnt);
    }

    private String buildDailyRewardId(QuestDTO questDTO, JsonObject loginAction) {
        Instant instant = Instant.ofEpochMilli(endTime);
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        int year = ldt.getYear();
        String month = ldt.getMonthValue() < 10 ? "0" + ldt.getMonthValue() : String.valueOf(ldt.getMonthValue());
        int questId;
        if (questDTO != null) {
            questId = questDTO.getId();
        } else {
            questId = loginAction.get("id").getAsInt();
        }
        return year + month + questId;
    }

    private JsonObject buildDailyRewardBody(QuestDTO questDTO, JsonObject loginAction) {
        JsonObject jsonObject = new JsonObject();
        if (questDTO != null) {

            jsonObject.addProperty("quantity", questDTO.getRewardQuantity());
            jsonObject.addProperty("id", questDTO.getId());
            jsonObject.addProperty("type", questDTO.getRewardType());
        } else {
            jsonObject.addProperty("quantity", loginAction.get("rewardQuantity").getAsInt());
            jsonObject.addProperty("id", loginAction.get("id").getAsInt());
            jsonObject.addProperty("type", loginAction.get("rewardType").getAsInt());
        }
        return jsonObject;
    }
}
