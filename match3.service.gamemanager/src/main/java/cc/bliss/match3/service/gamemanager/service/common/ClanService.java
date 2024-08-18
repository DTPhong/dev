/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import bliss.lib.framework.util.JSONUtil;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.ClanRepository;
import cc.bliss.match3.service.gamemanager.db.match3.ClanMemberWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.ClanWriteRepository;
import cc.bliss.match3.service.gamemanager.db.specification.ClanSpecification;
import cc.bliss.match3.service.gamemanager.ent.common.SearchObj;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.common.UpdateMoneyResult;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ClanInfo;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ClanMember;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.SSEService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class ClanService extends BaseService {

    private final String DEFAULT_CHANNEL = "sampleChannel";
    @Autowired
    private AdminService adminService;
    @Autowired
    private ProfileService profileService;

    public Map<Integer, ClanInfo> getListClan(Collection<Integer> listID) {
        List<ClanInfo> clanInfos = clanRepository.read().findAllById(listID);
        for (ClanInfo clanInfo : clanInfos) {
            int size = clanMemberRepository.read().countByClanIDAndStateIn(clanInfo.getId(), buildListStateMember());
            clanInfo.setSize(size);
        }
        return clanInfos.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
    }

    private List<Integer> buildListStateMember() {
        List<Integer> list = new ArrayList<>();
        list.add(EClanState.MEMBER.ordinal());
        list.add(EClanState.CO_LEADER.ordinal());
        list.add(EClanState.LEADER.ordinal());
        return list;
    }

    public String createClan(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        String name = jsonObject.get("name").getAsString();
        int badge = jsonObject.get("badge").getAsInt();
        String desc = jsonObject.get("desc").getAsString();
        int type = jsonObject.get("type").getAsInt();
        int trophyRequire = jsonObject.get("trophyRequire").getAsInt();

        if (name.length() > GameConstant.MAX_NAME_LENGTH) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Tên quá dài !", NetWorkAPI.CLAN_JOIN);
        }

        UpdateMoneyResult moneyResult = profileService.updateMoney(session.getId(), GameConstant.CLAN_CREATE_MONEY, EUpdateMoneyType.CLAN);
        if (moneyResult.getEUpdateMoneyResult().equals(EUpdateMoneyResult.FAIL)) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Không đủ tiền !", NetWorkAPI.CLAN_JOIN);
        }

        ClanInfo clanInfo = new ClanInfo();
        clanInfo.setName(name);
        clanInfo.setTag("#" + StringUtils.normalize(name.toLowerCase()));
        clanInfo.setDescription(desc);
        clanInfo.setBadge(badge);
        clanInfo.setType(type);
        clanInfo.setTrophyRequire(trophyRequire);
        clanInfo.setCreatedBy(session.getId());
        insertMatch3SchemaData(clanInfo);

        ClanMember clanMember = new ClanMember();
        clanMember.setClanID(clanInfo.getId());
        clanMember.setState(EClanState.LEADER.ordinal());
        clanMember.setUserID(session.getId());
        insertMatch3SchemaData(clanMember);

        return getDetail(clanInfo.getId());
    }

    public String searchClan(SearchObj searchObj) {
        SessionObj session = adminService.getSession();
        Specification<ClanInfo> spec = ClanSpecification.withName(searchObj.getSearchString());
        Pageable pageable = PageRequest.of(searchObj.getPage(), GameConstant.GAME_LOG_PAGE_SIZE, Sort.by("createdTime").descending());
        Page<ClanInfo> page = clanRepository.read().findAll(spec, pageable);
        JsonArray jsonArray = new JsonArray();
        for (ClanInfo clanInfo : page.getContent()) {
            Optional<ClanMember> optional = clanMemberRepository.read().findByUserIDAndClanID(session.getId(), clanInfo.getId());
            int size = clanMemberRepository.read().countByClanIDAndStateIn(clanInfo.getId(), buildListStateMember());
            int role = optional.isPresent() ? optional.get().getState() : EClanState.NONE.ordinal();
            JsonObject jsonObject = clanToJson(clanInfo, size, role);
            jsonArray.add(jsonObject);
        }
        JsonObject resp = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), page.getTotalPages(), page.getNumber(), jsonArray.size());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_GET_LIST);
    }

    public String getListClan(SearchObj searchObj) {
        SessionObj session = adminService.getSession();
        Pageable pageable = PageRequest.of(searchObj.getPage(), GameConstant.GAME_LOG_PAGE_SIZE, Sort.by("createdTime").descending());
        Page<ClanInfo> page = clanRepository.read().findAll(pageable);
        JsonArray jsonArray = new JsonArray();
        for (ClanInfo clanInfo : page.getContent()) {
            Optional<ClanMember> optional = clanMemberRepository.read().findByUserIDAndClanID(session.getId(), clanInfo.getId());
            int size = clanMemberRepository.read().countByClanIDAndStateIn(clanInfo.getId(), buildListStateMember());
            int role = optional.isPresent() ? optional.get().getState() : EClanState.NONE.ordinal();
            JsonObject jsonObject = clanToJson(clanInfo, size, role);
            jsonArray.add(jsonObject);
        }
        JsonObject resp = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), page.getTotalPages(), page.getNumber(), jsonArray.size());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_GET_LIST);
    }

    public String joinClan(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        if (!jsonObject.has("clanID")) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), NetWorkAPI.CLAN_JOIN);
        }
        int clanID = jsonObject.get("clanID").getAsInt();
        String requestMsg = jsonObject.get("requestMsg").getAsString();
        ClanInfo clanInfo = clanRepository.read().getById(clanID);
        SessionObj session = adminService.getSession();
        if (clanMemberRepository.read().findByUserID(session.getId()).isPresent()) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), NetWorkAPI.CLAN_JOIN);
        }
        ClanMember clanMember = new ClanMember();
        clanMember.setClanID(clanID);
        clanMember.setUserID(session.getId());
        JsonObject resp = new JsonObject();
        if (clanInfo.getType() == EClanType.OPEN.ordinal()) {
            clanMember.setState(EClanState.MEMBER.ordinal());
            resp.addProperty("joinClanStatus", 0);
        } else if (clanInfo.getType() == EClanType.PRIVATE.ordinal()) {
            clanMember.setState(EClanState.REQUEST.ordinal());
            clanMember.setRequestMsg(requestMsg);
            resp.addProperty("joinClanStatus", 1);
        }
        insertMatch3SchemaData(clanMember);
        resp.addProperty("clanID", clanID);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_JOIN);
    }

    public String leaveClan() {
        SessionObj session = adminService.getSession();
        Optional<ClanMember> optional = clanMemberRepository.read().findByUserID(session.getId());
        if (optional.isPresent()) {
            int clanID = optional.get().getClanID();
            clanMemberRepository.write().delete(optional.get());
            if (clanMemberRepository.read().countByClanIDAndStateIn(clanID, buildListStateMember()) == 0) {
                clanMemberRepository.write().deleteByClanID(clanID);
                clanRepository.write().deleteById(clanID);
            }
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.CLAN_JOIN);
        }
        return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), NetWorkAPI.CLAN_JOIN);
    }

    public String getDetail(int id) {
        SessionObj session = adminService.getSession();
        int sessionRole = 0;
        Optional<ClanInfo> optional = clanRepository.read().findById(id);
        if (optional.isPresent()) {
            ClanInfo clanInfo = optional.get();

            List<ClanMember> clanMembers = clanMemberRepository.read().findByClanIDAndStateIn(id, buildListStateMember());
            int totalTrophy = 0;
            JsonArray jsonArray = new JsonArray();
            for (ClanMember clanMember : clanMembers) {
                Profile profile = profileService.getProfileByID(clanMember.getUserID());
                totalTrophy += profile.getTrophy();
                JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy());
                data.addProperty("clanID", id);
                data.addProperty("clanRole", clanMember.getState());
                if (session != null && session.getId() == clanMember.getUserID()) {
                    sessionRole = clanMember.getState();
                }
                jsonArray.add(data);
            }
            JsonObject resp = clanToJson(clanInfo, jsonArray.size(), sessionRole);
            resp.add("member", jsonArray);
            resp.addProperty("totalTrophy", totalTrophy);
            resp.addProperty("weeklyDonate", 0);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_GET_LIST);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.name(), NetWorkAPI.CLAN_DETAIL);
    }

    public String getClanRequest(int id) {
        Optional<ClanInfo> optional = clanRepository.read().findById(id);
        if (optional.isPresent()) {
            List<ClanMember> clanMembers = clanMemberRepository.read().findByClanIDAndState(id, EClanState.REQUEST.ordinal());
            JsonArray jsonArray = new JsonArray();
            for (ClanMember clanMember : clanMembers) {
                Profile profile = profileService.getProfileByID(clanMember.getUserID());
                JsonObject data = JsonBuilder.profileToJson(profile);
                data.addProperty("clanID", id);
                data.addProperty("clanRole", clanMember.getState());
                data.addProperty("requestMsg", clanMember.getRequestMsg());
                jsonArray.add(data);
            }
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonArray, NetWorkAPI.CLAN_GET_LIST);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.name(), NetWorkAPI.CLAN_DETAIL);
    }

    public String approveRequest(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int requestID = jsonObject.get("requestID").getAsInt();
        int clanID = jsonObject.get("clanID").getAsInt();
        int approveType = jsonObject.get("approveType").getAsInt();
        Optional<ClanMember> optional = clanMemberRepository.read().findByUserID(requestID);
        if (!optional.isPresent()) {
            return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.name(), NetWorkAPI.CLAN_DETAIL);
        }
        ClanMember clanMember = optional.get();
        if (approveType == EClanApproveType.APPROVE.ordinal()) {
            clanMember.setState(EClanState.MEMBER.ordinal());
            clanMemberRepository.write().updateData(clanMember.getId(),clanMember.getClanID(),clanMember.getState(),clanMember.getRequestMsg(),clanMember.getUserID());
        } else if (approveType == EClanApproveType.REJECT.ordinal()) {
            clanMemberRepository.write().delete(clanMember);
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("clanID", clanID);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_JOIN);
    }

    public String kich(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        ClanMember role = clanMemberRepository.read().findByUserID(session.getId()).get();
        if (role.getState() != EClanState.LEADER.ordinal() && role.getState() != EClanState.CO_LEADER.ordinal()) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Không có quyền kich user !!!", NetWorkAPI.CLAN_DETAIL);
        }
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int requestID = jsonObject.get("requestID").getAsInt();
        int clanID = jsonObject.get("clanID").getAsInt();
        String kichMsg = jsonObject.get("kichMsg").getAsString();
        Optional<ClanMember> optional = clanMemberRepository.read().findByUserID(requestID);
        if (!optional.isPresent()) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Không tìm thấy user !!!", NetWorkAPI.CLAN_DETAIL);
        }
        ClanMember clanMember = optional.get();
        clanMemberRepository.write().delete(clanMember);
        JsonObject resp = new JsonObject();
        resp.addProperty("clanID", clanID);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), resp, NetWorkAPI.CLAN_JOIN);
    }

    public JsonObject clanToJson(ClanInfo clanInfo) {
        JsonObject resp = new JsonObject();
        resp.addProperty("id", clanInfo.getId());
        resp.addProperty("name", clanInfo.getName());
        resp.addProperty("desc", clanInfo.getDescription());
        resp.addProperty("tag", clanInfo.getTag());
        resp.addProperty("badge", clanInfo.getBadge());
        resp.addProperty("maxMemberSize", 50);
        resp.addProperty("trophyRequire", clanInfo.getTrophyRequire());
        resp.addProperty("requestStatus", 0);
        resp.addProperty("type", clanInfo.getType());
        resp.addProperty("chatChannel", "sampleChannel");

        return resp;
    }

    public JsonObject clanToJson(ClanInfo clanInfo, int size, int role) {
        JsonObject resp = clanToJson(clanInfo);
        resp.addProperty("memberSize", size);
        resp.addProperty("clanRole", role);

        return resp;
    }

    public String sendChat(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        String channelID;
        if (request.getParameter("chatChannel") != null) {
            channelID = request.getParameter("chatChannel");
        } else {
            channelID = DEFAULT_CHANNEL;
        }
        SessionObj session = adminService.getSession();
        jsonObject.addProperty("userID", session.getId());
        String msg = toChatMsg(jsonObject.toString());
        sseService.emitNextMsg("[" + msg + "]", channelID);
        redisSaveChat(channelID, msg);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.CLAN_SEND_CHAT);
    }

    private String toChatMsg(String data) {
        JsonObject jsonObject = JSONUtil.DeSerialize(data, JsonObject.class);
        long userID = jsonObject.get("userID").getAsLong();
        Profile profile = profileService.getMinProfileByID(userID);

        jsonObject.addProperty("username", profile.getUsername());
        jsonObject.addProperty("avatar", profile.getAvatarPath());
        jsonObject.addProperty("createdTime", System.currentTimeMillis());
        return jsonObject.toString();
    }

    public String getChatHistory(HttpServletRequest request) {
        JsonObject req = RequestUtils.requestToJson(request);
        String channelID;
        if (req.has("chatChannel")) {
            channelID = req.get("chatChannel").getAsString();
        } else {
            channelID = DEFAULT_CHANNEL;
        }
        JsonArray historyString = new JsonArray();
        List<String> history = redisGetListChat(channelID);
        for (String string : history) {
            JsonObject jsonObject = JSONUtil.DeSerialize(string, JsonObject.class);
            historyString.add(jsonObject);
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), historyString, NetWorkAPI.CLAN_CHAT_HISTORY);
    }

    private void redisSaveChat(String key, String value) {
        redisTemplateString.opsForList().leftPush(key, value);
        redisTemplateString.expire(key, Duration.ofDays(7));
        // delete old msg
        if (redisTemplateString.opsForList().size(key) > ModuleConfig.MESSAGE_LIMIT) {
            redisTemplateString.opsForList().leftPop(key);
        }
    }

    private List<String> redisGetListChat(String key) {
        return redisTemplateString.opsForList().range(key, 0, -1);
    }

}
