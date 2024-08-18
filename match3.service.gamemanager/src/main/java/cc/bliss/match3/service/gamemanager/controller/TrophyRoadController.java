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
import cc.bliss.match3.service.gamemanager.service.event.TrophyRoadService;
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
@RequestMapping("/trophyroad")
public class TrophyRoadController {

    @Autowired
    TrophyRoadService trophyRoadService;

    @GetMapping(value = "", produces = "application/json")
    public ResponseEntity<String> getTrophyRoad() {
        try {
            String result = trophyRoadService.getTrophyRoad();
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, TicketController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/claim", produces = "application/json")
    public ResponseEntity<String> claimTrophyRoad(HttpServletRequest httpServletRequest) {
        try {
            String result = trophyRoadService.claimTrophyRoad(httpServletRequest);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, TicketController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/claim-end-trophy", produces = "application/json")
    public ResponseEntity<String> claimEndTrohpyRoad(HttpServletRequest httpServletRequest) {
        try {
            String result = trophyRoadService.claimEndTrohpyRoad(httpServletRequest);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, TicketController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @GetMapping(value = "/removeAds", produces = "application/json")
    public ResponseEntity<String> removeAds() {
        try {
            String result = trophyRoadService.removeAds();
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, TicketController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }
}
