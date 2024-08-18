/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.rabbitmq;

import org.springframework.stereotype.Component;

/**
 * @author Phong
 */
@Component
public class Listener {

//    @Autowired
//    SendMoneyService sendMoneyService;
//    @Autowired
//    CampaignService campaignService;
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "${admin_event_queue}", durable = "true"),
//            exchange = @Exchange(value = "kplay.topic", type = "topic"),
//            key = "kplay.event.*"))
//    public void eventListener(String in) {
////        GMLocalQueue.addEventQueue(new EventQueueCmd(in, sendMoneyService));
//    }
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "${admin_login_queue}", durable = "true"),
//            exchange = @Exchange(value = "kplay.topic", type = "topic"),
//            key = "kplay.login.*"))
//    public void loginListener(String in) {
//        GMLocalQueue.addEventQueue(new UserLoginQueueCommand(in, campaignService));
//    }
}
