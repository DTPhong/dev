package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.data.HeroCollectionDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

@AllArgsConstructor
public class HeroCollectionTrackingCmd implements QueueCommand {
    Producer producer;
    Profile user;
    int userTrophy;
    HeroEnt heroEnt;
    String actionType;
    int card;
    int gold;
    int heroLevel;
    int skillLevel;
    long goldBeforeAction;
    RedisTemplate<String, String> redisTemplateString;
    @Override
    public void execute() {
        UserDetect userDetect = TrackingDataUtil.getUserDetect(user.getId(), redisTemplateString);
        HeroCollectionDataEnt heroCollectionDataEnt = HeroCollectionDataEnt.builder()
                .actionAtMs(System.currentTimeMillis())
                .actionType(actionType)
                .heroCardCost(card)
                .heroDiamondCost(0)
                .heroEmeraldCost(0)
                .heroFaction(heroEnt.getHeroClass().name())
                .heroGoldCost(gold)
                .heroHp(heroEnt.getHp())
                .heroId(heroEnt.getId())
                .heroLevel(heroLevel)
                .heroName(heroEnt.getTitle())
                .heroPower(heroEnt.getPower())
                .heroRarity(heroEnt.getRarity().name())
                .heroSkillLevel(skillLevel)
                .heroTrophy(heroEnt.getTrophy())
                .userId(user.getId())
                .userDisplayName(user.getUsername())
                .userGold(goldBeforeAction)
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
                .build();
        producer.sendHeroCollectionMessage(heroCollectionDataEnt);
    }
}
