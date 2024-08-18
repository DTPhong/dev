/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.controller;

import bliss.lib.framework.common.LogUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.SearchObj;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TutorialChangeCmd;
import cc.bliss.match3.service.gamemanager.service.event.Login7dQuestService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
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
@RequestMapping("/profiles")
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    @Autowired
    private Login7dQuestService login7dQuestService;

    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<String> getByID(@PathVariable(name = "id") long id) {
        try {
            String result = profileService.getByID(id);
            return ResponseEntity.ok().body(result);

        } catch (BadCredentialsException e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.PROFILE_BY_ID));
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_BY_ID));
        }
    }

    @PostMapping(value = "/page", produces = "application/json")
    public ResponseEntity<String> search(@RequestBody SearchObj searchObj) {
        try {
            String result = profileService.get(searchObj);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_SEARCH));
        }
    }

    @PostMapping(value = "/link", produces = "application/json")
    public ResponseEntity<String> search(HttpServletRequest request) {
        try {
            String result = profileService.linkAccount(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_SEARCH));
        }
    }

    @PostMapping(value = "/unlink", produces = "application/json")
    public ResponseEntity<String> unlinkAccount(HttpServletRequest request) {
        try {
            String result = profileService.unlinkAccount(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_SEARCH));
        }
    }

    @PostMapping(value = "/merge", produces = "application/json")
    public ResponseEntity<String> mergeAccount(HttpServletRequest request) {
        try {
            String result = profileService.mergeAccount(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_SEARCH));
        }
    }

    @PutMapping(value = "/edit", produces = "application/json")
    public ResponseEntity<String> editProfile(@RequestBody Profile profile) {
        try {
            String result = profileService.editProfile(profile);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_EDIT));
        }
    }

    @PutMapping(value = "/tutorial", produces = "application/json")
    public ResponseEntity<String> editTutorial(@RequestBody Profile profile) {
        try {
            String result = profileService.editTutorial(profile);
            GMLocalQueue.addQueue(new TutorialChangeCmd(profile.getId(), login7dQuestService));
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_EDIT));
        }
    }

    @PostMapping(value = "/delete", produces = "application/json")
    public ResponseEntity<String> requestDeleteAccount(HttpServletRequest request) {
        try {
            String result = profileService.requestDeleteAccount(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_SEARCH));
        }
    }

    @PostMapping(value = "/restore", produces = "application/json")
    public ResponseEntity<String> restoreAccount(HttpServletRequest request) {
        try {
            String result = profileService.restoreAccount(request);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, ProfileController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JsonBuilder.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.PROFILE_SEARCH));
        }
    }
}
