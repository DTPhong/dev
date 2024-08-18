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
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Phong
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/boards")
public class BoardController {

    final List<String> LOG = new ArrayList<>();

    @GetMapping(value = "/shuffle/log", produces = "application/json")
    public ResponseEntity<String> getLog(HttpServletRequest request) {
        try {
            JsonArray jsonArray = new JsonArray();
            for (String string : LOG) {
                jsonArray.add(string);
            }
            JsonObject resp = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());

            String result = ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_GET_LIST);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, BoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @PostMapping(value = "/shuffle/log", produces = "application/json")
    public ResponseEntity<String> shuffleLog(HttpServletRequest request) {
        try {
            JsonObject jsonObject = RequestUtils.requestToJson(request);
            String log = jsonObject.get("log").getAsString();
            LOG.add(log);

            JsonArray jsonArray = new JsonArray();
            for (String string : LOG) {
                jsonArray.add(string);
            }
            JsonObject resp = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());

            String result = ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_GET_LIST);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, BoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

    @DeleteMapping(value = "/shuffle/clear", produces = "application/json")
    public ResponseEntity<String> shuffleClear() {
        try {
            String result = "";
            LOG.clear();
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, BoardController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.LOGIN));
        }
    }

}
