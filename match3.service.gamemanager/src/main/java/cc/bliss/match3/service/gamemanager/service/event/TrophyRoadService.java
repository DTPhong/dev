/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.match3.ProfileWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.TrophyRoadWriteRepository;
import cc.bliss.match3.service.gamemanager.db.specification.TrophyRoadSpecification;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.common.UpdateMoneyResult;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TrophyRoadMileStoneEnt;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TrophyRoadTrackingCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.shop.ChestService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class TrophyRoadService extends BaseService {

    private static final String HASH_TROPHYROAD_USER = "trophyroad_%s";
    @Autowired
    AdminService adminService;
    @Autowired
    ProfileService profileService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    ChestService chestService;
    @Autowired
    HeroService heroService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private DailyQuestService dailyQuestService;

    public String getTrophyRoad() {
        JsonObject body = getTrophyRoadJson();
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), body, NetWorkAPI.GET_CONFIG);
    }

    public String claimEndTrohpyRoad(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        Profile profile = profileService.getProfileByID(sessionObj.getId());
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        List<RewardEnt> rewardEntList = inventoryService.claimItem(sessionObj.getId(), ERewardType.EMERALD, 100, EUpdateMoneyType.TROPHY_ROAD);
        JsonArray updateRewardBody = JsonBuilder.buildListReward(rewardEntList);

        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), new JsonObject(), updateRewardBody, -1).toString();
    }

    /**
     * @param mileStoneType
     * null: findAll
     * type: MAIN, ADS, ARENA
     */
    public List<TrophyRoadMileStoneEnt> getTrophyRoadMileStoneEnts(ETrophyRoadMileStoneType... mileStoneType) {
        Sort sort = Sort.by("milestone");
        if (mileStoneType != null && mileStoneType.length > 0) {
            Specification<TrophyRoadMileStoneEnt> specification = Specification
                    .where(TrophyRoadSpecification.withType(mileStoneType));
            return trophyRoadRepository.read().findAll(specification, Sort.by("milestone"));
        }
        // return all
        return trophyRoadRepository.read().findAll(Sort.by("milestone"));
    }

    public String claimTrophyRoad(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        Profile profile = profileService.getProfileByID(sessionObj.getId());
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        if (!jsonObject.has("id")) {
            return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), "id không hợp lệ !", NetWorkAPI.GET_CONFIG);
        }
        int idTrophyRoad = jsonObject.get("id").getAsInt();

        List<TrophyRoadMileStoneEnt> listTrophyRoadMileStone = getTrophyRoadMileStoneEnts(null);
        Optional<TrophyRoadMileStoneEnt> optional = listTrophyRoadMileStone.stream().filter(e -> e.getId() == idTrophyRoad).findFirst();
        if (optional.isPresent()) {
            Map<String, String> mapTrophyRoad = hashOperations.entries(getTrophyRoadKey(sessionObj.getId()));
            TrophyRoadMileStoneEnt roadMileStoneEnt = optional.get();
            if (mapTrophyRoad.containsKey(String.valueOf(roadMileStoneEnt.getId()))) {
                return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), "đã nhận phần thưởng !", NetWorkAPI.GET_CONFIG);
            }
            if (roadMileStoneEnt.getType() == ETrophyRoadMileStoneType.ADS.ordinal()) {
                GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, profile.getId(), EQuestType.WATCH_ADS_QUEST, 1));
            }
            hashOperations.increment(getTrophyRoadKey(sessionObj.getId()), String.valueOf(roadMileStoneEnt.getId()), 1);
            mapTrophyRoad.put(String.valueOf(roadMileStoneEnt.getId()), String.valueOf(1));
            JsonArray body = new JsonArray();
            int chest = 0;
            for (RewardEnt reward : roadMileStoneEnt.getRewards()) {
                if (ERewardType.isGold(reward.getERewardType().getValue())) {
                    UpdateMoneyResult moneyResult = profileService.updateMoney(sessionObj.getId(), reward.getDelta(), EUpdateMoneyType.TROPHY_ROAD);
                    reward.setAfter(moneyResult.getAfter());
                    reward.setBefore(moneyResult.getBefore());
                    body.add(JsonBuilder.buildReward(reward));
                } else if (reward.getERewardType() == ERewardType.HERO) {
                    inventoryService.addHero(sessionObj.getId(), reward.getRef());
                    reward.setAfter(1);
                    reward.setBefore(0);
                    body.add(JsonBuilder.buildReward(reward));
                } else if (reward.getERewardType() == ERewardType.HERO_CARD) {
                    long after = inventoryService.addShard(sessionObj.getId(), reward.getRef(), reward.getDelta());
                    reward.setAfter(after);
                    reward.setBefore(0);
                    body.add(JsonBuilder.buildReward(reward));
                } else if (reward.getERewardType() == ERewardType.RARE_CARD) {
                    int heroID;
                    if (reward.getRef() == 0) {
                        List<HeroEnt> heroEnts = heroService.findAll().stream().filter(e -> e.getRarity().equals(EHeroRarity.MYTHIC)).collect(Collectors.toList());
                        heroID = heroEnts.get(RandomUtils.RAND.nextInt(heroEnts.size())).getId();
                        reward.setRef(heroID);
                    } else {
                        heroID = reward.getRef();
                    }
                    long after = inventoryService.addShard(sessionObj.getId(), heroID, reward.getDelta());
                    reward.setAfter(after);
                    reward.setBefore(0);
                    reward.setERewardType(ERewardType.HERO_CARD);
                    body.add(JsonBuilder.buildReward(reward));
                } else if (reward.getERewardType() == ERewardType.EPIC_CARD) {
                    int heroID;
                    if (reward.getRef() == 0) {
                        List<HeroEnt> heroEnts = heroService.findAll().stream().filter(e -> e.getRarity().equals(EHeroRarity.EPIC)).collect(Collectors.toList());
                        heroID = heroEnts.get(RandomUtils.RAND.nextInt(heroEnts.size())).getId();
                    } else {
                        heroID = reward.getRef();
                    }
                    long after = inventoryService.addShard(sessionObj.getId(), heroID, reward.getDelta());
                    reward.setAfter(after);
                    reward.setBefore(0);
                    reward.setERewardType(ERewardType.HERO_CARD);
                    body.add(JsonBuilder.buildReward(reward));
                } else if (reward.getERewardType() == ERewardType.LEGENDARY_CARD) {
                    int heroID;
                    if (reward.getRef() == 0) {
                        List<HeroEnt> heroEnts = heroService.findAll().stream().filter(e -> e.getRarity().equals(EHeroRarity.LEGENDARY)).collect(Collectors.toList());
                        heroID = heroEnts.get(RandomUtils.RAND.nextInt(heroEnts.size())).getId();
                    } else {
                        heroID = reward.getRef();
                    }
                    long after = inventoryService.addShard(sessionObj.getId(), heroID, reward.getDelta());
                    reward.setAfter(after);
                    reward.setBefore(0);
                    reward.setERewardType(ERewardType.HERO_CARD);
                    body.add(JsonBuilder.buildReward(reward));
                } else if (reward.getERewardType() == ERewardType.FEATURE) {
                    reward.setAfter(1);
                    reward.setBefore(0);
                    body.add(JsonBuilder.buildReward(reward));
                } else if (ERewardType.isChest(reward.getERewardType().getValue())) {
                    chest = reward.getERewardType().getValue();
                    List<RewardEnt> listRewardRand = chestService.randomChestReward(sessionObj.getId(), chest);
                    body.addAll(JsonBuilder.buildListReward(listRewardRand));
                    if (chest == ERewardType.BEGINNER_CHEST.getValue()){
                        chest = ERewardType.WOODEN_CHEST.getValue();
                    }
                } else if (ERewardType.isEmerald(reward.getERewardType().getValue())) {
                    List<RewardEnt> listRewardRand = inventoryService.claimItem(sessionObj.getId(), reward.getERewardType(), reward.getDelta(), EUpdateMoneyType.TROPHY_ROAD);
                    body.addAll(JsonBuilder.buildListReward(listRewardRand));
                } else {
                    List<RewardEnt> listRewardRand = inventoryService.claimItem(sessionObj.getId(), reward.getERewardType(), reward.getDelta(), EUpdateMoneyType.TROPHY_ROAD);
                    body.addAll(JsonBuilder.buildListReward(listRewardRand));
                }
            }
            int userTrophy = leaderboardService.getProfileTrophy(profile.getId());
            int trophyMilestone = roadMileStoneEnt.getMilestone();
            int type = roadMileStoneEnt.getType();
            GMLocalQueue.addQueue(new TrophyRoadTrackingCmd(producer, profile, userTrophy, trophyMilestone, type, roadMileStoneEnt.getRewards(), redisTemplateString));
            List<TrophyRoadMileStoneEnt> listTrophyRoadMainMileStone =
                    listTrophyRoadMileStone.stream()
                            .filter(e -> e.getType() == ETrophyRoadMileStoneType.MAIN.ordinal() || e.getType() == ETrophyRoadMileStoneType.ARENA.ordinal()).collect(Collectors.toList());
            List<TrophyRoadMileStoneEnt> listTrophyRoadAds = listTrophyRoadMileStone.stream()
                    .filter(e -> e.getType() == ETrophyRoadMileStoneType.ADS.ordinal()).collect(Collectors.toList());
            JsonObject trophyRoadJson = getTrophyRoadJson(listTrophyRoadMainMileStone,listTrophyRoadAds);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), trophyRoadJson, body, chest).toString();
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), "id không hợp lệ !", NetWorkAPI.GET_CONFIG);
    }

    public String removeAds(){
        SessionObj sessionObj = adminService.getSession();
        Date expireTime = DateTimeUtils.addDays(30);
        profileRepository.write().updateTrophyRoadTicketExpired(sessionObj.getId(), expireTime);
        return getTrophyRoad();
    }

    private String getTrophyRoadKey(long userID){
        return String.format(HASH_TROPHYROAD_USER, userID);
    }

    public void deleteTrophyRoad(long userID){
        String key = getTrophyRoadKey(userID);
        redisTemplateString.delete(key);
    }

    private JsonObject getTrophyRoadJson(){
        List<TrophyRoadMileStoneEnt> listTrophyRoadMileStone = getTrophyRoadMileStoneEnts(ETrophyRoadMileStoneType.MAIN, ETrophyRoadMileStoneType.ARENA);
        List<TrophyRoadMileStoneEnt> listTrophyRoadAds = getTrophyRoadMileStoneEnts(ETrophyRoadMileStoneType.ADS);
        JsonObject body = getTrophyRoadJson(listTrophyRoadMileStone,listTrophyRoadAds);
        return body;
    }

    private JsonObject getTrophyRoadJson(List<TrophyRoadMileStoneEnt> listTrophyRoadMileStone,
                                         List<TrophyRoadMileStoneEnt> listTrophyRoadAds){
        SessionObj sessionObj = adminService.getSession();
        Profile profile = profileService.getProfileByID(sessionObj.getId());
        Map<String, String> mapTrophyRoad = hashOperations.entries(getTrophyRoadKey(sessionObj.getId()));
        JsonObject body = JsonBuilder.buildTrophyRoad(listTrophyRoadMileStone,listTrophyRoadAds, profile, mapTrophyRoad);
        return body;
    }

    private long getStartTrophyRoad(TriggerEnt triggerEnt){
        JsonObject object = JSONUtil.DeSerialize(triggerEnt.getTriggerTime(), JsonObject.class);
        JsonArray repeat_days_in_month = object.get("repeat_days_in_month").getAsJsonArray();
        JsonArray repeat_days_in_week = object.get("repeat_days_in_week").getAsJsonArray();
        if (repeat_days_in_week.size() <= 0 && repeat_days_in_month.size() <= 0) {
            return 0;
        }
        if (repeat_days_in_month.size() > 0) {
            int startDateOfMonth = repeat_days_in_month.get(0).getAsInt();
            Date d = DateTimeUtils.getBeginDate(DateTimeUtils.addDays(startDateOfMonth));
            return d.getTime();
        }
        return 0;
    }

    private long getEndTrophyRoad(TriggerEnt triggerEnt){
        JsonObject object = JSONUtil.DeSerialize(triggerEnt.getTriggerTime(), JsonObject.class);
        JsonArray repeat_days_in_month = object.get("repeat_days_in_month").getAsJsonArray();
        JsonArray repeat_days_in_week = object.get("repeat_days_in_week").getAsJsonArray();
        if (repeat_days_in_week.size() <= 0 && repeat_days_in_month.size() <= 0) {
            return 0;
        }
        if (repeat_days_in_month.size() > 0) {
            int startDateOfMonth = repeat_days_in_month.get(repeat_days_in_month.size() - 1).getAsInt();
            Date d = DateTimeUtils.getBeginDate(DateTimeUtils.addDays(startDateOfMonth));
            return d.getTime() + Duration.ofDays(1).toMillis();
        }
        return 0;
    }

    private long getNextStartTrophyRoad(TriggerEnt triggerEnt){
        JsonObject object = JSONUtil.DeSerialize(triggerEnt.getTriggerTime(), JsonObject.class);
        JsonArray repeat_days_in_month = object.get("repeat_days_in_month").getAsJsonArray();
        JsonArray repeat_days_in_week = object.get("repeat_days_in_week").getAsJsonArray();
        if (repeat_days_in_week.size() <= 0 && repeat_days_in_month.size() <= 0) {
            return 0;
        }
        if (repeat_days_in_month.size() > 0) {
            int startDateOfMonth = repeat_days_in_month.get(0).getAsInt();
            Date d = DateTimeUtils.getBeginDate(DateTimeUtils.addDays(startDateOfMonth));
            return d.getTime();
        }
        return 0;
    }
}
