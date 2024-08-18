package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.ProfileStatisticRepository;
import cc.bliss.match3.service.gamemanager.ent.common.QuestDTO;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ProfileStatistic;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.QuestTrackingCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Login7dQuestService extends QuestEventService{
    @Autowired
    private AdminService adminService;

    @Autowired
    private TriggerService triggerService;

    @Autowired
    private ProfileService profileService;

    private static final int EVENT_TTL_DAYS = 10; // kể từ khi nhận event, user có 10 ngày để làm event

    private static final int TRIGGER_AFTER_TUTORIAL_STEP = 56;

    private static final int REDIS_TTL_DAYS = 14; // progress cache 14 Days

    private static final int NUM_REWARD_DAYS = 7;

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final int NUM_MINS_A_DAY = 24 * 60;
//    private static final int NUM_MINS_A_DAY = 1;

    @Autowired
    private ProfileStatisticRepository profileStatisticRepository;

    /** API: Trả về thông tin event */
    public String getCurrentEvent(){
        SessionObj session = adminService.getSession();
        Profile profile = profileService.getProfileByID(session.getId());
        JsonObject jsonObject = getEventJsonObject(profile);
        if(jsonObject == null){
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "USER không nhận được event", NetWorkAPI.UNKNOWN);
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.UNKNOWN);
    }

    /** LOCAL_QUEUE: Lắng nghe sk user login, ghi nhận điểm danh */
    @Override
    public void listenLogin(Profile session){
        try{
            // Ghi nhận điểm danh
            JsonObject jsonObject = getEventJsonObject(session);
        } catch(Exception ex){
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.DEBUG, Login7dQuestService.class));
        }
    }

    /** LOCAL_QUEUE: Lắng nghe sk update tutorial, ghi nhận điểm danh */
    public void listenTutorialEvent(long userId){
        Profile profile = profileService.getProfileByID(userId);
        if(profile == null) return;
        JsonObject jsonObject = getEventJsonObject(profile);
    }

    /** API: User Claim Reward */
    public String claim(HttpServletRequest request){
        JsonObject res = new JsonObject();
        JsonObject requestObject = RequestUtils.requestToJson(request);
        int dayId = requestObject.get("dayId").getAsInt();
        if (dayId <= 0) return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "dayId missing", NetWorkAPI.UNKNOWN);
        SessionObj session = adminService.getSession();
        Profile profile = profileService.getProfileByID(session.getId());
        res = recordClaimReward(profile, dayId);
        JsonArray rewards = new JsonArray();
        if(res != null && res.has("rewards")){
            rewards = res.get("rewards").getAsJsonArray();
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), res, rewards, -1).toString();
    }

    /**
     * Kiểm tra các ràng buộc, ghi nhận user claim dayReward, trả thưởng, update cache Tiến dộ
     * @param profile - userProfile thời điểm request claim
     * @param dayId
     * @return JsonObject - tiến độ user sau khi claim
     */
    private JsonObject recordClaimReward(Profile profile, int dayId){
        List<EventEnt> eventEnts = getRunningEvents();
        if(eventEnts.isEmpty()) return null;    // event đã hết hạn, hoặc ko c event nào đang chạy
        EventEnt eventEnt = eventEnts.get(0);

        if(!isUserCanJoinEvent(profile)){
            loggingUnNormalCase(profile.getId(), "User request claim without join event");
            return null;
        }

        UserProgress userProgress = getUserProgress(profile.getId(), eventEnt);
        if(userProgress == null){
            loggingUnNormalCase(profile.getId(), "User request claim without progress");
            return null;
        }

        List<RewardEnt> list = new ArrayList<>();
        for(DayReward dayReward: userProgress.getDayRewards()){
            if(dayReward.getDayId() == dayId){
                if(dayReward.getRewardStatus() == ERewardStatus.CLAIMABLE.ordinal()
                        || dayReward.getRewardStatus() == ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal()){
                    list = sendReward(profile, eventEnt.getId(), userProgress, dayId);
                    dataTrackingUserClaim(profile, eventEnt, userProgress, dayReward, list);
                    userProgress = getUserProgress(profile.getId(), eventEnt);
                }
                break;
            }
        }

        // build userProgress to JsonObject;
        JsonObject jsonObject = userProgress.toJsonObject();
        jsonObject.addProperty("userId", profile.getId());
        jsonObject.addProperty("eventName", eventEnt.getTitle());
        JsonArray updateRewardBody = JsonBuilder.buildListReward(list);
        jsonObject.add("rewards", updateRewardBody);
        return jsonObject;

    }

    /**
     * Trả thưởng cho user, update cache Redis, Update Reddot Quest
     */
    private List<RewardEnt> sendReward(Profile profile, int eventId, UserProgress userProgress, int dayId){
        List<RewardEnt> list = new ArrayList<>();
        List<DayReward> dayRewards = new ArrayList<>(userProgress.getDayRewards());
        for(DayReward dayReward: dayRewards){
            if(dayReward.getDayId() == dayId){
                if (ERewardType.isChest(dayReward.getRewardType())){
                    list.addAll(chestService.randomChestReward(profile.getId(), dayReward.getRewardType()));
                } else {
                    list.addAll(inventoryService.claimItem(profile.getId(), ERewardType.findByValue(dayReward.getRewardType()), dayReward.getRewardAmount(), dayReward.getRewardRefId(), EUpdateMoneyType.QUEST));
                }
                UserProgress newProgress = switchRewardDayStatus(userProgress, dayReward);
                updateCacheProgress(profile.getId(), eventId, newProgress);
                reddotService.updateReddot(profile.getId(), EReddotFeature.EVENT_LOGIN_7D, -1);
                break;
            }
        }
        return list;
    }

    /**
     * Đổi trạng thái các dayReward (mỗi khi user nhận/ xem ads)
     * - Nếu state hiện tại CLAIMABLE đổi thành:
     *      WATCHABLE_ADS : nếu các ngày trước đó đã claim hết
     *      INACTIVE_WATCH_ADS: nếu các ngày trước có ngày có thể claim/xem ads
     * - Nếu state hiện tại là WATCHABLE_ADS_TO_CLAIM thì:
     *      1) Đổi hiện tại thành CLAIMED_ALL
     *      2) đổi ngày liền kề từ INACTIVE_WATCH_ADS thành WATCHABLE_ADS
     * *** Mục đích: buộc user phải xem ads tuần tự (ko cho chọn ngày để xem ads)
     * @param dayReward
     * @return userProgress sau khi đổi trạng thái các ngày
     */
    private UserProgress switchRewardDayStatus(UserProgress userProgress, DayReward dayReward){
        int curStatus = dayReward.getRewardStatus();
        if(curStatus == ERewardStatus.CLAIMABLE.ordinal()){
            // nhấn CLAIM xong thì chuyển thành WATCHABLE_ADS_TO_CLAM
            curStatus = ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal();
            // nhưng nếu tồn tại 1 ngày nào trước đó CHƯA XEM ADS hoặc CHƯA CLAIM thì chuyển thành INACTIVE_WATCH_ADS_TO_CLAIM
            List<DayReward> dayRewardBefore = userProgress.getDayRewards().stream().filter(d -> d.getDayId() < dayReward.getDayId()
                    && (dayReward.getRewardStatus() == ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal()
                        || dayReward.getRewardStatus() == ERewardStatus.CLAIMABLE.ordinal())
            ).collect(Collectors.toList());
            if(!dayRewardBefore.isEmpty()){
                curStatus = ERewardStatus.INACTIVE_WATCH_ADS_TO_CLAIM.ordinal();
            }

            for(DayReward dr: userProgress.getDayRewards()){
                if(dr.getDayId() == dayReward.getDayId()){
                    dr.setRewardStatus(curStatus);
                    dr.setLastRewardUpdateAtMs(System.currentTimeMillis());
                    break;
                }
            }

        }
        else if(dayReward.getRewardStatus() == ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal()){
            for(DayReward dr : userProgress.getDayRewards()){
                if(dr.getDayId() == dayReward.getDayId()){
                    dr.setRewardStatus(ERewardStatus.CLAIMED_ALL.ordinal());
                    dr.setLastRewardUpdateAtMs(System.currentTimeMillis());
                }
                // đổi ngày tiếp theo ở state INACTIVE_WATCH_ADS_TO_CLAIM (nếu có) thành WATCHABLE_ADS_TO_CLAM
                if(dr.getDayId() == dayReward.getDayId() + 1){
                    if(dr.getRewardStatus() == ERewardStatus.INACTIVE_WATCH_ADS_TO_CLAIM.ordinal()){
                        dr.setRewardStatus(ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal());
                        break;
                    }
                }
            }
        }

        return userProgress;
    }

    /**
    * Ghi nhận điểm danh & Trả về thông tin event của user dưới dạng JsonObject
    * */
    private JsonObject getEventJsonObject(Profile profile){
        JsonObject jsonObject = new JsonObject();

        if(!isUserCanJoinEvent(profile)) return null; // user chưa đủ dk tham gia

        long userId = profile.getId();

        List<EventEnt> eventEnts = getRunningEvents();
        if(eventEnts.isEmpty()) return null;
        EventEnt eventEnt = eventEnts.get(0);

        // Lấy user progress
        UserProgress userProgress = getUserProgress(userId, eventEnt);

        // user ko có progress, có thể: user đã tham gia event này r hoặc chưa tham gia lần nào
        if(userProgress == null){
            if(isUserJoinedEvent(userId)) return null; // user đã tham gia event này r.
            else userProgress = initProgress(userId, eventEnt);
        }

        // user chưa điểm danh, ghi nhận điểm danh
        if(!isUserRecordedLoginToday(userProgress)){
            userProgress = recordLogin(profile, eventEnt, userProgress);
        }

        // build userProgress to JsonObject;
        jsonObject = userProgress.toJsonObject();
        jsonObject.addProperty("userId", userId);
        jsonObject.addProperty("eventName", eventEnt.getTitle());
        jsonObject.addProperty("eventStartAtMs", userProgress.getAssignEventAtMs());
        return jsonObject;
    }

    /** Trả về danh sách các Event đang chạy*/
    private List<EventEnt> getRunningEvents(){
        List<TriggerEnt> listTrigger = triggerService.getCurrentListTrigger(ETriggerType.LOGIN_7DAYS.getValue());
        List<Integer> listEventId = listTrigger.stream().map(e -> e.getRefId()).collect(Collectors.toList());
        return triggerService.getListEvent(listEventId);
    }

    /**
    * Kiểm tra user có đủ điều kiện tham gia event.
    * Điều kiện:
    * 1) Hoàn thành "XONG" tutorial
    * */
    private boolean isUserCanJoinEvent(Profile profile){
        return profile.getTutorial() >= TRIGGER_AFTER_TUTORIAL_STEP;
    }

    /**
    * Kiểm tra user đã từng tham gia event này trước đó rồi chưa ?
    * */
    private boolean isUserJoinedEvent(Long userId){
        ProfileStatistic profileStatistic = profileStatisticRepository.read().findById(userId).orElse(null);
        if(profileStatistic.getIsJoined7dEvent() > 0 ) return true;
        return false;
    }

    /** Kiểm tra thời điểm hiện tại user đã điểm danh cho ngày hôm nay chưa ?*/
    private boolean isUserRecordedLoginToday(UserProgress userProgress){
        if(userProgress.lastRecordLoginAtMs <= 0) return false;
        String lastRecordAtDateStr = DateTimeUtils.toString(DateTimeUtils.getDateTime(userProgress.getLastRecordLoginAtMs()), DATE_FORMAT);
        String toDayDate = DateTimeUtils.toString(DateTimeUtils.getDateTime(System.currentTimeMillis()), DATE_FORMAT);
        if(toDayDate.equals(lastRecordAtDateStr)) return true;
        return false;
    }

    /**
     * Ghi nhận user điểm danh, update Redis, update Reddot
     * @Param userProgress - progress hiện tại của user
     * @Return UserProgress - progress mới của user sau khi ghi nhận
     * */
    private UserProgress recordLogin(Profile profile, EventEnt eventEnt, UserProgress userProgress){
        userProgress.dayRewards.sort(Comparator.comparing(DayReward::getDayId));
        // loop qua các ngày, tìm ngày nhỏ nhất có status NOT_READY, switch to CLAIMABLE
        for (DayReward dayReward: userProgress.dayRewards){
            if(dayReward.dayId > 0 && dayReward.getRewardStatus() == ERewardStatus.LOCK.ordinal()) {
                dayReward.setRewardStatus(ERewardStatus.CLAIMABLE.ordinal());
                userProgress.setLastRecordLoginAtMs(System.currentTimeMillis());
                updateCacheProgress(profile.getId(), eventEnt.getId(), userProgress);
                reddotService.updateReddot(profile.getId(), EReddotFeature.EVENT_LOGIN_7D, 1);
                dataTrackingUserDone(profile, eventEnt, userProgress, dayReward);
                break;
            }
        }

        return userProgress;
    }

    /**
     * Update userProgress ở Redis
     * */
    private void updateCacheProgress(Long userId, int eventId, UserProgress userProgress){
        // format ko theo ngày
        String progressHashKey = getHashEventKey(userId,eventId, EEventRecordType.NONE);
        redisTemplateString.opsForHash().putAll(progressHashKey, userProgress.toMap());
        redisTemplateString.expire(progressHashKey, Duration.ofDays(REDIS_TTL_DAYS));
    }

    /**
     * Get userProgress ở Redis
     * @Return Map Object-Object
     * */
    private Map<String, String> getCacheProgress(Long userId, int eventId){
        String progressHashKey = getHashEventKey(userId,eventId, EEventRecordType.NONE);
        Map<String, String> res = redisTemplateString.<String, String>opsForHash().entries(progressHashKey);
        return res;
    }

    /** Khởi tạo tiến đô cho user, update xuống redis */
    private UserProgress initProgress(Long userId, EventEnt eventEnt){
        UserProgress userProgress = new UserProgress();
        userProgress.setAssignEventAtMs(System.currentTimeMillis());
        userProgress.setDayRewards(new ArrayList<>());
        userProgress.setLastRecordLoginAtMs(0);
        userProgress.setDayRewards(new ArrayList<>(getRewardConfig(eventEnt).values()));
        profileStatisticRepository.write().updateIsJoined7dEvent(userId, 1);
        updateCacheProgress(userId, eventEnt.getId(), userProgress);
        return userProgress;
    }

    /**
     * Trả về tiến độ của user (từ cache Redis) ở dạng object UserProgress
     * @return
     */
    private UserProgress getUserProgress(Long userId, EventEnt eventEnt){
        Map<String, String> cacheProgress = getCacheProgress(userId, eventEnt.getId());
        if(cacheProgress.isEmpty()) return null;
        UserProgress userProgress = new UserProgress();
        Map<Integer, DayReward> dayId_dayReward = new HashMap<>();
        for(Map.Entry<String, String> entry: cacheProgress.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if(key.equals("assign_event_at_ms")){
                userProgress.setAssignEventAtMs(Long.parseLong(value));
                continue;
            }
            if(key.equals("last_record_login_at_ms")) {
                userProgress.setLastRecordLoginAtMs(Long.parseLong(value));
                continue;
            }
            for( int dayId = 1; dayId<= NUM_REWARD_DAYS; dayId++){
                if( key.contains(String.format("reward_day%d_reward_amount", dayId))) {
                    DayReward dayReward = dayId_dayReward.getOrDefault(dayId, new DayReward());
                    dayReward.setDayId(dayId);
                    dayReward.setRewardAmount(ConvertUtils.toInt(value));
                    dayId_dayReward.put(dayId, dayReward);
                    break;
                }
                else if( key.contains(String.format("reward_day%d_name", dayId))) {
                    DayReward dayReward = dayId_dayReward.getOrDefault(dayId, new DayReward());
                    dayReward.setDayId(dayId);
                    dayReward.setDayName(value);
                    dayId_dayReward.put(dayId, dayReward);
                    break;
                }
                else if(key.contains(String.format("reward_day%d_reward_status", dayId))) {
                    DayReward dayReward = dayId_dayReward.getOrDefault(dayId, new DayReward());
                    dayReward.setDayId(dayId);
                    dayReward.setRewardStatus(ConvertUtils.toInt(value));
                    dayId_dayReward.put(dayId, dayReward);
                    break;
                }
                else if(key.contains(String.format("reward_day%d_reward_type", dayId))) {
                    DayReward dayReward = dayId_dayReward.getOrDefault(dayId, new DayReward());
                    dayReward.setDayId(dayId);
                    dayReward.setRewardType(ConvertUtils.toInt(value));
                    dayId_dayReward.put(dayId, dayReward);
                    break;
                }
                else if(key.contains(String.format("reward_day%d_reward_ref_id", dayId))) {
                    DayReward dayReward = dayId_dayReward.getOrDefault(dayId, new DayReward());
                    dayReward.setDayId(dayId);
                    dayReward.setRewardRefId(ConvertUtils.toInt(value));
                    dayId_dayReward.put(dayId, dayReward);
                    break;
                }
                else if(key.contains(String.format("reward_day%d_status_update_at_ms", dayId))) {
                    DayReward dayReward = dayId_dayReward.getOrDefault(dayId, new DayReward());
                    dayReward.setDayId(dayId);
                    dayReward.setLastRewardUpdateAtMs(ConvertUtils.toLong(value));
                    dayId_dayReward.put(dayId, dayReward);
                    break;
                }

            }
        }
        if(dayId_dayReward.size() < NUM_REWARD_DAYS){
            Map<Integer, DayReward> rewardConfig = getRewardConfig(eventEnt);
            for(int i = 1; i <= NUM_REWARD_DAYS; i++){
                DayReward dayReward = dayId_dayReward.getOrDefault(i, rewardConfig.get(i));
                // chưa claim thì lấy config trong db
                if(dayReward.getRewardStatus() == ERewardStatus.CLAIMABLE.ordinal()){
                    DayReward newDayReward = rewardConfig.get(i);
                    dayReward.setRewardType(newDayReward.getRewardType());
                    dayReward.setRewardAmount(newDayReward.getRewardAmount());
                    dayReward.setRewardRefId(newDayReward.getRewardRefId());
                }

                dayId_dayReward.put(i, dayReward);
            }
        }
        List<DayReward> dayRewards = new ArrayList<>(dayId_dayReward.values());
        dayRewards.sort(Comparator.comparing(DayReward::getDayId));

        // Đảm bảo phải có ít nhất 1 ngày có thể CLAIM/WATCHABLE_ADS trước các ngày INACTIVE_WATCH_ADS
        // Đảm bảo chỉ có 1 ngày WATCHABLE_ADS
        int claimableOrWatchableCount = 0;
        boolean isProgressUpdated = false;
        for(DayReward dayReward : dayRewards){
                if(dayReward.getRewardStatus() == ERewardStatus.CLAIMABLE.ordinal()
                        || dayReward.getRewardStatus() == ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal()){
                    claimableOrWatchableCount++;
                }
                else if(dayReward.getRewardStatus() == ERewardStatus.INACTIVE_WATCH_ADS_TO_CLAIM.ordinal()){
                    // Nếu chưa có ngày nào có thể claim/watchable_ads trước đó thì đổi thành watchable_ads
                    if(claimableOrWatchableCount == 0){
                        dayReward.setRewardStatus(ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal());
                        claimableOrWatchableCount++;
                        isProgressUpdated = true;
                    }
                } else if(dayReward.getRewardStatus() == ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal()){
                    if(claimableOrWatchableCount > 0){
                        dayReward.setRewardStatus(ERewardStatus.INACTIVE_WATCH_ADS_TO_CLAIM.ordinal());
                        isProgressUpdated = true;
                    }
                }
        }
        userProgress.setDayRewards(dayRewards);
        if(isProgressUpdated){
            updateCacheProgress(userId, eventEnt.getId(), userProgress);
        }

        return userProgress;
    }

    private void loggingUnNormalCase(Long userId, String msg){
        GMLocalQueue.addQueue(new TelegramLoggerCmd("UserId: "+ userId +", msg: " + msg, TeleLogType.DEBUG, this.getClass()));
    }

    /**
     * Send tracking data khi user DONE
     * @param profile - profile user ngay trước thời điểm DONE
     * @param eventEnt - thông tin config của event
     * @param userProgress - tiến độ của user ngay trước thời điểm DONE
     * @param dayReward - Ngày user done
     */
    private void dataTrackingUserDone(Profile profile, EventEnt eventEnt, UserProgress userProgress, DayReward dayReward){
        dataTracking("DONE", profile, eventEnt, userProgress, dayReward, new ArrayList<>());
    }

    /**
     * Send tracking data khi user CLAIM reward
     * @param profile - profile user ngay trước thời điểm CLAIM
     * @param eventEnt - thông tin config của event
     * @param userProgress - tiến độ của user ngay trước thời điểm CLAIM
     * @param dayReward - Ngày user request CLAIM reward
     * @param rewards - Danh sách phần thưởng user nhận được
     */
    private void dataTrackingUserClaim(Profile profile, EventEnt eventEnt, UserProgress userProgress, DayReward dayReward, List<RewardEnt> rewards){
        String action = "CLAIM";
        if(dayReward.getRewardStatus() == ERewardStatus.WATCHABLE_ADS_TO_CLAM.ordinal()){
            action = "CLAIM_WITH_ADS";
        }
        dataTracking(action, profile, eventEnt, userProgress, dayReward, rewards);
    }

    private void dataTracking(String actionType, Profile profile, EventEnt eventEnt, UserProgress userProgress, DayReward dayReward, List<RewardEnt> rewards){
        JsonObject eventInfo = new JsonObject();
        eventInfo.addProperty("action", actionType);
        QuestDTO questDTO = new QuestDTO();
        questDTO.setId(dayReward.getDayId());
        questDTO.setTitle(dayReward.getDayName());
        eventInfo.addProperty("endTime", userProgress.getEventEndAtMs());
        eventInfo.addProperty("title", eventEnt.getTitle());
        eventInfo.addProperty("id", eventEnt.getId());
        JsonArray rewardArray = JsonBuilder.buildListReward(rewards);
        GMLocalQueue.addQueue(new QuestTrackingCmd(producer, profile, profile.getTrophy(), eventInfo, questDTO, rewardArray, actionType,  redisTemplateString));
    }
    /**
     * Đọc config trong db event
     * @param eventEnt
     * @return
     */
    private Map<Integer, DayReward> getRewardConfig(EventEnt eventEnt){
        Map<Integer, DayReward> dayId_dayReward = new HashMap<>();
        JsonObject customData =  JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        JsonArray rewardConfig = customData.getAsJsonArray("reward_config");
        for(JsonElement rewardConfigElement : rewardConfig){
            JsonObject reward = rewardConfigElement.getAsJsonObject();
            DayReward dayReward = new DayReward();
            dayReward.setDayId(ConvertUtils.toInt(reward.get("day_id")));
            dayReward.setRewardAmount(ConvertUtils.toInt(reward.get("reward_amount")));
            dayReward.setDayName(reward.get("day_name").getAsString());
            dayReward.setRewardType(ConvertUtils.toInt(reward.get("reward_type")));
            dayReward.setRewardRefId(ConvertUtils.toInt(reward.get("reward_ref_id")));
            dayReward.setRewardStatus(ERewardStatus.LOCK.ordinal());
            dayId_dayReward.put(dayReward.getDayId(), dayReward);
        }
        return dayId_dayReward;
    }

    @Data
    private class UserProgress{
        private long assignEventAtMs;   // thời điểm user nhận quest
        private List<DayReward> dayRewards; // Thông tin reward của users
        private long lastRecordLoginAtMs;   // thời điểm ghi nhận dđiểm danh gần nhất
        private long getEventEndAtMs(){
            Date assignDate = DateTimeUtils.getBeginDate(DateTimeUtils.getDateTime(assignEventAtMs));
            Date endDate = DateTimeUtils.addDays(assignDate, EVENT_TTL_DAYS); // cuối này 10, đầu ngày 11
            return DateTimeUtils.getMiliseconds(endDate);
        }

        /**
         * Thời điểm bắt đầu ngày tiếp theo
         * @return
         */
        private long getNextDayStartAtMs(){
            String todayDateStr = DateTimeUtils.toString(DateTimeUtils.getDateTime(System.currentTimeMillis()), DATE_FORMAT);
            Date todayDate = DateTimeUtils.getDateTime(todayDateStr, DATE_FORMAT);
            Date nextDay = DateTimeUtils.addMinutes(todayDate, NUM_MINS_A_DAY); // ngày tiếp theo
            return DateTimeUtils.getMiliseconds(nextDay);
        }

        public JsonObject toJsonObject(){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("assignEventAtMs", assignEventAtMs);
            jsonObject.addProperty("eventEndAtMs", getEventEndAtMs());
            jsonObject.addProperty("nextDayStartAtMs", getNextDayStartAtMs());
            JsonArray rewards = new JsonArray();
            for(DayReward dayReward: dayRewards){
                rewards.add(dayReward.toJsonObject());
            }
            jsonObject.add("eventRewards", rewards);
            return jsonObject;
        }

        /**
         *
         * @return Redis Hash Map
         */
        public Map<String, String> toMap(){
            Map<String, String> map = new HashMap<>();
            map.put("assign_event_at_ms", String.valueOf(assignEventAtMs));
            map.put("last_record_login_at_ms", String.valueOf(lastRecordLoginAtMs));

            List<DayReward> dayRewards = this.dayRewards.stream()
                    .filter(d -> d.rewardStatus != ERewardStatus.LOCK.ordinal()).collect(Collectors.toList());
            for(DayReward dayReward: dayRewards){
                String dayFormat = String.format("reward_day%d_", dayReward.getDayId());
                map.put(dayFormat+"name", String.valueOf(dayReward.getDayName()));
                map.put(dayFormat+"reward_type", String.valueOf(dayReward.getRewardType()));
                map.put(dayFormat+"reward_ref_id", String.valueOf(dayReward.getRewardRefId()));
                map.put(dayFormat+"reward_amount", String.valueOf(dayReward.getRewardAmount()));
                map.put(dayFormat+"reward_status", String.valueOf(dayReward.getRewardStatus()));
                map.put(dayFormat+"status_update_at_ms", String.valueOf(dayReward.getLastRewardUpdateAtMs()));
            }
            return map;
        }
    }

    @Data
    private class DayReward{
        private int dayId;
        private String dayName;
        private int rewardType;
        private int rewardRefId;
        private int rewardAmount;
        private int rewardStatus;
        private long lastRewardUpdateAtMs;

        public JsonObject toJsonObject(){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("dayId", dayId);
            jsonObject.addProperty("dayName", dayName);
            jsonObject.addProperty("rewardType", rewardType);
            jsonObject.addProperty("rewardRefId", rewardRefId);
            jsonObject.addProperty("rewardAmount", rewardAmount);
            jsonObject.addProperty("rewardStatus", rewardStatus);
//            jsonObject.addProperty("lastRewardUpdateAtMs", lastRewardUpdateAtMs);
            return jsonObject;
        }

    }

    private enum ERewardStatus {
        LOCK, // 0 - chưa đến ngày
        CLAIMABLE, // 1 - đã điểm danh, có thể nhấn nhận (Bấm Claim để nhận thưởng)
        WATCHABLE_ADS_TO_CLAM, // 2 - đã nhận, có thể xem ads nhận lần nữa (Bấm nút Watch Ads để xem ads và nhận thưởng)
        INACTIVE_WATCH_ADS_TO_CLAIM, // 3 - nút xem ads để nhận bị disable do có ngày trước đó chưa xem ads
        CLAIMED_ALL // 4 - đã nhận hết
    }
}
