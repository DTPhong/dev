/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.system;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.common.TicketEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.ent.enums.TicketStatus;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Version;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.EPushTicketTrackingData;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.PushTicketTrackingDataCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.AgonesService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.common.ProfileStatisticService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class TicketService extends BaseService {

    public static boolean IS_MAINTAIN = false;
    public static final Map<Long, TicketEnt> mapTicket = new NonBlockingHashMap<>();
    @Autowired
    private AdminService adminService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private SSEService sseService;
    @Autowired
    private AgonesService agonesService;
    @Autowired
    private HeroService heroService;
    @Autowired
    private Producer producerService;
    @Autowired
    private ProfileStatisticService profileStatisticService;
    @Autowired
    private BotService botService;
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    public TicketEnt getTicket(long userID) {
        return mapTicket.get(userID);
    }

    public String getAllTicket() {
        JsonArray jsonArray = new JsonArray();
        for (TicketEnt ticketEnt : mapTicket.values()) {
            JsonArray userInfo = new JsonArray();
            if (ticketEnt.getBotID() != 0){
                // for battle with bot
                {
                    // add profile user
                    Profile profile = profileService.getProfileByID(ticketEnt.getUserID());
                    JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                    data.addProperty("isBestFriend", ticketEnt.getBotID());
                    userInfo.add(data);
                }
                {
                    // add bot profile
                    Profile profile = profileService.getProfileByID(ticketEnt.getBotID());
                    JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                    data.addProperty("isBestFriend", ticketEnt.getBotID());
                    userInfo.add(data);
                }
            } else {
                // for battle with user
                for (TicketEnt object : mapTicket.values().stream().filter(e -> e.getRoomID() == ticketEnt.getRoomID()).collect(Collectors.toList())) {
                    Profile profile = profileService.getProfileByID(object.getUserID());
                    JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                    data.addProperty("isBestFriend", ticketEnt.getBotID());
                    userInfo.add(data);
                }
            }
            JsonObject ticketJson = JsonBuilder.ticketToJson(ticketEnt);
            ticketJson.add("userInfo", userInfo);
            jsonArray.add(ticketJson);
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonArray, NetWorkAPI.GET_LIST_TICKET);
    }

    public String createTicket(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s UserID: %s %s create ticket", DateTimeUtils.getNow("hh:MM:ss"),session.getUsername(), session.getId());
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        if (IS_MAINTAIN) {
            TicketEnt ticketEnt = new TicketEnt();
            ticketEnt.setUserID(session.getId());
            ticketEnt.setStatus(TicketStatus.MAINTAIN);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), JsonBuilder.ticketToJson(ticketEnt), NetWorkAPI.CREATE_TICKET);
        }
        if (mapTicket.containsKey(session.getId())) {
            sseService.emitNextMsg(pullTicketByUserID(session.getId()), session.getId());
            return pullTicketByUserID(session.getId());
        }
        Profile profile = profileService.getMinProfileByID(session.getId());
        TicketEnt ticketEnt = new TicketEnt();
        if (StringUtils.isNotBlank(profile.getVersion())){
            Version version = versionRepository.read().findById(profile.getVersion()).get();
            ticketEnt.setGameServerNameSpace(version.getGameServerNameSpace());
        }
        ticketEnt.setUserID(session.getId());
        ticketEnt.setUsername(session.getUsername());
        int heroTrophy = leaderboardService.getHeroTrophy(session.getId(), profile.getSelectHero());
        int totalTrophy = leaderboardService.getProfileTrophy(session.getId());
        HeroEnt heroEnt = heroService.getHero(session.getId(), profile.getSelectHero());
        ticketEnt.setTotalTrophy(totalTrophy);
        ticketEnt.setHeroTrophy(heroTrophy);
        ticketEnt.setWinStreak(profile.getWinStreak());
        ticketEnt.setLoseStreak(profile.getLoseStreak());
        ticketEnt.setBattleWon(profile.getBattleWon());
        ticketEnt.setLevel(heroEnt.getLevel());
        ticketEnt.setTutorial(profile.getTutorial());
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        if (jsonObject != null && jsonObject.has("roomID")) {
            int roomID = jsonObject.get("roomID").getAsInt();
            ticketEnt.setRoomID(roomID);
        }
        agonesService.getGameServer(ticketEnt);
        mapTicket.put(session.getId(), ticketEnt);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), JsonBuilder.ticketToJson(ticketEnt), NetWorkAPI.CREATE_TICKET);
    }

    public String pullTicketBySession() {
        SessionObj session = adminService.getSession();
        return pullTicketByUserID(session.getId());
    }

    /**
     * trả thông tin matching tickets gồm thông tin user và đối thủ (bot/user)
     * @param userID
     * @return
     */
    public String pullTicketByUserID(long userID) {
        if (mapTicket.containsKey(userID)) {
            TicketEnt ticketEnt = mapTicket.get(userID);
            JsonArray userInfo = new JsonArray();
            if (ticketEnt.getBotID() != 0){
                // for battle with bot
                {
                    // add profile user
                    Profile profile = profileService.getProfileByID(userID);
                    int selectHeroTrophy = leaderboardService.getHeroTrophy(userID, profile.getSelectHero());
                    profile.setSelectHeroTrophy(selectHeroTrophy);
                    JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                    data.addProperty("isBestFriend", profile.getBotType());
                    userInfo.add(data);
                }
                {
                    // add bot profile
                    Profile profile = profileService.getProfileByID(ticketEnt.getBotID());
                    int selectHeroTrophy = leaderboardService.getHeroTrophy(userID, profile.getSelectHero());
                    profile.setSelectHeroTrophy(selectHeroTrophy);
                    JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                    data.addProperty("isBestFriend", profile.getBotType());
                    userInfo.add(data);
                }
            } else {
                // for battle with user
                for (TicketEnt object : mapTicket.values().stream().filter(e -> e.getRoomID() == ticketEnt.getRoomID()).collect(Collectors.toList())) {
                    Profile profile = profileService.getProfileByID(object.getUserID());
                    int selectHeroTrophy = leaderboardService.getHeroTrophy(userID, profile.getSelectHero());
                    profile.setSelectHeroTrophy(selectHeroTrophy);
                    JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                    data.addProperty("isBestFriend", profile.getBotType());
                    userInfo.add(data);
                }
            }
            JsonObject ticketJson = JsonBuilder.ticketToJson(ticketEnt);
            ticketJson.add("userInfo", userInfo);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), ticketJson, NetWorkAPI.PULL_TICKET);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), "Invalid Ticket", NetWorkAPI.PULL_TICKET);
    }

    /**
     * Trả thông tin matching tickets khi user match với bot
     * @param userID
     * @param botProfile
     * @return
     */
    public String pullTicketByUserID(long userID, Profile botProfile) {
        if (mapTicket.containsKey(userID)) {
            TicketEnt ticketEnt = mapTicket.get(userID);
            JsonArray userInfo = new JsonArray();
            if (botProfile != null){
                // for battle with bot
                {
                    // add profile user
                    Profile profile = profileService.getProfileByID(userID);
                    int selectHeroTrophy = leaderboardService.getHeroTrophy(userID, profile.getSelectHero());
                    profile.setSelectHeroTrophy(selectHeroTrophy);
                    JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                    data.addProperty("isBestFriend", profile.getBotType());
                    userInfo.add(data);
                }
                {
                    // add bot profile
                    JsonObject data = JsonBuilder.profileToJson(botProfile, botProfile.getTrophy());
                    data.addProperty("isBestFriend", botProfile.getBotType());
                    userInfo.add(data);
                }
            }
            JsonObject ticketJson = JsonBuilder.ticketToJson(ticketEnt);
            ticketJson.add("userInfo", userInfo);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), ticketJson, NetWorkAPI.PULL_TICKET);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), "Invalid Ticket", NetWorkAPI.PULL_TICKET);
    }

    public void delTicket(long userID) {
        mapTicket.remove(userID);
    }

    public String delTicket() {
        SessionObj session = adminService.getSession();
        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s UserID: %s %s delete ticket", DateTimeUtils.getNow("hh:MM:ss"),session.getUsername(), session.getId());
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        if (mapTicket.containsKey(session.getId())) {
            TicketEnt ticketEnt = mapTicket.get(session.getId());
            boolean isMatchedWithUser = ticketEnt.getStatus().equals(TicketStatus.MATCHED) && ticketEnt.getBotID()!=0;
            boolean isOnRoomWithUser = ticketEnt.getStatus().equals(TicketStatus.MATCHED) && ticketEnt.getBotID()!=0;
            if (isOnRoomWithUser || isMatchedWithUser) {
                sseService.emitNextMsg(pullTicketByUserID(session.getId()), session.getId());
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), "Cannot delete onroom ticket", NetWorkAPI.DEL_TICKET);
            } else {
                delTicket(session.getId());

                PushTicketTrackingDataCmd cmd = new PushTicketTrackingDataCmd(profileService, heroService,profileStatisticService,ticketEnt, EPushTicketTrackingData.CANCEL, producerService);
                GMLocalQueue.addQueue(cmd);
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.DEL_TICKET);
    }

    public String delTicketRoom(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int roomID = jsonObject.get("roomID").getAsInt();
        for (TicketEnt value : mapTicket.values()) {
            if (value.getRoomID() == roomID) {
                delTicket(value.getUserID());
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.DEL_TICKET_BY_ROOM);
    }

    public String increaseRoom(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int roomID = jsonObject.get("roomID").getAsInt();
        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s Start increase room with id %d", DateTimeUtils.getNow("hh:MM:ss"), roomID);
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        String gameServerId = "";
        String gameServerName = "";
        for (TicketEnt value : mapTicket.values()) {
            if (value.getRoomID() == roomID) {
                if (!value.getGameServerId().isEmpty()) {
                    gameServerId = value.getGameServerId();
                    gameServerName = value.getGameServerNameSpace();
                    break;
                }
            }
        }
        int count = increaseGameServerRoom(gameServerId);
        agonesService.setDataUpdateRoomCount(count, gameServerId);
        //allocation game server
        agonesService.allocationGameServer("agones.dev/fleet", gameServerName);
        JsonObject response = new JsonObject();
        response.addProperty("gameServerID", gameServerId);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.DEL_TICKET_BY_ROOM);
    }

    public String decreaseRoom(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        if (!jsonObject.has("gameServerID")) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "gameServerID is require field", NetWorkAPI.DEL_TICKET_BY_ROOM);
        }
        String gameServerID = jsonObject.get("gameServerID").getAsString();
        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s Start decrease room for gameServerId %s", DateTimeUtils.getNow("hh:MM:ss"), gameServerID);
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        int count = decreaseGameServerRoom(gameServerID);
        agonesService.setDataUpdateRoomCount(count, gameServerID);
        //check if current count = 0 && at least 2 game servers allocated - > move status "Allocated" -> "Ready"
        if (count == 0) {
            agonesService.updateReadyStatus(gameServerID);
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.DEL_TICKET_BY_ROOM);
    }

    public String startGameRoom(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int roomID = jsonObject.get("roomID").getAsInt();
        String user = "";
        for (TicketEnt value : mapTicket.values()) {
            if (value.getStatus() != TicketStatus.ON_ROOM && value.getRoomID() == roomID) {
                value.setStatus(TicketStatus.ON_ROOM);

                sseService.emitNextMsg(pullTicketByUserID(value.getUserID()), value.getUserID());
                user += value.getUsername() + "_";
            }
            user += value.getUsername() + "_";
        }

        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s Start game room %s %s", DateTimeUtils.getNow("hh:MM:ss"), roomID, user);
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s Start game room %s %s", DateTimeUtils.getNow("hh:MM:ss"), roomID, user);
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s Start game room %s %s", DateTimeUtils.getNow("hh:MM:ss"), roomID, user);
        }
        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s Start game room %s %s", DateTimeUtils.getNow("hh:MM:ss"), roomID, user);
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.DEL_TICKET_BY_ROOM);
    }

    public String delTicket(HttpServletRequest request) {
        mapTicket.clear();
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.DEL_TICKET_BY_ROOM);
    }

    private String getGameServerCountRoomKey(String id) {
        return String.format(GameConstant.GAME_SERVER_COUNT_ROOM_KEY, id);
    }

    private int increaseGameServerRoom(String id) {
        String key = getGameServerCountRoomKey(id);
        redisTemplateString.opsForValue().increment(key);
        String value = redisTemplateString.opsForValue().get(key);
        redisTemplateString.expire(key, Duration.ofDays(2));
        return value == null ? 0 : Integer.parseInt(value);
    }

    private int decreaseGameServerRoom(String id) {
        redisTemplateString.opsForValue().decrement(getGameServerCountRoomKey(id));
        String value = redisTemplateString.opsForValue().get(getGameServerCountRoomKey(id));
        return value == null ? 0 : Integer.parseInt(value);
    }

    /**
     * Match both ticket user x user
     * @param cur
     * @param opponent
     */
    public void matchTicket(TicketEnt cur, TicketEnt opponent) {
        int blankRoom = roomService.getBlankRoom();
        long now = System.currentTimeMillis();
        cur.setRoomID(blankRoom);
        cur.setStatus(TicketStatus.MATCHED);
        cur.setMatchTime(now);
        opponent.setRoomID(blankRoom);
        opponent.setStatus(TicketStatus.MATCHED);
        opponent.setMatchTime(now);
        sseService.emitNextMsg(pullTicketByUserID(cur.getUserID()), cur.getUserID());
        sseService.emitNextMsg(pullTicketByUserID(opponent.getUserID()), opponent.getUserID());

        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s UserID: %s %s match ticket user %s %s", DateTimeUtils.getNow("hh:MM:ss"), cur.getUsername(), cur.getUserID(), opponent.getUsername(), opponent.getUserID());
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        PushTicketTrackingDataCmd cmd = new PushTicketTrackingDataCmd( producerService,
                EPushTicketTrackingData.MATCHED,
                opponent,
                botService.getBotType(cur.getBotID()),
                blankRoom,
                cur,
                profileStatisticService,
                heroService,
                profileService);
        GMLocalQueue.addQueue(cmd);
    }

    /**
     * Match ticket user x bot
     * @param ticket
     */
    public void matchTicket(TicketEnt ticket, Profile botProfile) {
        int blankRoom = roomService.getBlankRoom();
        long now = System.currentTimeMillis();
        ticket.setRoomID(blankRoom);
        ticket.setStatus(TicketStatus.MATCHED);
        ticket.setMatchTime(now);
        if (ModuleConfig.IS_DEBUG){
            String debugMsg = ResponseUtils.toResponseBody(99, "Begin send matched sse", NetWorkAPI.UNKNOWN);
            sseService.emitNextMsg(debugMsg, ticket.getUserID());
        }
        sseService.emitNextMsg(pullTicketByUserID(ticket.getUserID(), botProfile), ticket.getUserID());
        if (ModuleConfig.IS_DEBUG){
            String debugMsg = ResponseUtils.toResponseBody(99, "End send matched sse", NetWorkAPI.UNKNOWN);
            sseService.emitNextMsg(debugMsg, ticket.getUserID());
        }

        if (ModuleConfig.IS_DEBUG) {
            String log = String.format("Time: %s UserID: %s %s match ticket bot %s", DateTimeUtils.getNow("hh:MM:ss"), ticket.getUsername(), ticket.getUserID(), ticket.getBotID());
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
        }
        PushTicketTrackingDataCmd cmd = new PushTicketTrackingDataCmd( producerService,
                EPushTicketTrackingData.MATCHED,
                null,
                botService.getBotType(ticket.getBotID()),
                blankRoom,
                ticket,
                profileStatisticService,
                heroService,
                profileService);
        GMLocalQueue.addQueue(cmd);
    }

    public void leaveRoom(List<Long> userIDs) {
        for (Long userID : userIDs) {
            delTicket(userID);
        }
    }

    public void friendBattle(long userID, long friendID) {
        TicketEnt cur = new TicketEnt();
        cur.setUserID(userID);
        cur.setHeroTrophy(leaderboardService.getProfileTrophy(userID));
        TicketEnt opponent = new TicketEnt();
        opponent.setUserID(friendID);
        opponent.setHeroTrophy(leaderboardService.getProfileTrophy(friendID));

        matchTicket(cur, opponent);
        mapTicket.put(userID, cur);
        mapTicket.put(friendID, opponent);
    }

    public String maintain() {
        IS_MAINTAIN = true;
        redisTemplateString.convertAndSend("serverMaintainTopic","true");
        sseService.emitNextMsg(ResponseUtils.toResponseBody(HttpStatus.SERVICE_UNAVAILABLE.value(), "Server maintain !", NetWorkAPI.LOGIN), SSEService.GLOBAL_CHANNEL);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.DEL_TICKET);
    }

    /**
     * Cache lại botHeroLevel của match ticket, khi user match với bot (TTL 10s kể từ khi matched)
     * @param ticketId - userId
     * @param heroLv - heroLv của Bot
     */
    public void cacheMatchingTicket(long ticketId, int heroLv){
        String hashKey = String.format("bot_matching_ticket_%d", ticketId);
        String hashField = "bot_hero_level";
        redisTemplateString.opsForHash().put(hashKey, hashField, ConvertUtils.toString(heroLv));
        redisTemplateString.expire(hashKey, Duration.ofDays(1));
    }

    /**
     * Trả về heroLv của bot khi user matching với Bot
     * @param ticketId
     * @return
     */
    public int getBotHeroLv(long ticketId){
        String hashKey = String.format("bot_matching_ticket_%d", ticketId);
        String hashField = "bot_hero_level";
        return ConvertUtils.toInt(redisTemplateString.<String, String>opsForHash().get(hashKey, hashField));
    }
}
