package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.data.TrophyRoadDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class TrophyRoadTrackingCmd implements QueueCommand {

    Producer producer;
    Profile user;
    int userTrophy;
    int trophyMilestone;
    int actionType;
    List<RewardEnt> rewardEntList;
    RedisTemplate<String, String> redisTemplateString;
    @Override
    public void execute() {
        UserDetect userDetect = TrackingDataUtil.getUserDetect(user.getId(), redisTemplateString);
        TrophyRoadDataEnt trophyRoadDataEnt = TrophyRoadDataEnt.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .userDisplayName(user.getUsername())
                .trophyMilestone(trophyMilestone)
                .actionType(setActionType(actionType))
                .actionAtMs(System.currentTimeMillis())
                .seasonId(0)
                .seasonStartAtMs(ModuleConfig.TROPHY_ROAD_START_TIME_MILLISECOND)
                .seasonEndAtMs(ModuleConfig.TROPHY_ROAD_END_TIME_MILLISECOND)
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
                .rewardDataList(buildItemReceive(rewardEntList))
                .build();
        producer.sendTrophyRoadMessage(trophyRoadDataEnt);
    }

    private String setActionType(int type) {
        switch (type) {
            case 0:
            case 2:
                return "CLAIM";
            case 1:
                return "CLAIM_WITH_ADS";
            case 4:
                return "DONE";
            default:
                return "";
        }
    }

    private JsonArray buildItemReceive(List<RewardEnt> rewardEnts) {
        JsonArray jsonArray = new JsonArray();
        for (RewardEnt item : rewardEnts) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item_receive_id", item.getERewardType().getValue());
            jsonObject.addProperty("item_receive_amount", item.getDelta());
            jsonObject.addProperty("item_receive_name", item.getERewardType().name());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
