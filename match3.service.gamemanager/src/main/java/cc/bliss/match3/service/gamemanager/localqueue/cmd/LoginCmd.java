/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.data.LoginDataEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.event.DailyRewardService;
import cc.bliss.match3.service.gamemanager.service.event.Login7dQuestService;
import cc.bliss.match3.service.gamemanager.service.event.QuestEventService;
import cc.bliss.match3.service.gamemanager.util.TrackingDataUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author Phong
 */
@AllArgsConstructor
public class LoginCmd implements QueueCommand {

    DailyRewardService dailyRewardService;
    QuestEventService questEventService;
    Login7dQuestService login7dQuestService;
    Profile sessionObj;
    Producer producer;
    String loginMethod;
    int trophy;
    RedisTemplate<String, String> redisTemplateString;

    @Override
    public void execute() {
        questEventService.listenLogin(sessionObj);
        dailyRewardService.listenLogin(sessionObj);
        login7dQuestService.listenLogin(sessionObj);

        UserDetect userDetect = TrackingDataUtil.getUserDetect(sessionObj.getId(), redisTemplateString);
        LoginDataEnt loginDataEnt = LoginDataEnt
                .builder()
                .actionAtMs(System.currentTimeMillis())
                .userApp("-")
                .userCountryCode(userDetect.getCountryCode())
                .userCreatedAtMs(sessionObj.getDateCreated() != null ? sessionObj.getDateCreated().getTime() : System.currentTimeMillis())
                .userCurrencyCode("VND")
                .userDevice(sessionObj.getDeviceID())
                .userDisplayName(sessionObj.getUsername())
                .userEmail(sessionObj.getGmail())
                .userEmerald(sessionObj.getEmerald())
                .userGold(sessionObj.getMoney())
                .userId(sessionObj.getId())
                .userIp(userDetect.getIp())
                .userIsNew(sessionObj.getIsNew())
                .userLinkAccId("-")
                .userLoginMethod(loginMethod)
                .userWinLoseStreak(sessionObj.getWinStreak())
                .userPlatform("-")
                .userTrophy(trophy)
                .userUtmCampaign("-")
                .userUtmSource("-")
                .userVersion("-")
                .botHardMode(EBotType.fromValue(sessionObj.getBotType()).name())
                .build();
        producer.sendLoginMessage(loginDataEnt);
    }

}
