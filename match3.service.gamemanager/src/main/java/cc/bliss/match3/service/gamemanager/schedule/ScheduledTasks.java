/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.schedule;

import cc.bliss.match3.service.gamemanager.service.common.AgonesService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.event.RushArenaService;
import cc.bliss.match3.service.gamemanager.service.system.GameLogService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.SSEService;
import cc.bliss.match3.service.gamemanager.service.system.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Phong
 */
@Component
@EnableScheduling
public class ScheduledTasks {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private SSEService sseService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private RushArenaService rushArenaService;

    @Autowired
    private GameLogService gameLogService;

    @Autowired
    private AgonesService agonesService;

    @Scheduled(zone = "Asia/Ho_Chi_Minh", cron = "0/5 * * ? * *")
    // Run per 5s
    public void updateLeaderboardClan() {
//        leaderboardService.updateLeaderboardClan();
    }

    @Scheduled(zone = "Asia/Ho_Chi_Minh", cron = "0 0/1 * * * ?")
    // Run per 1min
    public void checkSSEExpireTask() {
        sseService.checkSSEExpire();
    }

    @Scheduled(zone = "Asia/Ho_Chi_Minh", cron = "0 0 * ? * *")
    // Run per 1H
    public void checkDeleteAccount() {
        profileService.checkDeleteAccount();
        // clear cache profile to authen jwt
        UserDetailsServiceImpl.MAP_PROFILE_CACHE.clear();
        gameLogService.createTable();
    }

//    @Scheduled(zone = "Asia/Ho_Chi_Minh", cron = "0/10 * * * * ?")
//    // Run per 10s
//    public void matchingWorker() {
//        rushArenaService.matchingWorker();
//        rushArenaService.checkAndSendResult();
//    }

}
