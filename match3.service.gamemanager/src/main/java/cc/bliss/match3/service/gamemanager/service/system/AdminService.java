/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.service.system;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.NetworkUtils;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.common.TicketEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.ent.enums.TicketStatus;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.RequestDeleteAccount;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.LoginCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.event.DailyRewardService;
import cc.bliss.match3.service.gamemanager.service.event.QuestEventService;
import cc.bliss.match3.service.gamemanager.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import org.apache.http.client.fluent.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Phong
 */
@Service
public class AdminService extends BaseService {
    
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    LeaderboardService leaderboardService;
    @Autowired
    HeroService heroService;
    @Autowired
    TicketService ticketService;
    @Autowired
    ProfileService profileService;
    @Autowired
    DailyRewardService dailyRewardService;
    @Autowired
    QuestEventService questEventService;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final List<String> DEFAULT_AVATARS = Arrays.asList(
            "https://dlc.match3arena.com/profile_ava_01.png",
            "https://dlc.match3arena.com/profile_ava_02.png",
            "https://dlc.match3arena.com/profile_ava_03.png",
            "https://dlc.match3arena.com/profile_ava_04.png",
            "https://dlc.match3arena.com/profile_ava_05.png"
            );

    private static final List<Integer> DEFAULT_FRAMES = Arrays.asList(0,1,2,3,4,5,6,7,8,9,10);

    public String genBot(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        long fromID = jsonObject.get("fromID").getAsLong();
        long toID = jsonObject.get("toID").getAsLong();
        int botType = jsonObject.get("botType").getAsInt();
        List<Profile> listBot = new ArrayList<>();
        for (long i = fromID; i < toID; i++) {
            if (profileRepository.read().findById(i).isPresent()){
                profileRepository.write().deleteById(i);
            }
            Profile profile = new Profile();
            String randomName = randomIdentifier();
            profile.setId(i);
            profile.setUsername("match3_" + randomName);
            profile.setDeviceID(String.valueOf(i));
            profile.addAvatar(DEFAULT_AVATARS);
            profile.addFrame(DEFAULT_FRAMES);
            profile.setIsNew(1);
            profile.setBotType(botType);
            listBot.add(profile);
        }
        insertMatch3SchemaListData(listBot);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), new JsonObject(), NetWorkAPI.LOGIN);
    }

    public String randomIdentifier() {
        Random random = new Random();
        int randomNumber;
        String playerName;
        do {
            randomNumber = random.nextInt(9999999) + 1;
            playerName = String.format("Player%07d", randomNumber);
        } while (profileRepository.read().existsByUsername(playerName));
        return playerName;
    }

    public SessionObj getSession() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof SessionObj) {
                return ((SessionObj) principal);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public SessionObj getSession(long userID) {
        return userDetailsService.loadUserById(userID);
    }

}
