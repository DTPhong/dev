package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.data.GachaDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
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
public class GachaTrackingCmd implements QueueCommand {

    Producer producer;
    Profile user;
    int userTrophy;
    EventEnt gachaEvent;
    int quantity;
    boolean isUseEmerald;
    long emeraldCost;
    int moneyType;
    JsonArray gachaData;
    long startTime;
    long endTime;
    RedisTemplate<String, String> redisTemplateString;

    @Override
    public void execute() {
        JsonObject gachaTrackingData = buildTrackingData(gachaEvent, quantity, isUseEmerald, emeraldCost, moneyType, gachaData);
        UserDetect userDetect = TrackingDataUtil.getUserDetect(user.getId(), redisTemplateString);
        GachaDataEnt gachaDataEnt = GachaDataEnt.builder()
                .userId(user.getId())
                .userDisplayName(user.getUsername())
                .userAmethyst(user.getAmethyst())
                .userRoyalAmethyst(user.getRoyalAmethyst())
                .actionType(gachaTrackingData.get("actionType").getAsString())
                .actionCost(gachaTrackingData.get("actionCost").getAsInt())
                .actionCostCurrency(gachaTrackingData.get("actionCostCurrency").getAsString())
                .actionAtMs(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .bannerId(gachaTrackingData.get("bannerId").getAsInt())
                .bannerName(gachaTrackingData.get("bannerName").getAsString())
                .startSeasonTime(startTime)
                .endSeasonTime(endTime)
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
                .listItemReceive(gachaTrackingData.get("listGacha").getAsJsonArray())
                .build();
        producer.sendGachaMessage(gachaDataEnt);
    }

    private JsonObject buildTrackingData(EventEnt gachaEvent, int quantity, boolean isUseEmerald, long emeraldCost, int moneyType, JsonArray gachaData) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("actionType", quantity == 1 ? "SUMMON1" : "SUMMON10");
        if (isUseEmerald) {
            jsonObject.addProperty("actionCost", emeraldCost);
            jsonObject.addProperty("actionCostCurrency", "EMERALD");
        } else {
            jsonObject.addProperty("actionCost", quantity);
            if (moneyType == 11) {
                jsonObject.addProperty("actionCostCurrency", "AMETHYST");
            } else if (moneyType == 12) {
                jsonObject.addProperty("actionCostCurrency", "ROYAL_AMETHYST");
            }
        }
        jsonObject.addProperty("bannerId", gachaEvent.getId());
        jsonObject.addProperty("bannerName", gachaEvent.getTitle());

        JsonArray rewards = new JsonArray();
        for (JsonElement gachaItem : gachaData) {
            JsonObject item = new JsonObject();
            JsonObject gacha = gachaItem.getAsJsonObject();
            item.addProperty("item_receive_id", gacha.get("refID").getAsInt());
            item.addProperty("item_receive_amount", gacha.get("delta").getAsInt());
            item.addProperty("item_receive_name", gacha.get("receiveItemName").getAsString());
            rewards.add(item);
        }
        jsonObject.add("listGacha", rewards);
        return jsonObject;
    }
}
