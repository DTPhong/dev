/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.system;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.NetworkUtils;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.Bootstrap;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.InventoryRepository;
import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.db.specification.UserSpecification;
import cc.bliss.match3.service.gamemanager.ent.common.SearchObj;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.common.Statistic;
import cc.bliss.match3.service.gamemanager.ent.common.UpdateMoneyResult;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import cc.bliss.match3.service.gamemanager.ent.data.UserPropertyChangeEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.*;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.LoginCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.common.ProfileStatisticService;
import cc.bliss.match3.service.gamemanager.service.event.*;
import cc.bliss.match3.service.gamemanager.util.*;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class ProfileService extends BaseService {

    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private ProfileStatisticService profileStatisticService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private GachaService gachaService;
    @Autowired
    private TrophyRoadService trophyRoadService;
    @Autowired
    private DailyRewardService dailyRewardService;
    @Autowired
    private SSEService sSEService;
    @Autowired
    QuestEventService questEventService;
    @Autowired
    HeroService heroService;
    @Autowired
    private Login7dQuestService login7dQuestService;
    @Autowired
    private DailyQuestService dailyQuestService;

    private static final List<String> DEFAULT_AVATARS = Arrays.asList(
            "https://dlc.match3arena.com/profile_ava_01.png",
            "https://dlc.match3arena.com/profile_ava_02.png",
            "https://dlc.match3arena.com/profile_ava_03.png",
            "https://dlc.match3arena.com/profile_ava_04.png",
            "https://dlc.match3arena.com/profile_ava_05.png"
    );

    private static final List<Integer> DEFAULT_FRAMES = Arrays.asList(0,1,2,3,4,5,6,7,8,9,10);
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ProfileRepository profileRepository;


    public List<Profile> getByListId(Collection<Long> listID) {
        return profileRepository.read().findAllById(listID);
    }

    public Map<Long, Profile> getMapByListId(Collection<Long> listID) {
        List<Profile> profiles = getByListId(listID);
        return profiles.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
    }

    public String getVersion(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        String clientVersion = jsonObject.get("clientVersion").getAsString();
        int clientOS = jsonObject.get("os").getAsInt();
        long userID = jsonObject.has("userID") ? jsonObject.get("userID").getAsLong() : 0;
        Optional<Version> optional = versionRepository.read().findById(clientVersion);
        if (optional.isPresent()){
            if (jsonObject.has("userID")){
                profileRepository.write().updateVersion(userID, clientVersion);
            }
            Version version = optional.get();

            jsonObject.addProperty("forceUpdate", version.getForceUpdate() == 1);
            jsonObject.addProperty("forceVersion", version.getForceUpdateVersion());
            jsonObject.addProperty("forceUrl", version.getForceUpdateUrl());
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.UNKNOWN);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), "Invalid parameter", NetWorkAPI.UNKNOWN);
    }

    public String login(HttpServletRequest request) throws IOException {
        // check maintain status
        if (TicketService.IS_MAINTAIN) {
            return ResponseUtils.toResponseBody(HttpStatus.SERVICE_UNAVAILABLE.value(), "Server maintain !", NetWorkAPI.LOGIN);
        }
        String ip = NetworkUtils.getClientIP(request);
        // check open close test
        if (System.currentTimeMillis() < ModuleConfig.CLOSE_TEST_START_TIME.getTime() && !ModuleConfig.WHITELIST_IP.contains(ip)){
            return ResponseUtils.toResponseBody(HttpStatus.SERVICE_UNAVAILABLE.value(), "Server maintain !", NetWorkAPI.LOGIN);
        }
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        Profile profile = null;
        // init variable
        int profileTrophy = 0;
        int deleteAccountStatus = 0;
        long deleteTime = 0;
        Date currentTimeStamp = new Date();
        String deviceID = jsonObject.has("token") ? jsonObject.get("token").getAsString() : "";
        String socialID = jsonObject.has("socialID") ? jsonObject.get("socialID").getAsString() : "";
        String version = jsonObject.has("version") ? jsonObject.get("version").getAsString() : "";
        // validate client input
        if (deviceID.isEmpty() && socialID.isEmpty()) {
            return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.LOGIN);
        }
        // logic
        DeviceMappingID deviceMappingID = new DeviceMappingID(deviceID, socialID);
        Optional<DeviceMappingEnt> optional = deviceMappingRepository.read().findById(deviceMappingID);
        long userID;
        int isNew = 0;
        if (optional.isPresent()) {
            // find profile by deviceID and socialID
            userID = optional.get().getUserID();
            profile = profileRepository.read().findById(userID).get();
        } else {
            deviceMappingID = new DeviceMappingID(deviceID, "");
            optional = deviceMappingRepository.read().findById(deviceMappingID);
            if (optional.isPresent()) {
                // find profile by deviceID
                userID = optional.get().getUserID();
                profile = profileRepository.read().findById(userID).get();
            } else {
                // create new profile
                isNew = 1;
                profile = new Profile();
                String randomName = randomIdentifier();
                profile.setUsername(randomName);
                profile.setLastLogin(currentTimeStamp);
                profile.setDeviceID(deviceID);
                if (jsonObject.has("timeZone")) {
                    profile.setTimeZone(jsonObject.get("timeZone").getAsFloat());
                }
                if (ModuleConfig.IS_TEST && profile.getDeviceID().endsWith("-stress-test")){
                    profile.setBattleWon(4);
                    profile.setLoseStreak(1);
                }
                profile.addAvatar(DEFAULT_AVATARS);
                profile.addFrame(DEFAULT_FRAMES);
                profile.setIsNew(isNew);
                // insert data
                insertMatch3SchemaData(profile);
            }
        }

        if (isNew == 1){
            // mapping key login
            DeviceMappingEnt deviceMappingEnt = new DeviceMappingEnt();
            deviceMappingEnt.setDeviceMappingID(deviceMappingID);
            deviceMappingEnt.setUserID(profile.getId());
            insertMatch3SchemaData(deviceMappingEnt);
        } else {
            profileTrophy = leaderboardService.getProfileTrophy(profile.getId());
            Optional<RequestDeleteAccount> optionalRequestDeleteAccount = requestDeleteAccountRepository.read().findById(profile.getId());
            if (optionalRequestDeleteAccount.isPresent()){
                deleteAccountStatus = 1;
                deleteTime = optionalRequestDeleteAccount.get().getDeleteTime().getTime();
            }
            profile.setLastLogin(currentTimeStamp);
            profile.setDeviceID(deviceID);
            version = version.isEmpty() ? profile.getVersion() : version;
            profileRepository.write().updateLoginField(profile.getId(), currentTimeStamp, deviceID, version, isNew);
        }
        String jwt = jwtUtils.generateJwtTokenForLogin(profile.getId(), currentTimeStamp);
        JsonObject profileJson = JsonBuilder.profileToJson(profile, profileTrophy);
        JsonObject hero = heroService.getHeroJson(profile.getId(), profile.getSelectHero());
        JsonObject response = JsonBuilder.buildLoginResponse(profileJson,hero,jwt,deleteAccountStatus,deleteTime);

        GMLocalQueue.addQueue(new LoginCmd(dailyRewardService, questEventService, login7dQuestService, profile, producer, "GUEST", profileTrophy, redisTemplateString));
        buildUserTracking(ip, profile.getId(), jwtUtils.getTokenExpireTime(jwt));
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.LOGIN);
    }

    /**
     * Only fetch data on database
     *
     * @param id
     * @return
     */
    public Profile getMinProfileByID(long id) {
        return profileRepository.read().findById(id).get();
    }

    /**
     * With trophy data
     *
     * @param id
     * @return
     */
    public Profile getProfileByID(long id) {
        Profile profile = profileRepository.read().findById(id).get();
        Optional<ClanMember> clanMemberOptional = clanMemberRepository.read().findByUserID(id);
        if (clanMemberOptional.isPresent() && (clanMemberOptional.get().getState() == EClanState.MEMBER.ordinal()
                || clanMemberOptional.get().getState() == EClanState.LEADER.ordinal()
                || clanMemberOptional.get().getState() == EClanState.CO_LEADER.ordinal())) {
            profile.setClanID(clanMemberOptional.get().getClanID());
        }
        int profileTrophy = leaderboardService.getProfileTrophy(profile.getId());
        profile.setTrophy(profileTrophy);
        List<HeroEnt> heroEnts = inventoryService.getListOwnedHero(id);
        profile.setTotalHeroes(heroEnts.size());
        return profile;
    }

    public String getByID(long id) {
        Optional<Profile> optional = profileRepository.read().findById(id);
        if (optional.isPresent()) {
            Profile profile = getProfileByID(id);
            Map<String, Statistic> mapStatistic = profileStatisticService.getStatisticData(id);
            int totalHero = ConvertUtils.toInt(heroRepository.read().count());
            int ownedHero = inventoryService.countOwnedHero(id);
            JsonObject data = JsonBuilder.profileToJson(profile, profile.getTrophy(), mapStatistic, totalHero, ownedHero);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), data, NetWorkAPI.PROFILE_BY_ID);
        }
        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.PROFILE_BY_ID);
    }

    public String get(SearchObj searchObj) {
        Specification<Profile> spec = UserSpecification.withUserID(searchObj.getPlayerID())
                .and(UserSpecification.withUsername(searchObj.getUsername()));
        Pageable pageable = PageRequest.of(searchObj.getPage(), searchObj.getLimit(), Sort.by("createdTime").descending());

        Page<Profile> page = profileRepository.read().findAll(spec, pageable);
        JsonObject jsonObject = JsonBuilder.toSearchResponseData(page);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.PROFILE_SEARCH);
    }

    public String editProfile(Profile changedData) {
        SessionObj session = adminService.getSession();
        Optional<Profile> optional = profileRepository.read().findById(session.getId());
        if (optional.isPresent()) {
            Profile profile = optional.get();
            int profileTrophy = leaderboardService.getProfileTrophy(profile.getId());
            if (StringUtils.isNotEmpty(changedData.getUsername())) {
                if (changedData.getUsername().length() > 15 || changedData.getUsername().length() < 2) {
                    JsonObject data = JsonBuilder.editProfileToJson(profile, profileTrophy, EEditProfileError.INVALID_USERNAME.ordinal());
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), data, NetWorkAPI.PROFILE_EDIT);
                }
                if (!isValidName(changedData.getUsername().toLowerCase())) {
                    JsonObject data = JsonBuilder.editProfileToJson(profile, profileTrophy, EEditProfileError.INVALID_USERNAME.ordinal());
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), data, NetWorkAPI.PROFILE_EDIT);
                }
                if (!profile.getUsername().contentEquals(changedData.getUsername()) && profileRepository.read().existsByUsername(changedData.getUsername())){
                    JsonObject data = JsonBuilder.editProfileToJson(profile, profileTrophy, EEditProfileError.DUPLICATE_USERNAME.ordinal());
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), data, NetWorkAPI.PROFILE_EDIT);
                }
                profile.setUsername(changedData.getUsername());
                profileRepository.write().updateUsername(profile.getId(), profile.getUsername());
            }

            if (changedData.getFrame() != -1) {
                profile.setFrame(changedData.getFrame());
                profileRepository.write().updateFrame(profile.getId(), profile.getFrame());
            }
            if (StringUtils.isNotEmpty(changedData.getAvaId())) {
                String avaPath = profile.getAvaPath(changedData.getAvaId());
                profile.setAvatarPath(avaPath.replace("\"", ""));
                profileRepository.write().updateAvatarPath(profile.getId(), profile.getAvatarPath());
            }

            JsonObject data = JsonBuilder.editProfileToJson(profile, profileTrophy, EEditProfileError.NONE.ordinal());
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), data, NetWorkAPI.PROFILE_EDIT);
        }
        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.PROFILE_EDIT);
    }

    public String editTutorial(Profile changedData) {
        SessionObj session = adminService.getSession();
        Optional<Profile> optional = profileRepository.read().findById(session.getId());
        if (optional.isPresent()) {
            Profile profile = optional.get();
            int before = profile.getTutorial();
            if (changedData.getTutorial() > profile.getTutorial()){
                profile.setTutorial(changedData.getTutorial());

                profileRepository.write().updateTutorialByProfileId(profile.getId(), profile.getTutorial());
            }
            Profile afterProfile = profileRepository.read().findById(session.getId()).get();
            UserPropertyChangeEnt userPropertyChangeEnt = UserPropertyChangeEnt.buildUserPropertyChangeEnt(
                    profile, "EDIT_TUTORIAL", -1, changedData.getTutorial(),
                    changedData.getTutorial(), afterProfile.getTutorial(), "EDIT_TUTORIAL"
            );
            producer.sendPropertyChangeMessage(userPropertyChangeEnt);
            JsonObject data = JsonBuilder.profileToJson(profile);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), data, NetWorkAPI.PROFILE_EDIT);
        }
        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.PROFILE_EDIT);
    }


    private boolean isValidName(String username) {
        String pattern = "^([a-zxyỳọáầảấờễàạằệếýộậốũứĩõúữịỗìềểẩớặòùồợãụủíỹắẫựỉỏừỷởóéửỵẳẹèẽổẵẻỡơôưăêâđ 0-9]+[a-zxyỳọáầảấờễàạằệếýộậốũứĩõúữịỗìềểẩớặòùồợãụủíỹắẫựỉỏừỷởóéửỵẳẹèẽổẵẻỡơôưăêâđ 0-9]+)$";
        return username.matches(pattern);
    }

    public UpdateMoneyResult updateMoney(long id, long delta, EUpdateMoneyType eUpdateMoneyType) {
        UpdateMoneyResult moneyResult = new UpdateMoneyResult();
        try {
            Optional<Profile> optional = profileRepository.read().findById(id);
            if (!optional.isPresent()){
                // Not exist userID
                moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
                return moneyResult;
            }
            Profile profile = optional.get();
            long before = profile.getMoney();
            if (profile.getMoney() + delta < 0) {
                // prevent minus money
                delta = -profile.getMoney();
            }
            profile.setMoney(profile.getMoney() + delta);
            profileRepository.write().updateGoldByProfileId(profile.getId(), delta);
            long after = profile.getMoney();
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.SUCCESS);
            moneyResult.setBefore(before);
            moneyResult.setDelta(delta);
            moneyResult.setAfter(after);

            // log user money
            UserPropertyChangeEnt userPropertyChangeEnt = UserPropertyChangeEnt.buildUserPropertyChangeEnt(
                    profile, ERewardType.GOLD.name(), -1, delta,
                    before, after, eUpdateMoneyType.name()
            );
            producer.sendPropertyChangeMessage(userPropertyChangeEnt);
            sSEService.emitNextMsg(JsonBuilder.buildSSEUpdateMoney(delta, before, after, ERewardType.GOLD.getValue()), id);
            GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, profile.getId(), EQuestType.COLLECT_GOLD, delta));
        } catch (Exception ex){
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, ProfileService.class));
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
        }
        return moneyResult;
    }

    public UpdateMoneyResult updateEmerald(long id, long delta, EUpdateMoneyType eUpdateMoneyType) {
        UpdateMoneyResult moneyResult = new UpdateMoneyResult();
        try {
            Optional<Profile> optional = profileRepository.read().findById(id);
            if (!optional.isPresent()){
                // Not exist userID
                moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
                return moneyResult;
            }
            Profile profile = optional.get();
            long before = profile.getEmerald();
            if (profile.getEmerald() + delta < 0) {
                // prevent minus money
                delta = -profile.getEmerald();
            }
            profile.setEmerald(profile.getEmerald() + delta);
            profileRepository.write().updateEmeraldByProfileId(profile.getId(), delta);
            long after = profile.getEmerald();
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.SUCCESS);
            moneyResult.setBefore(before);
            moneyResult.setDelta(delta);
            moneyResult.setAfter(after);

            // log user money
            UserPropertyChangeEnt userPropertyChangeEnt = UserPropertyChangeEnt.buildUserPropertyChangeEnt(
                    profile, ERewardType.EMERALD.name(), -1, delta,
                    before, after, eUpdateMoneyType.name()
            );
            producer.sendPropertyChangeMessage(userPropertyChangeEnt);
            sSEService.emitNextMsg(JsonBuilder.buildSSEUpdateMoney(delta, before, after, ERewardType.EMERALD.getValue()), id);
        } catch (Exception ex){
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, ProfileService.class));
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
        }
        return moneyResult;
    }

    public UpdateMoneyResult updateAmethyst(long id, long delta, EUpdateMoneyType eUpdateMoneyType) {
        UpdateMoneyResult moneyResult = new UpdateMoneyResult();
        try {
            Optional<Profile> optional = profileRepository.read().findById(id);
            if (!optional.isPresent()){
                // Not exist userID
                moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
                return moneyResult;
            }
            Profile profile = optional.get();
            long before = profile.getAmethyst();
            if (profile.getAmethyst() + delta < 0) {
                // prevent minus money
                delta = -profile.getAmethyst();
            }
            profile.setAmethyst(profile.getAmethyst() + delta);
            profileRepository.write().updateAmethystByProfileId(profile.getId(), delta);
            long after = profile.getAmethyst();
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.SUCCESS);
            moneyResult.setBefore(before);
            moneyResult.setDelta(delta);
            moneyResult.setAfter(after);

            // log user money
            UserPropertyChangeEnt userPropertyChangeEnt = UserPropertyChangeEnt.buildUserPropertyChangeEnt(
                    profile, ERewardType.AMETHYST.name(), -1, delta,
                    before, after, eUpdateMoneyType.name()
            );
            producer.sendPropertyChangeMessage(userPropertyChangeEnt);
            sSEService.emitNextMsg(JsonBuilder.buildSSEUpdateMoney(delta, before, after, ERewardType.AMETHYST.getValue()), id);
        } catch (Exception ex){
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, ProfileService.class));
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
        }
        return moneyResult;
    }

    public UpdateMoneyResult updateRoyalAmethyst(long id, long delta, EUpdateMoneyType eUpdateMoneyType) {
        UpdateMoneyResult moneyResult = new UpdateMoneyResult();
        try {
            Optional<Profile> optional = profileRepository.read().findById(id);
            if (!optional.isPresent()){
                // Not exist userID
                moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
                return moneyResult;
            }
            Profile profile = optional.get();
            long before = profile.getRoyalAmethyst();
            if (profile.getRoyalAmethyst() + delta < 0) {
                // prevent minus money
                delta = -profile.getRoyalAmethyst();
            }
            profile.setRoyalAmethyst(profile.getRoyalAmethyst() + delta);
            profileRepository.write().updateGoldByProfileId(profile.getId(), delta);
            long after = profile.getRoyalAmethyst();
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.SUCCESS);
            moneyResult.setBefore(before);
            moneyResult.setDelta(delta);
            moneyResult.setAfter(after);

            // log user money
            UserPropertyChangeEnt userPropertyChangeEnt = UserPropertyChangeEnt.buildUserPropertyChangeEnt(
                    profile, ERewardType.ROYAL_AMETHYST.name(), -1, delta,
                    before, after, eUpdateMoneyType.name()
            );
            producer.sendPropertyChangeMessage(userPropertyChangeEnt);
            sSEService.emitNextMsg(JsonBuilder.buildSSEUpdateMoney(delta, before, after, ERewardType.ROYAL_AMETHYST.getValue()), id);
        } catch (Exception ex){
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, ProfileService.class));
            moneyResult.setEUpdateMoneyResult(EUpdateMoneyResult.FAIL);
        }
        return moneyResult;
    }

    public String linkAccount(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int socialType = jsonObject.get("socialType").getAsInt();
        SessionObj sessionObj = adminService.getSession();
        long userID = jsonObject.get("userID").getAsLong();
        String token = jsonObject.has("token") ? jsonObject.get("token").getAsString() : "";
        if (token.isEmpty()) {
            return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
        }
        switch (socialType) {
            case 0: {
                // gg
                JsonObject googleData = GoogleUtils.fetchGoogleAccount2(token);
                String socialID = googleData.has("id") ? googleData.get("id").getAsString() : "";
                if (socialID.isEmpty()) {
                    return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
                }
                Optional<Profile> optional = profileRepository.read().findByGoogleId(socialID);
                if (!optional.isPresent()) {
                    Profile profile = profileRepository.read().getById(userID);
                    profile.setGoogleId(socialID);
                    profile.setGoogleName(ConvertUtils.toString(googleData.get("name").getAsString()));
                    profile.setGoogleAvatar(ConvertUtils.toString(googleData.get("picture").getAsString()));
                    profile.setAvatarPath(ConvertUtils.toString(googleData.get("picture").getAsString()));
                    profileRepository.write().updateGoogleLink(profile.getId(),
                            profile.getGoogleId(),
                            profile.getGoogleName(),
                            profile.getGoogleAvatar(),
                            profile.getGmail(),
                            profile.getGoogleAvatar());

                    // clear deviceID key
                    deviceMappingRepository.write().deleteById(
                            DeviceMappingID.builder().deviceID(profile.getDeviceID()).socialID("").build()
                    );
                    // add deviceID, socialID
                    DeviceMappingEnt deviceMappingEnt = new DeviceMappingEnt();
                    deviceMappingEnt.setDeviceMappingID(
                            DeviceMappingID.builder().deviceID(profile.getDeviceID()).socialID(socialID).build()
                    );
                    deviceMappingEnt.setUserID(profile.getId());
                    deviceMappingRepository.write().saveAndFlush(deviceMappingEnt);

                    JsonObject response = JsonBuilder.buildLinkSocialResponse(ELinkSocialStatus.SUCCESS, profile);
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UNKNOWN);
                } else {
                    Profile profile = profileRepository.read().getById(userID);
                    if (profile.getId() == optional.get().getId()) {
                        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
                    }
                    JsonObject response = JsonBuilder.buildLinkSocialResponse(ELinkSocialStatus.MERGE, profile, optional.get());
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UNKNOWN);
                }
            }
            case 2: {
                // apple
                JsonObject appleData = AppleUtils.fetchData(token);
                String socialID = appleData.has("id") ? appleData.get("id").getAsString() : "";
                if (socialID.isEmpty()) {
                    return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
                }
                Optional<Profile> optional = profileRepository.read().findByAppleID(socialID);
                if (!optional.isPresent()) {
                    Profile profile = profileRepository.read().getById(sessionObj.getId());
                    profile.setAppleID(socialID);
                    profile.setAppleName(ConvertUtils.toString(appleData.get("name").getAsString()));
                    profile.setAppleAvatar(ConvertUtils.toString(appleData.get("picture").getAsString()));
                    profileRepository.write().updateAppleLink(profile.getId(),
                            profile.getAppleID(),
                            profile.getAppleName(),
                            profile.getAppleAvatar(),
                            profile.getAppleEmail(),
                            profile.getAppleAvatar());

                    JsonObject response = JsonBuilder.buildLinkSocialResponse(ELinkSocialStatus.SUCCESS, profile);
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UNKNOWN);
                } else {
                    Profile profile = profileRepository.read().getById(sessionObj.getId());
                    if (profile.getId() == optional.get().getId()) {
                        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
                    }
                    JsonObject response = JsonBuilder.buildLinkSocialResponse(ELinkSocialStatus.MERGE, profile, optional.get());
                    return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UNKNOWN);
                }
            }
        }
        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
    }

    public String mergeAccount(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        long selectUserID = jsonObject.get("selectUserID").getAsLong();
        long removeUserID = jsonObject.get("removeUserID").getAsLong();
        if (selectUserID == removeUserID) {
            return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
        }
        Profile selectUser = profileRepository.read().getById(selectUserID);
        Profile removeUser = profileRepository.read().getById(removeUserID);
        boolean isChooseCloudAccount = selectUser.getId() != sessionObj.getId();
        if (isChooseCloudAccount){
            // choose cloud account
            String socialID = org.apache.commons.lang.StringUtils.isNotBlank(selectUser.getGoogleId())
                    ? selectUser.getGoogleId()
                    : selectUser.getAppleID();
            if (socialID == null || socialID.isEmpty()){
                return ResponseUtils.toErrorBody("Error on merge account", NetWorkAPI.UNKNOWN);
            }
            String localDeviceID = removeUser.getDeviceID();
                // add mapping key
            DeviceMappingEnt deviceMappingEnt = new DeviceMappingEnt();
            deviceMappingEnt.setDeviceMappingID(
                    DeviceMappingID.builder().deviceID(localDeviceID).socialID(socialID).build()
            );
            deviceMappingEnt.setUserID(selectUser.getId());
            deviceMappingRepository.write().saveAndFlush(deviceMappingEnt);
        } else {
            // choose local account
                // update social data local account
            selectUser.setGoogleId(removeUser.getGoogleId());
            selectUser.setGoogleName(removeUser.getGoogleName());
            selectUser.setGoogleAvatar(removeUser.getGoogleAvatar());
            selectUser.setGmail(removeUser.getGmail());
            profileRepository.write().updateGoogleLink(selectUser.getId(),
                    selectUser.getGoogleId(),
                    selectUser.getGoogleName(),
                    selectUser.getGoogleAvatar(),
                    selectUser.getGmail(),
                    selectUser.getGoogleAvatar());

                // unlink cloud account data
            removeUser.setGoogleId("");
            removeUser.setGoogleName("");
            removeUser.setAvatarPath("");
            removeUser.setGmail("");
            profileRepository.write().updateGoogleLink(removeUser.getId(),
                    removeUser.getGoogleId(),
                    removeUser.getGoogleName(),
                    removeUser.getGoogleAvatar(),
                    removeUser.getGmail(),
                    removeUser.getGoogleAvatar());

                // clear all mapping key another device
            List<DeviceMappingEnt> deviceMappingEntList = deviceMappingRepository.read().findBySocialID(selectUser.getGoogleId());
            deviceMappingRepository.write().deleteAll(deviceMappingEntList);

                // add mapping key local acc
            {
                DeviceMappingEnt deviceMappingEnt = new DeviceMappingEnt();
                deviceMappingEnt.setDeviceMappingID(
                        DeviceMappingID.builder().deviceID(selectUser.getDeviceID()).socialID(selectUser.getGoogleId()).build()
                );
                deviceMappingEnt.setUserID(selectUser.getId());
                deviceMappingRepository.write().saveAndFlush(deviceMappingEnt);
            }
                // add mapping key cloud acc
            {
                DeviceMappingEnt deviceMappingEnt = new DeviceMappingEnt();
                deviceMappingEnt.setDeviceMappingID(
                        DeviceMappingID.builder().deviceID(removeUser.getDeviceID()).socialID("").build()
                );
                deviceMappingEnt.setUserID(removeUser.getId());
                deviceMappingRepository.write().saveAndFlush(deviceMappingEnt);
            }
        }
        String jwt = jwtUtils.generateJwtToken(selectUser.getId());
        JsonObject profileJson = JsonBuilder.profileToJson(selectUser, selectUser.getTrophy());
        JsonObject response = new JsonObject();
        response.addProperty("token", jwt);
        response.add("profile", profileJson);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.LOGIN);
    }

    public String unlinkAccount(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int socialType = jsonObject.get("socialType").getAsInt();
        SessionObj sessionObj = adminService.getSession();
        Profile profile = profileRepository.read().getById(sessionObj.getId());
        switch (socialType) {
            case 0: {
                // clear all mapping key another device
                List<DeviceMappingEnt> deviceMappingEntList = deviceMappingRepository.read().findBySocialID(profile.getGoogleId());
                System.out.println(deviceMappingEntList.size());
                deviceMappingEntList.forEach(e -> System.out.println(e.getDeviceMappingID().getDeviceID() + " " + e.getDeviceMappingID().getSocialID()));
                deviceMappingRepository.write().deleteAll(deviceMappingEntList);

                // unlink account data
                profile.setGmail("");
                profile.setGoogleId("");
                profile.setGoogleAvatar("");
                profile.setGoogleName("");
                profileRepository.write().updateGoogleLink(profile.getId(),
                        profile.getGoogleId(),
                        profile.getGoogleName(),
                        profile.getGoogleAvatar(),
                        profile.getGmail(),
                        "");

                // add mapping key
                DeviceMappingEnt deviceMappingEnt = new DeviceMappingEnt();
                deviceMappingEnt.setDeviceMappingID(
                        DeviceMappingID.builder().deviceID(profile.getDeviceID()).socialID("").build()
                );
                deviceMappingEnt.setUserID(profile.getId());
                deviceMappingRepository.write().saveAndFlush(deviceMappingEnt);

                JsonObject response = JsonBuilder.profileToJson(profile);
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UNKNOWN);
            }
            case 2: {
                // apple
                profile.setAppleAvatar("");
                profile.setAppleEmail("");
                profile.setAppleID("");
                profile.setAppleName("");

                JsonObject response = JsonBuilder.profileToJson(profile);
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UNKNOWN);
            }
        }
        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.UNKNOWN);
    }

    public String requestDeleteAccount(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        Optional<RequestDeleteAccount> optional = requestDeleteAccountRepository.read().findById(sessionObj.getId());
        if (optional.isPresent()) {
            return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.value(), NetWorkAPI.UNKNOWN);
        }
        RequestDeleteAccount requestDeleteAccount = new RequestDeleteAccount();
        requestDeleteAccount.setUserId(sessionObj.getId());
        requestDeleteAccount.setRequestTime(new Date());
        requestDeleteAccount.setDeleteTime(new Date(System.currentTimeMillis() + Duration.ofDays(7).toMillis()));
        insertMatch3SchemaData(requestDeleteAccount);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UNKNOWN);
    }

    public String restoreAccount(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        Optional<RequestDeleteAccount> optional = requestDeleteAccountRepository.read().findById(sessionObj.getId());
        if (optional.isPresent()) {
            requestDeleteAccountRepository.write().delete(optional.get());
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UNKNOWN);
        }
        return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.value(), NetWorkAPI.UNKNOWN);
    }

    public void checkDeleteAccount() {
        List<HeroEnt> listHero = heroRepository.read().findAll();
        List<RequestDeleteAccount> requestDeleteAccounts = requestDeleteAccountRepository.read().findAll();
        for (RequestDeleteAccount requestDeleteAccount : requestDeleteAccounts) {
            Profile profile = profileRepository.read().getById(requestDeleteAccount.getUserId());
            long deltaTime = System.currentTimeMillis() - profile.getLastLogin().getTime();
            long userId = requestDeleteAccount.getUserId();
            if (deltaTime > Duration.ofDays(7).toMillis()){
                // delete account
//                profile.setTutorial(1);
//                profile.setMoney(0);
//                profile.setEmerald(0);
//                profile.setRoyalAmethyst(0);
//                profile.setAmethyst(0);
//                profile.setWinStreak(0);
//                profile.setBattleWon(0);
//                profile.setSelectHero(1);
//                profile.setDominateWin(0);
//                profile.setGodLikeWin(0);
//                profile.setHighestTrophy(0);
//                profile.setHighestStreak(0);
//                profile.setFrame(0);
                profileRepository.write().updateDeleteAccount(profile.getId());

                if (inventoryRepository.read().existsById(userId)){
                    inventoryRepository.write().deleteById(userId);
                }
                if (profileStatisticRepository.read().existsById(userId)){
                    profileStatisticRepository.write().deleteById(userId);
                }
                if (requestDeleteAccountRepository.read().existsById(userId)){
                    requestDeleteAccountRepository.write().deleteById(userId);
                }
                redisTemplateString.delete(gachaService.getRedisKey(userId));
                mailRepository.write().deleteMailByUserID(userId);
                trophyRoadService.deleteTrophyRoad(userId);
                leaderboardService.deleteLeaderboard(listHero, userId);
                dailyRewardService.deleteRedisKey(userId);
                profileStatisticService.clearData(userId);

                requestDeleteAccountRepository.write().delete(requestDeleteAccount);
                GMLocalQueue.addQueue(new TelegramLoggerCmd("Delete account " + profile.getUsername(), TeleLogType.EXCEPTION, Bootstrap.class));
            }
        }
    }

    public boolean isFTUE(int battleWon, int loseStreak){
        return (battleWon+loseStreak) <= 4;
    }

    public boolean isFUTEquest(Profile profile){
        return profile.getTutorial() <= 54;
    }

    private void buildUserTracking(String ip, long userId, long tokenExpireTime) {
        try {
            UserDetect userDetect = new UserDetect();
            userDetect.setIp(ip);
            userDetect.setCountryCode("VN");
            String cacheUserInfo = ModuleConfig.GSON_BUILDER.toJson(userDetect);
            String sessionKey = String.format(GameConstant.CLIENT_INFO, userId);
            redisTemplateString.opsForValue().set(sessionKey, cacheUserInfo, tokenExpireTime, TimeUnit.MILLISECONDS);
        }catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.DEBUG, ProfileService.class));
        }
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

    public List<Profile> getBotProfileByHeroSelectedIn(List<Integer> heroSelected){
        return new ArrayList<>(profileRepository.read().findAllByBotTypeIsNotAndSelectHeroIn(0, heroSelected));
    }

}
