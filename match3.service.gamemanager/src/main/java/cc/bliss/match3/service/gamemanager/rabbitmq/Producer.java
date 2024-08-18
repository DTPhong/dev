/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.rabbitmq;

import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.ent.data.IDataEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Phong
 */
@Component
public class Producer {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Value("${spring.rabbitmq.exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.login_routing}")
    private String loginRouting;

    @Value("${spring.rabbitmq.trophyroad_routing}")
    private String trophyRoadRouting;

    @Value("${spring.rabbitmq.gacha_routing}")
    private String gachaRouting;

    @Value("${spring.rabbitmq.quest_routing}")
    private String questRouting;

    @Value("${spring.rabbitmq.hero_collection_routing}")
    private String heroRouting;

    @Value("${spring.rabbitmq.daily_reward_routing}")
    private String dailyRewardRouting;

    @Value("${spring.rabbitmq.shop_routing}")
    private String shopRouting;

    @Value("${spring.rabbitmq.playgame_routing}")
    private String playGameRouting;

    @Value("${spring.rabbitmq.matching_ticket}")
    private String matchingTicketRouting;

    @Value("${spring.rabbitmq.property_change}")
    private String propertyChangeRouting;

    public void sendMatchingTicketMessage(IDataEnt message) {
        sendMessage(exchange, matchingTicketRouting, message.toJsonString());
    }

    public void sendLoginMessage(IDataEnt message) {
        sendMessage(exchange, loginRouting, message.toJsonString());
    }

    private void sendMessage(String exchange, String routingKey, String message) {
        amqpTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void sendTrophyRoadMessage(IDataEnt message) {
        sendMessage(exchange, trophyRoadRouting, message.toJsonString());
    }

    public void sendGachaMessage(IDataEnt message) {
        sendMessage(exchange, gachaRouting, message.toJsonString());
    }

    public void sendQuestMessage(IDataEnt message) {
        sendMessage(exchange, questRouting, message.toJsonString());
    }

    public void sendHeroCollectionMessage(IDataEnt message) {
        sendMessage(exchange, heroRouting, message.toJsonString());
    }

    public void sendDailyRewardMessage(IDataEnt message) {
        sendMessage(exchange, dailyRewardRouting, message.toJsonString());
    }

    public void sendShopMessage(IDataEnt message) {
        sendMessage(exchange, shopRouting, message.toJsonString());
    }

    public void sendPlayGameMessage(IDataEnt message) {
        sendMessage(exchange, playGameRouting, message.toJsonString());
    }
    public void sendPropertyChangeMessage(IDataEnt message) {
        sendMessage(exchange, propertyChangeRouting, message.toJsonString());
    }
}
