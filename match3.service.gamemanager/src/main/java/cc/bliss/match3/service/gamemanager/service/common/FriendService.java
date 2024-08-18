/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.match3.FriendWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.common.LeaderBoardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.EFriendRequestType;
import cc.bliss.match3.service.gamemanager.ent.enums.EFriendStatus;
import cc.bliss.match3.service.gamemanager.ent.enums.EProfileStatus;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.FriendEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.TicketService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class FriendService extends BaseService {

    /*
         battle_request_{requestID}_{responseID}
     */
    private final String BATTLE_REQUEST_FORMAT = "battle_request_%s_%s";
    @Autowired
    private AdminService adminService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private TicketService ticketService;

    private String getBattleRequestKey(long requestID, long responseID) {
        return String.format(BATTLE_REQUEST_FORMAT, requestID, responseID);
    }

    public void saveFriend(FriendEnt friendRequest, long userID, long friendID) {
        FriendEnt friendEnt = new FriendEnt();
        if (friendRequest.getFriendID() == friendID) {
            friendEnt.setFriendID(userID);
            friendEnt.setUserID(friendID);
        } else {
            friendEnt.setFriendID(friendID);
            friendEnt.setUserID(userID);
        }
        friendEnt.setFriendStatus(EFriendStatus.FRIEND.ordinal());
        friendRequest.setFriendStatus(EFriendStatus.FRIEND.ordinal());

        insertMatch3SchemaListData(Arrays.asList(friendEnt, friendRequest));
    }

    public String sendFriendRequest(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int type = jsonObject.get("type").getAsInt();
        long friendID = jsonObject.get("friendID").getAsLong();
        EFriendRequestType friendRequestType = EFriendRequestType.findByValue(type);
        switch (friendRequestType) {
            case SEND_FRIEND_REQUEST: {
                FriendEnt isFriend = friendRepository.read().findByUserIDAndFriendID(session.getId(), friendID, EFriendStatus.FRIEND.ordinal());
                if (isFriend != null) {
                    return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Đã là bạn !", NetWorkAPI.SEND_FRIEND_REQUEST);
                }
                FriendEnt userRequest = friendRepository.read().findByUserIDAndFriendID(session.getId(), friendID, EFriendStatus.FRIEND_REQUEST.ordinal());
                if (userRequest != null) {
                    return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Đã gửi lời mời kết bạn !", NetWorkAPI.SEND_FRIEND_REQUEST);
                }
                FriendEnt friendRequest = friendRepository.read().findByUserIDAndFriendID(friendID, session.getId(), EFriendStatus.FRIEND_REQUEST.ordinal());
                if (friendRequest != null) {
                    if (friendRepository.read().countListFriend(friendID) >= GameConstant.FRIEND_LIMIT) {
                        return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Đối phương đã vượt quá giới hạn kết bạn !", NetWorkAPI.SEND_FRIEND_REQUEST);
                    }
                    if (friendRepository.read().countListFriend(session.getId()) >= GameConstant.FRIEND_LIMIT) {
                        return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Bạn đã vượt quá giới hạn kết bạn !", NetWorkAPI.SEND_FRIEND_REQUEST);
                    }
                    saveFriend(friendRequest, friendID, session.getId());
                } else {
                    userRequest = new FriendEnt();
                    userRequest.setFriendID(friendID);
                    userRequest.setUserID(session.getId());
                    userRequest.setFriendStatus(EFriendStatus.FRIEND_REQUEST.ordinal());
                    insertMatch3SchemaData(userRequest);
                }
            }
            break;
            case ACCEPT_FRIEND_REQUEST: {
                FriendEnt friendRequest = friendRepository.read().findByUserIDAndFriendID(friendID, session.getId(), EFriendStatus.FRIEND_REQUEST.ordinal());
                if (friendRequest != null) {
                    if (friendRepository.read().countListFriend(friendID) >= GameConstant.FRIEND_LIMIT) {
                        return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Đối phương đã vượt quá giới hạn kết bạn !", NetWorkAPI.SEND_FRIEND_REQUEST);
                    }
                    if (friendRepository.read().countListFriend(session.getId()) >= GameConstant.FRIEND_LIMIT) {
                        return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Bạn đã vượt quá giới hạn kết bạn !", NetWorkAPI.SEND_FRIEND_REQUEST);
                    }
                    saveFriend(friendRequest, friendID, session.getId());
                }
            }
            break;
            case DENY_FRIEND_REQUEST: {
                FriendEnt friendRequest = friendRepository.read().findByUserIDAndFriendID(friendID, session.getId(), EFriendStatus.FRIEND_REQUEST.ordinal());
                if (friendRequest != null) {
                    friendRepository.write().delete(friendRequest);
                }
            }
            break;
            case REMOVE_FRIEND: {
                FriendEnt userRequest = friendRepository.read().findByUserIDAndFriendID(session.getId(), friendID, EFriendStatus.FRIEND.ordinal());
                FriendEnt friendRequest = friendRepository.read().findByUserIDAndFriendID(friendID, session.getId(), EFriendStatus.FRIEND.ordinal());
                friendRepository.write().delete(userRequest);
                friendRepository.write().delete(friendRequest);
            }
            break;
            case SEND_FRIEND_BATTLE: {
                String battleKey = getBattleRequestKey(session.getId(), friendID);
                redisTemplateString.opsForValue().set(battleKey, "", GameConstant.FRIEND_BATTLE_EXPIRE, TimeUnit.MILLISECONDS);
            }
            break;
            case ACCEPT_FRIEND_BATTLE: {
                String battleKey = getBattleRequestKey(friendID, session.getId());
                redisTemplateString.delete(battleKey);
                ticketService.friendBattle(session.getId(), friendID);
            }
            break;
            case DENY_FRIEND_BATTLE: {
                String battleKey = getBattleRequestKey(friendID, session.getId());
                redisTemplateString.delete(battleKey);
            }
            break;
            default:
                return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), NetWorkAPI.SEND_FRIEND_REQUEST);
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.SEND_FRIEND_REQUEST);
    }

    public JsonObject buildLeaderBoardEnt(Profile profile) {
        int profileTrophy = leaderboardService.getProfileTrophy(profile.getId());
        JsonObject jsonObject = JsonBuilder.profileToJson(profile, profileTrophy);
        return jsonObject;
    }

    public String getListFriend() {
        SessionObj session = adminService.getSession();
        List<FriendEnt> friendEnts = friendRepository.read().findListFriend(session.getId());
        Set<Long> listID = friendEnts.stream().map(e -> e.getFriendID()).collect(Collectors.toSet());
        Map<Long, Profile> mapProfile = profileService.getMapByListId(listID);
        JsonArray jsonArray = new JsonArray();
        for (FriendEnt friendEnt : friendEnts) {
            Profile profile = mapProfile.get(friendEnt.getFriendID());
            JsonObject jsonObject = buildLeaderBoardEnt(profile);

            String sendBattleKey = getBattleRequestKey(session.getId(), profile.getId());
            boolean isSendBattle = redisTemplateString.opsForValue().get(sendBattleKey) != null;
            String receiveBattleKey = getBattleRequestKey(profile.getId(), session.getId());
            boolean isReceiveBattle = redisTemplateString.opsForValue().get(receiveBattleKey) != null;
            if (isSendBattle) {
                jsonObject.addProperty("status", EProfileStatus.SEND_BATTLE_REQUEST.ordinal());
            } else if (isReceiveBattle) {
                jsonObject.addProperty("status", EProfileStatus.RECEIVE_BATTLE_REQUEST.ordinal());
            } else {
                jsonObject.addProperty("status", EProfileStatus.OFFLINE.ordinal());
            }
            jsonArray.add(jsonObject);
        }
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        response.addProperty("online", friendEnts.size());
        response.addProperty("friendCount", jsonArray.size());
        response.addProperty("friendLimit", GameConstant.FRIEND_LIMIT);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.FRIEND_LIST);
    }

    public String getListFriendLeaderboard() {
        SessionObj session = adminService.getSession();
        List<FriendEnt> friendEnts = friendRepository.read().findListFriend(session.getId());
        Set<Long> listID = friendEnts.stream().map(e -> e.getFriendID()).collect(Collectors.toSet());
        listID.add(session.getId());

        Map<Long, Profile> mapProfile = profileService.getMapByListId(listID);
        List<LeaderBoardEnt> leaderBoardEnts = leaderboardService.getListByIds(listID);
        List<LeaderBoardEnt> leaderboards = new ArrayList<>();
        leaderboards.addAll(leaderBoardEnts);
        int currentRank;
        LeaderBoardEnt currentUser;
        if (leaderBoardEnts.stream().anyMatch(e -> e.getUserID() == session.getId())) {
            currentUser = leaderBoardEnts.stream().filter(e -> e.getUserID() == session.getId()).findFirst().get();
            currentRank = leaderBoardEnts.indexOf(currentUser);
        } else {
            currentUser = new LeaderBoardEnt(session.getId().intValue(), 100);
            currentRank = leaderBoardEnts.size() + 1;
        }

        JsonArray jsonArray = new JsonArray();
        for (LeaderBoardEnt leaderboard : leaderboards) {
            JsonObject object = leaderboardService.buildLeaderBoardEnt(leaderboard,
                    mapProfile.get(leaderboard.getUserID()), leaderBoardEnts.indexOf(leaderboard) + 1);
            jsonArray.add(object);
        }
        jsonArray.add(leaderboardService.buildLeaderBoardEnt(currentUser, mapProfile.get(currentUser.getUserID()), currentRank));
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.FRIEND_LEADERBOARD_LIST);
    }

    public String getListFriendRequest() {
        SessionObj session = adminService.getSession();
        List<FriendEnt> friendEnts = friendRepository.read().findListFriendRequest(session.getId());
        Set<Long> listID = friendEnts.stream().map(e -> e.getFriendID()).collect(Collectors.toSet());
        Map<Long, Profile> mapProfile = profileService.getMapByListId(listID);
        JsonArray jsonArray = new JsonArray();
        for (FriendEnt friendEnt : friendEnts) {
            Profile profile = mapProfile.get(friendEnt.getFriendID());
            JsonObject jsonObject = buildLeaderBoardEnt(profile);
            jsonArray.add(jsonObject);
        }
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.FRIEND_REQUEST_LIST);
    }
}
