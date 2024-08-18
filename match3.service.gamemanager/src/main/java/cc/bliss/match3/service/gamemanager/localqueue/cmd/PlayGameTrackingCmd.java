package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.data.PlayGameDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

@AllArgsConstructor
public class PlayGameTrackingCmd implements QueueCommand {
    Producer producer;
    Profile user;
    int userTrophy;
    JsonObject gameLog;
    RedisTemplate<String, String> redisTemplateString;
    @Override
    public void execute() {
        UserDetect userDetect = TrackingDataUtil.getUserDetect(user.getId(), redisTemplateString);
        PlayGameDataEnt playGameDataEnt = PlayGameDataEnt.builder()
                //userInfo
                .userId(user.getId())
                .userDisplayName(user.getUsername())
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
                //playgame data
                .componentUserDisplayName(gameLog.get("componentUserDisplayName").getAsString())
                .componentUserId(gameLog.get("componentUserId").getAsInt())
                .explodeBoomCount(gameLog.get("explodeBoomCount").getAsInt())
                .explodeDoubleSpecialCount(gameLog.get("explodeDoubleSpecialCount").getAsInt())
                .explodeHorizontalRocketCount(gameLog.get("explodeHorizontalRocketCount").getAsInt())
                .explodeSpecial5Count(gameLog.get("explodeSpecial5Count").getAsInt())
                .explodeVerticalRocketCount(gameLog.get("explodeVerticalRocketCount").getAsInt())
                .gemGreenCount(gameLog.get("gemGreenCount").getAsInt())
                .gemPinkCount(gameLog.get("gemPinkCount").getAsInt())
                .gemBlueCount(gameLog.get("gemBlueCount").getAsInt())
                .gemRedCount(gameLog.get("gemRedCount").getAsInt())
                .gemYellowCount(gameLog.get("gemYellowCount").getAsInt())
                .makeBoomCount(gameLog.get("makeBoomCount").getAsInt())
                .makeHorizontalRocketCount(gameLog.get("makeHorizontalRocketCount").getAsInt())
                .makeSpecial5Count(gameLog.get("makeSpecial5Count").getAsInt())
                .makeVerticalRocketCount(gameLog.get("makeVerticalRocketCount").getAsInt())
                .matchDurationMs(gameLog.get("matchDurationMs").getAsLong())
                .matchEndAtMs(gameLog.get("matchEndAtMs").getAsLong())
                .matchFindDurationMs(gameLog.get("matchFindDurationMs").getAsLong())
                .matchId(gameLog.get("matchId").getAsLong())
                .merge3Count(gameLog.get("merge3Count").getAsInt())
                .sessionId(gameLog.get("sessionId").getAsLong())
                .userComboCount(gameLog.get("userComboCount").getAsInt())
                .userComboDmg(gameLog.get("userComboDmg").getAsInt())
                .userDmg(gameLog.get("userDmg").getAsInt())
                .userHardLockCause(gameLog.get("userHardLockCause").getAsInt())
                .userHardLockUnlock(gameLog.get("userHardLockUnlock").getAsInt())
                .userHardLockedCount(gameLog.get("userHardLockedCount").getAsInt())
                .userHighestComboCount(gameLog.get("userHighestComboCount").getAsInt())
                .userHeroId(gameLog.get("userHeroId").getAsInt())
                .userHeroLevel(gameLog.get("userHeroLevel").getAsInt())
                .userHeroName(gameLog.get("userHeroName").getAsString())
                .userHeroSkillLevel(gameLog.get("userHeroSkillLevel").getAsInt())
                .userHeroTrophy(gameLog.get("userHeroTrophy").getAsInt())
                .userHpRegen(gameLog.get("userHpRegen").getAsInt())
                .userHpRemain(gameLog.get("userHpRemain").getAsInt())
                .userHpUltiRegen(gameLog.get("userHpUltiRegen").getAsInt())
                .userJoinType(gameLog.get("userJoinType").getAsString())
                .userManaRegen(gameLog.get("userManaRegen").getAsInt())
                .userMediumLockCause(gameLog.get("userMediumLockCause").getAsInt())
                .userMediumLockUnlock(gameLog.get("userMediumLockUnlock").getAsInt())
                .userMediumLockedCount(gameLog.get("userMediumLockedCount").getAsInt())
                .matchResult(gameLog.get("matchResult").getAsInt())
                .userSubJoinType(gameLog.get("userSubJoinType").getAsString())
                .userUltiCount(gameLog.get("userUltiCount").getAsInt())
                .userUltiDmg(gameLog.get("userUltiDmg").getAsInt())
                .userWinLoseTrophy(gameLog.get("userWinLoseTrophy").getAsInt())
                .roundId(gameLog.get("roundId").getAsInt())
                .roundResult(gameLog.get("roundResult").getAsInt())
                .roundDurationMs(gameLog.get("roundDurationMs").getAsInt())
                .matchRoundCount(gameLog.get("matchRoundCount").getAsInt())
                .componentBotType(gameLog.get("componentBotType").getAsString())
                .mergeLightXHrocket(gameLog.get("mergeLightXHrocket").getAsInt())
                .mergeLightXVrocket(gameLog.get("mergeLightXVrocket").getAsInt())
                .mergeLightXBomb(gameLog.get("mergeLightXBomb").getAsInt())
                .mergeRocketXRocket(gameLog.get("mergeRocketXRocket").getAsInt())
                .mergeBombXBomb(gameLog.get("mergeBombXBomb").getAsInt())
                .mergeLightXLight(gameLog.get("mergeLightXLight").getAsInt())
                .mergeBombXVrocket(gameLog.get("mergeBombXVrocket").getAsInt())
                .mergeBombXHrocket(gameLog.get("mergeBombXHrocket").getAsInt())
                .build();

        producer.sendPlayGameMessage(playGameDataEnt);
    }

}
