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
import cc.bliss.match3.service.gamemanager.service.common.ClanService;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Phong
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/clans")
public class ClanController {

    @Autowired
    private ClanService clanService;

    @PostMapping(value = "/page", produces = "application/json")
    public ResponseEntity<String> getListClan(@RequestBody SearchObj searchObj) {
        try {
            String result = clanService.getListClan(searchObj);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/search", produces = "application/json")
    public ResponseEntity<String> searchListClan(@RequestBody SearchObj searchObj) {
        try {
            String result = clanService.searchClan(searchObj);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "", produces = "application/json")
    public ResponseEntity<String> searchListClan(HttpServletRequest request) {
        try {
            String result = clanService.createClan(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/join", produces = "application/json")
    public ResponseEntity<String> joinClan(HttpServletRequest request) {
        try {
            String result = clanService.joinClan(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @DeleteMapping(value = "/leave", produces = "application/json")
    public ResponseEntity<String> leaveClan() {
        try {
            String result = clanService.leaveClan();
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<String> detail(@PathVariable(name = "id") int id) {
        try {
            String result = clanService.getDetail(id);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @GetMapping(value = "/request/{id}", produces = "application/json")
    public ResponseEntity<String> getClanRequest(@PathVariable(name = "id") int id) {
        try {
            String result = clanService.getClanRequest(id);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/approve", produces = "application/json")
    public ResponseEntity<String> approveRequest(HttpServletRequest request) {
        try {
            String result = clanService.approveRequest(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/kich", produces = "application/json")
    public ResponseEntity<String> kich(HttpServletRequest request) {
        try {
            String result = clanService.kich(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(path = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getChatHistory(HttpServletRequest request) {
        try {
            String body = clanService.getChatHistory(request);
            return ResponseEntity.status(HttpStatus.OK).body(body);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.CLAN_CHAT_HISTORY));
        }
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(HttpServletRequest request) {
        try {
            String body = clanService.sendChat(request);
            return ResponseEntity.status(HttpStatus.OK).body(body);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ClanController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.CLAN_SEND_CHAT));
        }
    }
}
