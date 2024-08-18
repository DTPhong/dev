package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.*;
import cc.bliss.match3.service.gamemanager.ent.enums.EEventType;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.ent.enums.EUpdateMoneyType;
import cc.bliss.match3.service.gamemanager.ent.enums.TicketStatus;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.SSEService;
import cc.bliss.match3.service.gamemanager.service.system.TicketService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RushArenaService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(RushArenaService.class);
    @Autowired
    private ProfileService profileService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private SSEService sseService;

    private static final String USER_EVENT_INFO_KEY = "event_%s_%s";
    private static final String LIST_EVENT_USER_INFO = "events_%s";
    private static final String ROOM_EVENT_ID_KEY = "room_event_id";
    private static final String ACTIVE_ROOMS_KEY = "rush_active_rooms_%s";
    private static final String WIN_STREAK_KEY = "rush_win_streak_%s";
    private static final String HIGHEST_WIN_STREAK_KEY = "rush_highest_win_streak_%s";
    public final Map<Long, TicketEvent> mapEvents = new NonBlockingHashMap<>();
    private static final int MAX_ROOM_SIZE = 5;
    private static final Gson gson = ModuleConfig.GSON_BUILDER;

    private void generateFakeData() {
        List<Long> userIds = new ArrayList<>();
        for (int i = 1000279; i <= 1000282; i++) {
            userIds.add((long) i);
        }

        for (Long userId : userIds) {
            TicketEvent ticketEvent = new TicketEvent();
            ticketEvent.setUserId(userId);

            if (userId == 105 || userId == 106 || userId == 107) {
                ticketEvent.setDeltaTrophy(300);
            } else {
                ticketEvent.setCurrentTrophy(200);
            }
            ticketEvent.setUserName("vu "+userId);
            ticketEvent.setEventStartTime(1718873940000L);
            ticketEvent.setEventEndTime(1719409200000L);
            ticketEvent.setEventType(13);
            ticketEvent.setBotType(0);
            ticketEvent.setStatus(TicketStatus.WAITING);
            mapEvents.put(userId, ticketEvent);
        }
    }

//    @PostConstruct
//    public void init() {
//        generateFakeData();
//    }

    public String join(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int eventType = jsonObject.get("type").getAsInt();
        long userId = session.getId();
        //chỉ add user chưa có room  vào pool (mapEvents), pool này sẽ chứa tất cả user có nhu cầu tham gia event
        String userEventKey = getUserEventInfoKey(eventType, userId);
        boolean isJoined = Boolean.TRUE.equals(redisTemplateString.hasKey(userEventKey));
        if (isJoined) {
            return ResponseUtils.toErrorBody("User already join this event", NetWorkAPI.EVENT);
        }
        TicketEvent ticketEvent = setTicketEvent(jsonObject, userId);
        Profile profile = profileService.getMinProfileByID(userId);
        int profileTrophy = leaderboardService.getProfileTrophy(userId);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), JsonBuilder.eventTicketToJson(ticketEvent, profile, profileTrophy), NetWorkAPI.CREATE_TICKET);
    }


    public String deleteTicket() {
        SessionObj session = adminService.getSession();
        if (mapEvents.containsKey(session.getId())) {
            TicketEvent ticketEvent = mapEvents.get(session.getId());
            if (ticketEvent.getStatus().equals(TicketStatus.ON_ROOM) || ticketEvent.getStatus().equals(TicketStatus.MATCHED)) {
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), "Cannot delete ticket", NetWorkAPI.DEL_TICKET);
            } else {
                cancelTicket(session.getId());
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.DEL_TICKET);
    }

    private void cancelTicket(long userID) {
        TicketEvent ticketEvent =  mapEvents.get(userID);
        ticketEvent.setStatus(TicketStatus.CANCELLED);
    }

    private String sendEventTicketSSE(TicketEvent ticketEvent) {
        long userId = ticketEvent.getUserId();
        Profile profile = profileService.getMinProfileByID(userId);
        int profileTrophy = leaderboardService.getProfileTrophy(userId);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), JsonBuilder.eventTicketToJson(ticketEvent, profile, profileTrophy), NetWorkAPI.RUSH_MATCH_UPDATE);
    }

    private TicketEvent setTicketEvent(JsonObject jsonObject, long userId) {
        int eventType = jsonObject.get("type").getAsInt();
        int eventId = jsonObject.get("id").getAsInt();
        long eventStartTime = jsonObject.get("startTime").getAsLong();
        long eventEndTime = jsonObject.get("endTime").getAsLong();
        Profile profile = profileService.getMinProfileByID(userId);
        int trophy = leaderboardService.getProfileTrophy(userId);
        EventEnt eventEnt = eventRepository.read().getById(eventId);
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        JsonArray listReward = customData.getAsJsonArray("listReward");
        List<EventReward> eventRewards = new ArrayList<>();
        for (JsonElement e : listReward) {
            JsonObject item = e.getAsJsonObject();
            EventReward eventReward = new EventReward();
            eventReward.setRewardType(item.get("type").getAsInt());
            eventReward.setRank(item.get("rank").getAsInt());
            eventReward.setAmount(item.get("amount").getAsInt());
            eventRewards.add(eventReward);
        }
        TicketEvent ticketEvent = new TicketEvent();
        ticketEvent.setEventType(eventType);
        ticketEvent.setEventId(eventId);
        ticketEvent.setEventStartTime(eventStartTime);
        ticketEvent.setEventEndTime(eventEndTime);
        ticketEvent.setUserId(userId);
        ticketEvent.setCurrentTrophy(trophy);
        ticketEvent.setUserName(profile.getUsername());
        ticketEvent.setDeltaTrophy(trophy > GameConstant.JUNIOR_TROPHY ? GameConstant.INIT_TICKET_DELTA_TROPHY : GameConstant.JUNIOR_INIT_TICKET_DELTA_TROPHY);
        ticketEvent.setStatus(TicketStatus.WAITING);
        ticketEvent.setBotType(profile.getBotType());
        ticketEvent.setListReward(eventRewards);
        mapEvents.put(userId, ticketEvent);
        return ticketEvent;
    }

    private void removeCancelTicket(List<TicketEvent> tickets) {
        tickets.removeIf(ticket -> ticket.getStatus() == TicketStatus.CANCELLED);
    }

    public void matchingWorker() {
        while (!TicketService.IS_MAINTAIN) {
            List<TicketEvent> ticketEvents = new ArrayList<>(mapEvents.values());
            removeCancelTicket(ticketEvents);
            Set<Long> matchedUserIds = new HashSet<>();
            boolean roomCreated = false;

            for (int i = 0; i < ticketEvents.size(); i++) {
                TicketEvent cur = ticketEvents.get(i);
                if (matchedUserIds.contains(cur.getUserId())) {
                    continue; // Skip if user is already matched
                }

                Set<TicketEvent> matchedEvents = new HashSet<>();
                matchedEvents.add(cur);
                matchedUserIds.add(cur.getUserId());
                cur.setStatus(TicketStatus.MATCHED);
                sseService.emitNextMsg(sendEventTicketSSE(cur), cur.getUserId());

                for (int j = 0; j < ticketEvents.size() && matchedEvents.size() < MAX_ROOM_SIZE; j++) {
                    TicketEvent opponent = ticketEvents.get(j);

                    if (matchedUserIds.contains(opponent.getUserId()) || cur.getUserId() == opponent.getUserId()) {
                        continue; // Skip if opponent is already matched or is the same as the current user
                    }

                    int deltaTrophy = Math.abs(opponent.getCurrentTrophy() - cur.getCurrentTrophy());
                    if (deltaTrophy <= opponent.getDeltaTrophy() && deltaTrophy <= cur.getDeltaTrophy()) {
                        matchedEvents.add(opponent);
                        matchedUserIds.add(opponent.getUserId());
                        opponent.setStatus(TicketStatus.MATCHED);
                        sseService.emitNextMsg(sendEventTicketSSE(cur), cur.getUserId());
                    }
                }

                // Add bot if room has less than 5 people and deltaTrophy condition has been set
                while (matchedEvents.size() < MAX_ROOM_SIZE) {
                    long botID = RandomUtils.random(27, 31);
                    if (!matchedUserIds.contains(botID)) {
                        TicketEvent ticketEvent = matchedEvents.stream().filter(ticket -> ticket.getBotType() == 0).findFirst().get();
                        int eventType = ticketEvent.getEventType();
                        int eventId = ticketEvent.getEventType();
                        TicketEvent botEvent = createBotTicketEvent(botID, eventType, eventId);
                        matchedEvents.add(botEvent);
                        matchedUserIds.add(botID);
                    }
                }

                if (matchedEvents.size() == MAX_ROOM_SIZE) {
                    RoomEvent roomEvent = createRoom(new ArrayList<>(matchedEvents));
                    String roomEventStr = gson.toJson(roomEvent);
                    redisTemplateString.opsForHash().put(getActiveRoomsKey(roomEvent.getEventType()), String.valueOf(roomEvent.getId()), roomEventStr);
                    sendEventSSE(roomEvent);
                    matchedEvents.forEach(event -> mapEvents.remove(event.getUserId()));
                    roomCreated = true;
                    break; // Break out of the inner loop to start matching from the beginning
                }
            }

            if (!roomCreated) {
                break; // No more rooms can be created
            }
        }
    }

    public String claim(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        long userID = adminService.getSession().getId();
        long roomID = jsonObject.get("roomID").getAsLong();
        int eventType = jsonObject.get("type").getAsInt();
        String activeRoomsKey = getActiveRoomsKey(eventType);
        String roomStr = (String)redisTemplateString.opsForHash().get(activeRoomsKey, String.valueOf(roomID));
        RoomEvent room = gson.fromJson(roomStr, RoomEvent.class);
        JsonArray updateRewards;
        List<RewardEnt> rewards = new ArrayList<>();
        if (room != null) {
            room.getListUser().sort(Comparator.comparing(UserParticipant::getScore, Comparator.reverseOrder())
                    .thenComparing(UserParticipant::getTimeReachedScore)
                    .thenComparing(UserParticipant::getTrophy, Comparator.reverseOrder()));
            for (int i = 0; i < room.getListUser().size(); i++) {
                UserParticipant participant = room.getListUser().get(i);
                if (participant.getUserId() == userID) {
                    if (!participant.isClaimed()) {
                        participant.setRank(i + 1);
                        rewards = claimReward(participant.getUserId(), participant.getRank(), room.getListReward());
                        participant.setClaimed(true);
                        String updatedRoomStr = gson.toJson(room);
                        redisTemplateString.opsForHash().put(activeRoomsKey, String.valueOf(roomID), updatedRoomStr);
                        break;
                    } else {
                        return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "User already claim!", NetWorkAPI.DEL_TICKET);
                    }
                }
            }
            updateRewards = JsonBuilder.buildListReward(rewards);
            return claimEventsBySession(userID, updateRewards);
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), "User already claim!", NetWorkAPI.DEL_TICKET);
    }

    private RoomEvent getRoomBySession(long userId, int eventType) {
        String activeRoomsKey = getActiveRoomsKey(eventType);
        Map<Object, Object> activeRoomsCache = redisTemplateString.opsForHash().entries(activeRoomsKey);
        for (Map.Entry<Object, Object> entry : activeRoomsCache.entrySet()) {
            String activeRoomStr = (String) entry.getValue();
            RoomEvent room = gson.fromJson(activeRoomStr, RoomEvent.class);
            room.getListUser().sort(Comparator.comparing(UserParticipant::getScore)
                    .reversed().thenComparing(UserParticipant::getTimeReachedScore));
            for (UserParticipant participant : room.getListUser()) {
                if (participant.getUserId() == userId) {
                    return room;
                }
            }
        }
        return null;
    }

    public String getRoom(int eventType) {
        JsonObject roomEventJson = new JsonObject();
        SessionObj sessionObj = adminService.getSession();
        long userId = sessionObj.getId();
        String activeRoomsKey = getActiveRoomsKey(eventType);
        Map<Object, Object> activeRoomsCache = redisTemplateString.opsForHash().entries(activeRoomsKey);
        for (Map.Entry<Object, Object> entry : activeRoomsCache.entrySet()) {
            String activeRoomStr = (String) entry.getValue();
            RoomEvent room = gson.fromJson(activeRoomStr, RoomEvent.class);
            room.getListUser().sort(Comparator.comparing(UserParticipant::getScore).reversed()
                    .thenComparing(UserParticipant::getTrophy).reversed()
                    .thenComparing(UserParticipant::getTimeReachedScore));
            for (UserParticipant participant : room.getListUser()) {
                if (participant.getUserId() == userId) {
                    setRoomRewardQuantity(room);
                    roomEventJson = JsonBuilder.eventRoomToJson(room);
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), roomEventJson, NetWorkAPI.PULL_TICKET);
                }
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), roomEventJson, NetWorkAPI.PULL_TICKET);
    }

    private void setRoomRewardQuantity(RoomEvent roomEvent) {
        for (int i = 0; i < roomEvent.getListUser().size(); i++) {
            UserParticipant userParticipant = roomEvent.getListUser().get(i);
            userParticipant.setRank(i + 1);
            setUserRewardQuantity(userParticipant);
        }
    }



    private void sendEventSSE(RoomEvent roomEvent) {
        for (UserParticipant userParticipant : roomEvent.getListUser()) {
            sseService.emitNextMsg(pullEventsBySession(userParticipant.getUserId()), userParticipant.getUserId());
        }
    }

    private void sendEventResultSSE(RoomEvent roomEvent) {
        for (UserParticipant userParticipant : roomEvent.getListUser()) {
            sseService.emitNextMsg(pullEventResultBySession(userParticipant.getUserId()), userParticipant.getUserId());
        }
    }

    private void sendEventUpdateSSE(long userID) {
        String eventStr= pullEventUpdateBySession(userID);
        JsonObject eventData = JSONUtil.DeSerialize(eventStr, JsonObject.class);
        JsonObject data = eventData.get("data").getAsJsonObject();
        if (data.size() > 0) {
            sseService.emitNextMsg(pullEventUpdateBySession(userID), userID);
        }
    }

    public void checkAndSendResult() {
        long currentTime = System.currentTimeMillis();
        Set<String> activeRoomKeys = redisTemplateString.keys("rush_active_rooms_*");
        for (String activeRoomKey : activeRoomKeys) {
            Map<Object, Object> activeRoomsCache = redisTemplateString.opsForHash().entries(activeRoomKey);
            for (Map.Entry<Object, Object> entry : activeRoomsCache.entrySet()) {
                String roomKey = (String) entry.getKey();
                String activeRoomStr = (String) entry.getValue();
                RoomEvent room = gson.fromJson(activeRoomStr, RoomEvent.class);
                long isClaimedCount = room.getListUser().stream().filter(UserParticipant::isClaimed).count();
                if (isClaimedCount == MAX_ROOM_SIZE) {
                    redisTemplateString.opsForHash().delete(activeRoomKey, roomKey);
                    continue;
                }
                if (room.getEndRoomTime() <= currentTime && !room.isSendResult()) {
                    //check if is bot, auto set -> claimed
                    for (UserParticipant participant : room.getListUser()) {
                        if (participant.getBotType() != 0) {
                            participant.setClaimed(true);
                        }
                    }
                    sendEventResultSSE(room);
                    room.setSendResult(true);
                    String updatedRoomStr = gson.toJson(room);
                    redisTemplateString.opsForHash().put(activeRoomKey, roomKey, updatedRoomStr);
                }
            }
        }
    }

    private void setUserRewardQuantity(UserParticipant userParticipant) {
        int rank = userParticipant.getRank();
        userParticipant.setRewardType(ERewardType.GOLD);
        switch (rank) {
            case 1:
                userParticipant.setRewardQuantity(GameConstant.RUSH_EVENT_RANK_1);
                break;
            case 2:
                userParticipant.setRewardQuantity(GameConstant.RUSH_EVENT_RANK_2);
                break;
            case 3:
                userParticipant.setRewardQuantity(GameConstant.RUSH_EVENT_RANK_3);
                break;
            case 4:
            case 5:
                userParticipant.setRewardQuantity(GameConstant.RUSH_EVENT_RANK_4_5);
                break;
        }
    }

    private List<RewardEnt> claimReward(long userId, int rank, List<EventReward> eventRewards) {
        return checkAndClaim(eventRewards, rank, userId);
    }

    private List<RewardEnt> checkAndClaim(List<EventReward> eventRewards, int rank, long userId) {
        List<RewardEnt> list = new ArrayList<>();
        for (EventReward reward : eventRewards) {
            if (rank == reward.getRank()) {
                ERewardType eRewardType = ERewardType.findByValue(reward.getRewardType());
                int quantity = reward.getAmount();
                list.addAll(inventoryService.claimItem(userId, eRewardType, quantity, EUpdateMoneyType.RUSH_ARENA));
            }
        }
        return list;
    }

    private String getActiveRoomsKey(int eventType) {
        return String.format(ACTIVE_ROOMS_KEY, eventType);
    }

    private TicketEvent createBotTicketEvent(long botId, int eventType, int eventId) {
        TicketEvent botEventTicket = new TicketEvent();
        Profile profile = profileService.getMinProfileByID(botId);
        int botTrophy = leaderboardService.getProfileTrophy(botId);
        botEventTicket.setUserId(botId);
        botEventTicket.setUserName(profile.getUsername());
        botEventTicket.setCurrentTrophy(botTrophy);
        botEventTicket.setBotType(profile.getBotType());
        botEventTicket.setEventType(eventType);
        botEventTicket.setEventId(eventId);
        return botEventTicket;
    }

    public int getBlankRoom() {
        return redisTemplateString.opsForValue().increment(ROOM_EVENT_ID_KEY).intValue();
    }

    private RoomEvent createRoom(List<TicketEvent> tickets) {
        tickets.forEach(ticketEvent -> ticketEvent.setStatus(TicketStatus.ON_ROOM));
        RoomEvent roomEvent = new RoomEvent();
        roomEvent.setId(getBlankRoom());
        TicketEvent userTicket = tickets.stream().filter(ticketEvent -> ticketEvent.getBotType() == 0).findFirst().get();
        EventEnt eventEnt = eventRepository.read().getById(userTicket.getEventId());
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        roomEvent.setEventType(userTicket.getEventType());
        roomEvent.setStartEventTime(userTicket.getEventStartTime());
        roomEvent.setEndEventTime(userTicket.getEventEndTime());
        roomEvent.setStartRoomTime(System.currentTimeMillis());
        roomEvent.setListReward(userTicket.getListReward());
        long roomDuration = customData.get("roomDuration").getAsLong();
        long currentTime = System.currentTimeMillis();
        long remainTime = roomEvent.getEndEventTime() - currentTime;
        if (remainTime > roomDuration) {
            roomEvent.setEndRoomTime(System.currentTimeMillis() + roomDuration);
        } else {
            roomEvent.setEndRoomTime(System.currentTimeMillis() + remainTime);
        }

        List<UserParticipant> userParticipants = new ArrayList<>();
        List<Integer> botLevels = getBotLevels(tickets);
        int count = 0;
        for (TicketEvent ticket : tickets) {
            UserParticipant participant = new UserParticipant();
            participant.setUserId(ticket.getUserId());
            participant.setUserName(ticket.getUserName());
            participant.setTrophy(ticket.getCurrentTrophy());
            participant.setBotType(ticket.getBotType());
            participant.setTimeReachedScore(currentTime);
            if (participant.getBotType() != 0) {
                if (count < botLevels.size()) {
                    participant.setBotLevel(botLevels.get(count));
                    count++;
                }
            }
            userParticipants.add(participant);
            //create redis key for user join event : event_[eid]_[uid]
            String userEventKey = getUserEventInfoKey(ticket.getEventType(), ticket.getUserId());
            long expireTime = roomEvent.getEndEventTime() - System.currentTimeMillis();
            redisTemplateString.opsForValue().setIfAbsent(userEventKey, "0", expireTime, TimeUnit.MILLISECONDS);
            //create redis key for list event of user: events_[uid]
            String userListEventKey = getUserListEventInfoKey(ticket.getUserId());
            redisTemplateString.opsForSet().add(userListEventKey, userEventKey);
            redisTemplateString.expire(userListEventKey, 7, TimeUnit.DAYS);
        }
        roomEvent.setListUser(userParticipants);
        logger.info("Created Room : {}", roomEvent);
        return roomEvent;
    }

    private List<Integer> getBotLevels(List<TicketEvent> tickets) {
        long realUserCount = tickets.stream().filter(ticketEvent -> ticketEvent.getBotType() == 0).count();

        // Add bots with appropriate levels for calculate bot point
        List<Integer> botLevels;
        if (realUserCount == 1) {
            botLevels = Arrays.asList(1,2,3,4);
        } else if (realUserCount == 2) {
            botLevels = Arrays.asList(2,3,4);
        } else if (realUserCount == 3) {
            botLevels = Arrays.asList(3,4);
        } else if (realUserCount == 4) {
            botLevels = Collections.singletonList(4);
        } else {
            botLevels = Collections.emptyList();
        }
        return botLevels;
    }

    private String getUserEventInfoKey(long eventId, long userId) {
        return String.format(USER_EVENT_INFO_KEY, eventId, userId);
    }

    private String getUserListEventInfoKey(long userId) {
        return String.format(LIST_EVENT_USER_INFO, userId);
    }

    public void updateScore(long userId, int eventType, String userEventKey, long winId, JsonArray jsonArray,
                            int matchRoundCount, EEventType eEventType, long matchEndAtMs) {
        String activeRoomsKey = getActiveRoomsKey(eventType);
        Map<Object, Object> activeRoomsCache = redisTemplateString.opsForHash().entries(activeRoomsKey);
        for (Map.Entry<Object, Object> entry : activeRoomsCache.entrySet()) {
            String roomKey = (String) entry.getKey();
            String activeRoomStr = (String) entry.getValue();
            RoomEvent room = gson.fromJson(activeRoomStr, RoomEvent.class);
            if (matchEndAtMs > room.getEndRoomTime()) {
                return;
            }
            for (int i = 0; i < room.getListUser().size(); i++) {
                UserParticipant participant = room.getListUser().get(i);
                int profileTrophy = leaderboardService.getProfileTrophy(participant.getUserId());
                participant.setTrophy(profileTrophy);
                if (participant.getUserId() == userId) {
                    int point;
                    if (EEventType.RUSH_SAVE_THE_HEART.equals(eEventType)) {
                        point = calculateRushHeartPoint(userId, winId, jsonArray);
                    } else if (EEventType.RUSH_ACTIVE_LIGHTNING.equals(eEventType)) {
                        point = calculateGem5Point(userId, jsonArray);
                    } else if (EEventType.RUSH_WIN_STREAK.equals(eEventType)) {
                        point = calculateWinStreakPoint(userId, winId, room.getEndEventTime());
                    } else if (EEventType.RUSH_PERFECT_VICTORY.equals(eEventType)) {
                        point = calculatePerfectVictory(userId, winId, matchRoundCount);
                    } else {
                        continue;
                    }

                    if (participant.getBotType() != 0) {
                        int botPoint = calculateBotPoint(participant.getBotLevel(), point);
                        redisTemplateString.opsForValue().increment(userEventKey, botPoint);
                    } else {
                        if (EEventType.RUSH_WIN_STREAK.equals(eEventType)) {
                            redisTemplateString.opsForValue().set(userEventKey, String.valueOf(point));
                        } else {
                            redisTemplateString.opsForValue().increment(userEventKey, point);
                        }
                    }

                    String value = redisTemplateString.opsForValue().get(userEventKey);
                    int score = Integer.parseInt(value);
                    participant.setScore(score);
                    participant.setTimeReachedScore(matchEndAtMs);
                    String updatedRoomStr = gson.toJson(room);
                    redisTemplateString.opsForHash().put(activeRoomsKey, roomKey, updatedRoomStr);
                }
            }
        }
    }

    private int calculateBotPoint(int botLevel, int point) {
        int botPoint;
        switch (botLevel) {
            case 1:
                botPoint = (int)Math.ceil( (double)point / 2);
                break;
            case 2:
                botPoint = (int)Math.ceil( (double)point / 2.5);
                break;
            case 3:
                botPoint = (int)Math.ceil( (double) point / 3);
                break;
            case 4:
                botPoint = new Random().nextInt(2); // random between 0 and 1
                break;
            default:
                botPoint = point; // Default case, should not happen
                break;
        }
        return botPoint;
    }

    public void listenEndGame(GameLog gameLog) {
        JsonArray jsonArray = JSONUtil.DeSerialize(gameLog.getRounds(), JsonArray.class);
        long winId = gameLog.getWinID();
        List<Long> userIds = gameLog.getListUserID();
        for (Long userId : userIds) {
            String userListEventKey = getUserListEventInfoKey(userId);
            Set<String> userEvents = redisTemplateString.opsForSet().members(userListEventKey);
            for (String userEventKey : userEvents) {
                int eventType = Integer.parseInt(userEventKey.substring(6,8));
                EEventType eEventType = EEventType.findByValue(eventType);
                updateScore(userId, eventType, userEventKey, winId, jsonArray, gameLog.getMatchRoundCount(), eEventType, gameLog.getMatchEndAtMs());
            }
            sendEventUpdateSSE(userId);
        }
    }

    private int calculateRushHeartPoint(long userId, long winId, JsonArray jsonArray) {
        int countHeart = 0;
        if (winId == -1) {
            return countHeart;
        }
        if (userId == winId) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                JsonArray playerTurn = jsonObject.get("playerTurns").getAsJsonArray();
                for (int j = playerTurn.size() - 1; j >= 0; j--) {
                    JsonObject playerTurnJson = playerTurn.get(j).getAsJsonObject();
                    int roundPoint = playerTurnJson.get("roundPoint").getAsInt();
                    if (playerTurnJson.get("playerId").getAsInt() == userId) {
                       countHeart = roundPoint;
                       break;
                    }
                }
            }
        }
        return countHeart;
    }

    private int calculateGem5Point(long userId, JsonArray jsonArray) {
        int countGem5 = 0;
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            JsonArray playerTurn = jsonObject.get("playerTurns").getAsJsonArray();
            for (int j = 0; j < playerTurn.size(); j++) {
                JsonObject playerTurnJson = playerTurn.get(j).getAsJsonObject();
                if (playerTurnJson.get("playerId").getAsInt() == userId) {
                    countGem5 += playerTurnJson.get("explodeSpecial5Count").getAsInt();
                }
            }
        }
        return countGem5;
    }

    private int calculateWinStreakPoint(long userId, long winId, long endEventTime) {
        long expireTime = endEventTime - System.currentTimeMillis();
        redisTemplateString.opsForValue().setIfAbsent(getRushWinStreakKey(userId), "0", expireTime, TimeUnit.MILLISECONDS);
        redisTemplateString.opsForValue().setIfAbsent(getRushHighestWinStreak(userId), "0", expireTime, TimeUnit.MILLISECONDS);
        int point = Integer.parseInt(Objects.requireNonNull(redisTemplateString.opsForValue().get(getRushHighestWinStreak(userId))));
        if (userId == winId) {
            redisTemplateString.opsForValue().increment(getRushWinStreakKey(userId));
            int winStreak = Integer.parseInt(Objects.requireNonNull(redisTemplateString.opsForValue().get(getRushWinStreakKey(userId))));
            int highestWinStreak = Integer.parseInt(Objects.requireNonNull(redisTemplateString.opsForValue().get(getRushHighestWinStreak(userId))));
            if (winStreak >= highestWinStreak) {
                point = winStreak;
                redisTemplateString.opsForValue().set(getRushHighestWinStreak(userId), String.valueOf(winStreak));
            } else {
                point = highestWinStreak;
            }
        } else {
            redisTemplateString.opsForValue().set(getRushWinStreakKey(userId), "0");
        }
        return point;
    }

    private int calculatePerfectVictory(long userId, long winId, int matchRoundCount) {
        int point = 0;
        if (userId == winId) {
            if (matchRoundCount == 2) {
                point = 1;
            }
        }
        return point;
    }

    private String getRushWinStreakKey(long userId) {
        return String.format(WIN_STREAK_KEY, userId);
    }

    private String getRushHighestWinStreak(long userId) {
        return String.format(HIGHEST_WIN_STREAK_KEY, userId);
    }

    public String getEvents() {
        return pullEventsBySession(adminService.getSession().getId());
    }

    public String pullEventsBySession(long userId) {
        JsonObject jsonObject = getCurrentEvent(userId);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.RUSH_MATCH_COMPLETED);
    }

    public String claimEventsBySession(long userId, JsonArray rewards) {
        JsonObject jsonObject = getCurrentEvent(userId);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, rewards, 0).toString();
    }

    public String pullEventResultBySession(long userID) {
        JsonObject jsonObject = getCurrentEvent(userID);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.RUSH_COMPLETED);
    }

    public String pullEventUpdateBySession(long userID) {
        JsonObject jsonObject = getCurrentEvent(userID);
        JsonObject eventData = new JsonObject();
        JsonArray events = jsonObject.getAsJsonArray("events");
        for (JsonElement item : events) {
            JsonObject itemJson = item.getAsJsonObject();
            if (itemJson.get("listUser").getAsJsonArray().size() != 0) {
                eventData = itemJson;
                break;
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), eventData, NetWorkAPI.RUSH_ROOM_UPDATE);
    }

    public JsonObject getCurrentEvent(long userID) {
        JsonObject jsonObject = new JsonObject();
        JsonArray listEventArr = new JsonArray();
        JsonArray listEventArrBk = new JsonArray();
        List<EventEnt> listEvent = eventRepository.read().findByType(EEventType.RUSH_ARENA.ordinal());
        for (EventEnt eventEnt : listEvent) {
            JsonObject eventJson = buildEventJson(eventEnt, userID);

            listEventArr.add(eventJson);
        }

        for (EventEnt eventEnt : listEvent) {
            JsonObject eventJson = buildEventJsonUpComing(eventEnt);
            listEventArrBk.add(eventJson);
        }

        //check if user played this event, swap with the next time of this event (move it to coming soon)
        boolean isPlayed = false;
        int indexToReplace = -1;
        int type = 0;
        for (int i = 0; i < listEventArr.size(); i++) {
            JsonObject event = listEventArr.get(i).getAsJsonObject();
            if (event.get("isRunning").getAsBoolean()) {
                type = event.get("type").getAsInt();
                String userEventKey = getUserEventInfoKey(type, userID);
                boolean isJoined = Boolean.TRUE.equals(redisTemplateString.hasKey(userEventKey));
                if (isJoined) {
                    isPlayed = true;
                    indexToReplace = i;
                    break;
                }
            }
        }
        if (isPlayed) {
            RoomEvent roomEvent = getRoomBySession(userID, type);
            //when room not delete yet, check user is claim reward or not -> if claimed, swap event to the upcoming
            if (roomEvent != null) {
                for (UserParticipant userParticipant : roomEvent.getListUser()) {
                    if (userParticipant.getUserId() == userID) {
                        if (userParticipant.isClaimed()) {
                            for (int i = 0; i < listEventArrBk.size(); i++) {
                                JsonObject backupEvent = listEventArrBk.get(i).getAsJsonObject();
                                if (backupEvent.get("type").getAsInt() == type) {
                                    listEventArr.set(indexToReplace, backupEvent);
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                //when room deleted(all user are claimed reward) -> swap event to the up coming
                for (int i = 0; i < listEventArrBk.size(); i++) {
                    JsonObject backupEvent = listEventArrBk.get(i).getAsJsonObject();
                    if (backupEvent.get("type").getAsInt() == type) {
                        listEventArr.set(indexToReplace, backupEvent);
                        break;
                    }
                }
            }
        }

        // Sort the list of events by startTime
        List<JsonObject> eventList = new ArrayList<>();
        for (int i = 0; i < listEventArr.size(); i++) {
            JsonObject event = listEventArr.get(i).getAsJsonObject();
            JsonArray listUser = event.getAsJsonArray("listUser");

            // Sort the listUser array
            List<JsonObject> users = new ArrayList<>();
            if (listUser != null) {
                for (int j = 0; j < listUser.size(); j++) {
                    users.add(listUser.get(j).getAsJsonObject());
                }
            }


            users.sort(Comparator.comparing((JsonObject o) -> o.get("progress").getAsInt(), Comparator.reverseOrder())
                    .thenComparing((JsonObject o) -> o.get("trophy").getAsInt(), Comparator.reverseOrder())
                    .thenComparing(o -> o.get("timeReachedScore").getAsLong()));

            // Convert the sorted list back to JsonArray
            JsonArray sortedListUser = new JsonArray();
            for (int z = 0; z < users.size(); z++) {
                JsonObject sortedItem = users.get(z).getAsJsonObject();
                sortedItem.addProperty("rank", z + 1);
                sortedListUser.add(sortedItem);
            }

            // Replace the original listUser with the sorted one
            event.add("listUser", sortedListUser);
            eventList.add(event);
        }
        eventList.sort(Comparator.comparingLong(e -> e.get("startTime").getAsLong()));
        listEventArr = new JsonArray();
        String userListEventKey = getUserListEventInfoKey(userID);
        for (JsonObject event : eventList) {
            //clear upcoming from listEventUser key redis
            if (!event.get("isRunning").getAsBoolean()) {
                String userEventKey = getUserEventInfoKey(event.get("type").getAsInt(), userID);
                redisTemplateString.opsForSet().remove(userListEventKey, userEventKey);
            }
            listEventArr.add(event);
        }
        jsonObject.add("events", listEventArr);
        return jsonObject;
    }

    private JsonObject buildEventJson(EventEnt eventEnt, long userID) {
        JsonObject eventJson = new JsonObject();

        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        JsonArray listReward = customData.getAsJsonArray("listReward");
        int eventType = customData.get("type").getAsInt();
        List<EventTimeDTO> eventTimeDTOS = buildEventTimes(customData);
        eventJson.addProperty("id", eventEnt.getId());
        eventJson.addProperty("title", eventEnt.getTitle());
        eventJson.addProperty("type", eventType);
        eventJson.addProperty("rewardType", customData.get("rewardType").getAsInt());
        setEventTimes(eventTimeDTOS, eventJson);
        eventJson.add("listReward", listReward);

        JsonArray listUser = new JsonArray();
        RoomEvent roomEvent = getRoomBySession(userID, eventType);
        if (roomEvent != null) {
            eventJson.addProperty("endRoomTime", roomEvent.getEndRoomTime());

            for (int i = 0; i < roomEvent.getListUser().size(); i++) {
                UserParticipant participant = roomEvent.getListUser().get(i);
                JsonObject jsonObject = new JsonObject();
                Profile profile = profileService.getMinProfileByID(participant.getUserId());
                int trophy = leaderboardService.getProfileTrophy(profile.getId());
                JsonObject user = JsonBuilder.profileToJson(profile, trophy);
                jsonObject.add("user", user);
                jsonObject.addProperty("progress", participant.getScore());
                jsonObject.addProperty("roomID", roomEvent.getId());
                jsonObject.addProperty("trophy", participant.getTrophy());
                jsonObject.addProperty("timeReachedScore", participant.getTimeReachedScore());
                jsonObject.addProperty("isClaimed", participant.isClaimed());
                listUser.add(jsonObject);
            }
        }
        eventJson.add("listUser", listUser);

        return eventJson;
    }

    private JsonObject buildEventJsonUpComing(EventEnt eventEnt) {
        JsonObject eventJson = new JsonObject();
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        JsonArray listReward = customData.getAsJsonArray("listReward");
        List<EventTimeDTO> eventTimeDTOS = buildEventTimes(customData);
        eventJson.addProperty("id", eventEnt.getId());
        eventJson.addProperty("title", eventEnt.getTitle());
        eventJson.addProperty("type", customData.get("type").getAsInt());
        eventJson.addProperty("rewardType", customData.get("rewardType").getAsInt());
        setEventTimesUpComing(eventTimeDTOS, eventJson);
        eventJson.add("listReward", listReward);

        return eventJson;
    }

    private boolean checkUserInRoom(int eventType, long userID) {
        boolean userInRoom = false;
        String activeRoomsKey = getActiveRoomsKey(eventType);
        Map<Object, Object> activeRoomsCache = redisTemplateString.opsForHash().entries(activeRoomsKey);
        for (Map.Entry<Object, Object> entry : activeRoomsCache.entrySet()) {
            String activeRoomStr = (String) entry.getValue();
            RoomEvent room = gson.fromJson(activeRoomStr, RoomEvent.class);
            for (int i = 0; i < room.getListUser().size(); i++) {
                UserParticipant participant = room.getListUser().get(i);
                if (participant.getUserId() == userID) {
                    userInRoom = true;
                    break;
                }
            }
            break;
        }
        return userInRoom;
    }

    private List<EventTimeDTO> buildEventTimes(JsonObject customData) {
        List<EventTimeDTO> eventTimeDTOS = new ArrayList<>();
        JsonArray activeTimes = customData.get("activeTimes").getAsJsonArray();
        for (int i = 0; i < activeTimes.size(); i++) {
            JsonArray times = activeTimes.get(i).getAsJsonArray();
            int startDay = times.get(0).getAsInt();
            int startHour = times.get(1).getAsInt();
            int startMinute = times.get(2).getAsInt();
            int endDay = times.get(3).getAsInt();
            int endHour = times.get(4).getAsInt();
            int endMinute = times.get(5).getAsInt();

            EventTimeDTO eventTimeDTO = new EventTimeDTO();
            eventTimeDTO.setStartDay(startDay);
            eventTimeDTO.setStartHour(startHour);
            eventTimeDTO.setStartMinutes(startMinute);
            eventTimeDTO.setEndDay(endDay);
            eventTimeDTO.setEndHour(endHour);
            eventTimeDTO.setEndMinutes(endMinute);

            eventTimeDTOS.add(eventTimeDTO);
        }
        return eventTimeDTOS;
    }

    private void setEventTimes(List<EventTimeDTO> eventTimeDTOS, JsonObject eventJson) {
        long startInMillis = 0;
        long endInMillis = 0;
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalTime currentTime = LocalTime.now(zoneId);
        LocalDateTime now = LocalDateTime.of(today, currentTime);
        EventTimeDTO currentEvent = null;
        EventTimeDTO nextEvent = null;
        LocalDateTime nextEventStart = null;
        LocalDateTime start = null;
        LocalDateTime end = null;
        for (EventTimeDTO item : eventTimeDTOS) {
            LocalDateTime itemStart = item.getStartDateTime(today);
            LocalDateTime itemEnd = item.getEndDateTime(today);
            if (itemStart.isBefore(now) && itemEnd.isBefore(now)) {
                itemStart = itemStart.plusWeeks(1);
                itemEnd = itemEnd.plusWeeks(1);
            }
            if (itemStart.isAfter(itemEnd)) {
                itemStart = itemStart.minusWeeks(1);
            }
            if (now.isAfter(itemStart) && now.isBefore(itemEnd)) {
                currentEvent = item;
                start = itemStart;
                end = itemEnd;
                break;
            } else if (now.isBefore(itemStart)) {
                if (nextEvent == null || itemStart.isBefore(nextEventStart)) {
                    nextEvent = item;
                    nextEventStart = itemStart;
                    start = itemStart;
                    end = itemEnd;
                }
            }
        }
        if (currentEvent != null) {
            startInMillis = start.atZone(zoneId).toInstant().toEpochMilli();
            endInMillis = end.atZone(zoneId).toInstant().toEpochMilli();
        }
        if (nextEvent != null) {
            startInMillis = start.atZone(zoneId).toInstant().toEpochMilli();
            endInMillis = end.atZone(zoneId).toInstant().toEpochMilli();
        }
        long nowTime = now.atZone(zoneId).toInstant().toEpochMilli();
        eventJson.addProperty("isRunning", startInMillis <= nowTime && endInMillis >= nowTime);
        eventJson.addProperty("startTime", startInMillis);
        eventJson.addProperty("endTime", endInMillis);
    }


    private void setEventTimesUpComing(List<EventTimeDTO> eventTimeDTOS, JsonObject eventJson) {
        long startInMillis = 0;
        long endInMillis = 0;
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalTime currentTime = LocalTime.now(zoneId);
        LocalDateTime now = LocalDateTime.of(today, currentTime);
        EventTimeDTO nextEvent = null;
        LocalDateTime nextEventStart = null;
        LocalDateTime start = null;
        LocalDateTime end = null;
        for (EventTimeDTO item : eventTimeDTOS) {
            LocalDateTime itemStart = item.getStartDateTime(today);
            LocalDateTime itemEnd = item.getEndDateTime(today);
            if (now.isBefore(itemStart)) {
                if (nextEvent == null || itemStart.isBefore(nextEventStart)) {
                    nextEvent = item;
                    nextEventStart = itemStart;
                    start = itemStart;
                    end = itemEnd;
                }
            }
        }
        if (nextEvent != null) {
            startInMillis = start.atZone(zoneId).toInstant().toEpochMilli();
            endInMillis = end.atZone(zoneId).toInstant().toEpochMilli();
            eventJson.addProperty("isRunning", false);
        }
        eventJson.addProperty("startTime", startInMillis);
        eventJson.addProperty("endTime", endInMillis);
    }

}
