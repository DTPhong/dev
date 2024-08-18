/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.controller;

import bliss.lib.framework.common.LogUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.SearchObj;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.system.GameLogService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
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
@RequestMapping("/logs")
public class GameLogController {

    @Autowired
    private GameLogService gameLogService;

    @PostMapping(value = "", produces = "application/json")
    public ResponseEntity<String> create(HttpServletRequest request) {
        try {
            String result = gameLogService.create(request);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, GameLogController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.BATTLE_LOG_POST));
        }
    }

}
