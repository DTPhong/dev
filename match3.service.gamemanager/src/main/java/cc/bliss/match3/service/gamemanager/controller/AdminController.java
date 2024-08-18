/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.controller;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.NetworkUtils;
import cc.bliss.match3.service.gamemanager.Bootstrap;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.common.AgonesService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Phong
 */
@CrossOrigin(origins = "*")
@RestController
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private ProfileService profileService;

    @Autowired
    private AgonesService agonesService;

    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<String> login(HttpServletRequest request) {
        try {
            GMLocalQueue.addQueue(new TelegramLoggerCmd("===========> IP Login " + NetworkUtils.getClientIP(request), TeleLogType.EXCEPTION, Bootstrap.class));
            String result = profileService.login(request);
            return ResponseEntity.ok().body(result);

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, AdminController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, AdminController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/gen-bot", produces = "application/json")
    public ResponseEntity<String> genBot(HttpServletRequest request) {
        try {
            String result = adminService.genBot(request);
            return ResponseEntity.ok().body(result);

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, AdminController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, AdminController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    //health check
    @GetMapping(value = "/")
    public ResponseEntity<String> healthCheck() {
        try {
            return ResponseEntity.ok().body("OK");
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, AdminController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    //health check
    @GetMapping(value = "/region")
    public ResponseEntity<String> getGkeRegionInfo() {
        try {
            String result = agonesService.getRegion();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, AdminController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

}
