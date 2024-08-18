/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.controller;

import bliss.lib.framework.common.LogUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Phong
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/leaderboards")
public class LeaderBoardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @PostMapping(value = "", produces = "application/json")
    public ResponseEntity<String> getById(HttpServletRequest request) {
        try {
            String result = leaderboardService.getLeaderboard(request);
            return ResponseEntity.ok().body(result);

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/edit", produces = "application/json")
    public ResponseEntity<String> editTrophy(HttpServletRequest request) {
        try {
            String result = leaderboardService.editTrophy(request);
            return ResponseEntity.ok().body(result);

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @GetMapping(value = "/reset", produces = "application/json")
    public ResponseEntity<String> resetTrophy() {
        try {
            leaderboardService.resetTrophy();
            return ResponseEntity.ok().build();

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @GetMapping(value = "/info", produces = "application/json")
    public ResponseEntity<String> getLeaderboardInfo() {
        try {
            String result = leaderboardService.getLeaderboardInfo();
            return ResponseEntity.ok().body(result);

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @GetMapping(value = "/detail/{id}", produces = "application/json")
    public ResponseEntity<String> getLeaderboardInfo(@PathVariable(name = "id") String id) {
        try {
            String result = leaderboardService.getLeaderboardByID(id);
            return ResponseEntity.ok().body(result);

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, LeaderBoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }
}
