package cc.bliss.match3.service.gamemanager.controller;

import bliss.lib.framework.common.LogUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.event.WinBattleService;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/win-battle")
public class WinBattleController {

    @Autowired
    private WinBattleService winBattleService;

    @GetMapping(value = "/upgrade-box")
    public ResponseEntity<String> upgradeMysteryBox() {
        try {
            String result = winBattleService.mysteryBoxUpgrade();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.DEBUG, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @GetMapping(value = "/claim-box")
    public ResponseEntity<String> claimMysteryBox() {
        try {
            String result = winBattleService.claimBox();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.DEBUG, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }

    @GetMapping(value = "/progress")
    public ResponseEntity<String> getWinProgress() {
        try {
            String result = winBattleService.winBattleProgress();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.DEBUG, ConfigController.class));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtils.toErrorBody(LogUtil.stackTrace(e), NetWorkAPI.EVENT));
        }
    }
}
