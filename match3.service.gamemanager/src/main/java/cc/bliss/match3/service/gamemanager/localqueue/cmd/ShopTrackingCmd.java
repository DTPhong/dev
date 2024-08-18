package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.data.ShopDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import com.google.gson.JsonArray;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

@AllArgsConstructor
public class ShopTrackingCmd implements QueueCommand {
    Producer producer;
    Profile user;
    int userTrophy;
    long price;
    String itemName;
    int itemId;
    JsonArray rewardData;
    String actionType;
    String itemLocation;
    int slotLocation;
    String itemPriceCurrency;
    long goldBeforeAction;
    long emeraldBeforeAction;
    RedisTemplate<String, String> redisTemplateString;
    @Override
    public void execute() {
        UserDetect userDetect = TrackingDataUtil.getUserDetect(user.getId(), redisTemplateString);
        ShopDataEnt shopDataEnt = ShopDataEnt.builder()
                .transId(UUID.randomUUID().toString())
                .itemId(itemId)
                .itemName(itemName)
                .itemPrice(price)
                .itemPriceCurrency(itemPriceCurrency)
                .actionType(actionType)
                .itemLocation(itemLocation)
                .slotLocation(slotLocation)
                .actionAtMs(System.currentTimeMillis())
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
                .listItemReceive(rewardData)
                .build();
        producer.sendShopMessage(shopDataEnt);
    }
}
