/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.controller;

import bliss.lib.framework.common.LogUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.event.EventService;
import cc.bliss.match3.service.gamemanager.service.event.Login7dQuestService;
import cc.bliss.match3.service.gamemanager.service.event.RushArenaService;
import cc.bliss.match3.service.gamemanager.service.event.WinBattleService;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Phong
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private RushArenaService rushArenaService;

    @Autowired
    private WinBattleService winBattleService;

    @Autowired
    private Login7dQuestService login7dQuestService;

    @GetMapping(value = "/quest", produces = "application/json")
    public ResponseEntity<String> getCurrentEvent() {
        try {
            String result = eventService.getCurrentEvent();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.GET_CONFIG));
        }
    }

    @PostMapping(value = "/quest", produces = "application/json")
    public ResponseEntity<String> claimEvent(HttpServletRequest request) {
        try {
            String result = eventService.claimEvent(request);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.GET_CONFIG));
        }
    }

    @PostMapping(value = "/recordAds", produces = "application/json")
    public ResponseEntity<String> watchAds(HttpServletRequest request) {
        try {
            String result = eventService.watchAds(request);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.GET_CONFIG));
        }
    }

    @PostMapping(value = "/recordSurvey", produces = "application/json")
    public ResponseEntity<String> recordSurvey() {
        try {
            String result = eventService.recordSurvey();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.GET_CONFIG));
        }
    }

    @GetMapping(value = "/daily-reward", produces = "application/json")
    public ResponseEntity<String> getDailyReward() {
        try {
            String result = eventService.getDailyReward();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.GET_CONFIG));
        }
    }

    @PostMapping(value = "/daily-reward", produces = "application/json")
    public ResponseEntity<String> claimDailyReward(HttpServletRequest request) {
        try {
            String result = eventService.claimDailyReward(request);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.GET_CONFIG));
        }
    }

    @GetMapping(value = "/rush", produces = "application/json")
    public ResponseEntity<String> getRushEvent() {
        try {
            String result = rushArenaService.getEvents();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @PostMapping(value = "/rush", produces = "application/json")
    public ResponseEntity<String> joinRushEvent(HttpServletRequest request) {
        try {
            String result = rushArenaService.join(request);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @DeleteMapping(value = "/rush", produces = "application/json")
    public ResponseEntity<String> deleteTicket(HttpServletRequest request) {
        try {
            String result = rushArenaService.deleteTicket();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @GetMapping(value = "/rush/room", produces = "application/json")
    public ResponseEntity<String> getRoom(@RequestParam("type") int eventType) {
        try {
            String result = rushArenaService.getRoom(eventType);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @PostMapping(value = "/rush/claim", produces = "application/json")
    public ResponseEntity<String> claimRushReward(HttpServletRequest request) {
        try {
            String result = rushArenaService.claim(request);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.DEBUG, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @GetMapping(value = "/login-7d-quest", produces = "application/json")
    public ResponseEntity<String> getLogin7dQuest(){
        try{
            String result = login7dQuestService.getCurrentEvent();
            return ResponseEntity.ok().body(result);
        } catch (Exception e){
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.DEBUG, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @PostMapping(value = "/claim-login-7d-quest", produces = "application/json")
    public ResponseEntity<String> claimLogin7dQuest(HttpServletRequest request){
        try{
            String result = login7dQuestService.claim(request);
            return ResponseEntity.ok().body(result);
        } catch (Exception e){
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.DEBUG, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }
}
