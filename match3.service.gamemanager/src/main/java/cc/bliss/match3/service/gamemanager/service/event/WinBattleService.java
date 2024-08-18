package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.EventRepository;
import cc.bliss.match3.service.gamemanager.ent.common.ChestConfig;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RandomItem;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WinBattleService extends BaseService {

    private static final Map<Integer, ChestConfig> MAP_MYSTERY_BOX_CONFIG = new NonBlockingHashMap<>();

    private static final String USER_MYSTERY_BOX_KEY = "mystery_box_%s_%s";

    private static final String USER_WIN_BATTLE_LIST_PROGRESS_KEY = "win_battle_list_progress_%s";

    private static final int MYSTERY_BOT_MAX_UPGRADE = 4;

    public static final String HASH_USER_WIN_BATTLE_INFO = "hash_win_battle_info_%s_%s_%s";

    private static final int TROPHY_TO_UNLOCK = 60;

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private AdminService adminService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private HeroService heroService;
    @Autowired
    private LeaderboardService leaderboardService;

    static {
        {
            //mystery box rare
            ChestConfig config = new ChestConfig();
            config.setId(ERewardType.MYSTERY_BOX_RARE.getValue());
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(150).orgin(150)
                        .ownedPercentRewardType(55)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EMERALD).bound(10).orgin(10)
                        .ownedPercentRewardType(30)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(6).orgin(6)
                        .ownedPercentRewardType(15)
                        .notOwnedPercent(5)
                        .build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.NONE.getValue());
            MAP_MYSTERY_BOX_CONFIG.put(ERewardType.MYSTERY_BOX_RARE.getValue(), config);
        }
        {
            //mystery box epic
            ChestConfig config = new ChestConfig();
            config.setId(ERewardType.MYSTERY_BOX_EPIC.getValue());
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(300).orgin(300)
                        .ownedPercentRewardType(50)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EMERALD).bound(30).orgin(30)
                        .ownedPercentRewardType(35)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(12).orgin(12)
                        .ownedPercentRewardType(10)
                        .notOwnedPercent(5)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EPIC_CARD).bound(1).orgin(1)
                        .ownedPercentRewardType(10)
                        .notOwnedPercent(5)
                        .build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.NONE.getValue());
            MAP_MYSTERY_BOX_CONFIG.put(ERewardType.MYSTERY_BOX_EPIC.getValue(), config);
        }
        {
            //mystery box legendary
            ChestConfig config = new ChestConfig();
            config.setId(ERewardType.MYSTERY_BOX_LEGENDARY.getValue());
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(600).orgin(600)
                        .ownedPercentRewardType(40)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EMERALD).bound(90).orgin(90)
                        .ownedPercentRewardType(30)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(24).orgin(24)
                        .ownedPercentRewardType(15)
                        .notOwnedPercent(5)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.ROYAL_AMETHYST).bound(1).orgin(1)
                        .ownedPercentRewardType(10)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EPIC_CARD).bound(1).orgin(1)
                        .ownedPercentRewardType(5)
                        .notOwnedPercent(5)
                        .build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.NONE.getValue());
            MAP_MYSTERY_BOX_CONFIG.put(ERewardType.MYSTERY_BOX_LEGENDARY.getValue(), config);
        }
        {
            //mystery box mythic
            ChestConfig config = new ChestConfig();
            config.setId(ERewardType.MYSTERY_BOX_MYTHIC.getValue());
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(1300).orgin(1300)
                        .ownedPercentRewardType(45)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EMERALD).bound(180).orgin(180)
                        .ownedPercentRewardType(30)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(50).orgin(50)
                        .ownedPercentRewardType(10)
                        .notOwnedPercent(5)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.ROYAL_AMETHYST).bound(3).orgin(3)
                        .ownedPercentRewardType(10)
                        .build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EPIC_CARD).bound(3).orgin(3)
                        .ownedPercentRewardType(5)
                        .notOwnedPercent(5)
                        .build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.NONE.getValue());
            MAP_MYSTERY_BOX_CONFIG.put(ERewardType.MYSTERY_BOX_MYTHIC.getValue(), config);
        }
    }

    public String mysteryBoxUpgrade() {
        long userID = adminService.getSession().getId();
        String userWinBattleProgressKey = getUserWinBattleListProgressKey(userID);
        List<String> latestWinDate = redisTemplateString.opsForList().range(userWinBattleProgressKey, -1, -1);
        JsonObject response = new JsonObject();
        if (!latestWinDate.isEmpty()) {
            String lastDateValue = latestWinDate.get(0);
            boolean isMatchConditionToClaim = isMatchConditionToClaim(userID, lastDateValue);
            if (isMatchConditionToClaim) {
                String upgradeField = "upgrade_count";
                EventEnt eventEnt = getWinBattleEvent();
                int eventID = eventEnt.getId();
                Map<String, String> mapData = getMapEventData(userID, eventID, lastDateValue);
                int upgradeCount = ConvertUtils.toInt(mapData.get(upgradeField));
                if (upgradeCount <= 0) {
                    return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Can not upgrade more!", NetWorkAPI.UNKNOWN);
                }
                //upgrade box
                String mysteryBoxField = getUserMysteryBoxKey(userID, lastDateValue);
                String value = redisTemplateString.opsForValue().get(mysteryBoxField);
                int boxId = value == null ? -1 : Integer.parseInt(value);

                ChestConfig chestConfig = new ChestConfig();
                if (upgradeCount == MYSTERY_BOT_MAX_UPGRADE) {
                    chestConfig = MAP_MYSTERY_BOX_CONFIG.get(boxId);
                } else {
                    //random mysteryBox
                    if (ERewardType.MYSTERY_BOX_MYTHIC.getValue() != boxId) {
                        List<ChestConfig> chestConfigs = setAppearanceConfig(boxId);
                        int boxRandom = getRandMysteryBox(chestConfigs);
                        chestConfig = MAP_MYSTERY_BOX_CONFIG.get(boxRandom);
                    }
                }
                response.addProperty("id", chestConfig.getId());
                response.addProperty("quantity", 1);
                response.addProperty("title", ERewardType.findByValue(chestConfig.getId()).name());
                response.addProperty("maxUpgrade", MYSTERY_BOT_MAX_UPGRADE - 1);
                response.addProperty("remainUpgrade", upgradeCount - 1);
                recordDynamicField(userID, eventID, upgradeField, -1, lastDateValue);
                //update box value
                redisTemplateString.opsForValue().set(mysteryBoxField, String.valueOf(chestConfig.getId()), 7, TimeUnit.DAYS);
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.UNKNOWN);
    }

    private List<ChestConfig> setAppearanceConfig(int boxId) {
        List<ChestConfig> chestConfigs = new ArrayList<>();
        if (ERewardType.MYSTERY_BOX_RARE.getValue() == boxId) {
            {
                ChestConfig config = MAP_MYSTERY_BOX_CONFIG.get(ERewardType.MYSTERY_BOX_RARE.getValue());
                config.setAppearanceRate(70);
                chestConfigs.add(config);
            }
            {
                ChestConfig config = MAP_MYSTERY_BOX_CONFIG.get(ERewardType.MYSTERY_BOX_EPIC.getValue());
                config.setAppearanceRate(30);
                chestConfigs.add(config);
            }
        }
        if (ERewardType.MYSTERY_BOX_EPIC.getValue() == boxId) {
            {
                ChestConfig config = MAP_MYSTERY_BOX_CONFIG.get(ERewardType.MYSTERY_BOX_EPIC.getValue());
                config.setAppearanceRate(92);
                chestConfigs.add(config);
            }
            {
                ChestConfig config = MAP_MYSTERY_BOX_CONFIG.get(ERewardType.MYSTERY_BOX_LEGENDARY.getValue());
                config.setAppearanceRate(8);
                chestConfigs.add(config);
            }
        }
        if (ERewardType.MYSTERY_BOX_LEGENDARY.getValue() == boxId) {
            {
                ChestConfig config = MAP_MYSTERY_BOX_CONFIG.get(ERewardType.MYSTERY_BOX_LEGENDARY.getValue());
                config.setAppearanceRate(95);
                chestConfigs.add(config);
            }
            {
                ChestConfig config = MAP_MYSTERY_BOX_CONFIG.get(ERewardType.MYSTERY_BOX_MYTHIC.getValue());
                config.setAppearanceRate(5);
                chestConfigs.add(config);
            }
        }
        return chestConfigs;
    }

    public String winBattleProgress() {
        long userID = adminService.getSession().getId();
        ZoneId utcZone = ZoneId.systemDefault();
        EventEnt eventEnt = getWinBattleEvent();
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        int adjustHours = customData.get("adjustHours").getAsInt();
        LocalDateTime now = LocalDateTime.now(utcZone).minusHours(adjustHours);
        JsonArray listMilestone = customData.getAsJsonArray("listMilestone");
        int maxMilestone = customData.get("maxMilestone").getAsInt();
        int resetHour = customData.get("resetHour").getAsInt();
        int resetMinute = customData.get("resetMinute").getAsInt();
        int resetSecond = customData.get("resetSecond").getAsInt();
        LocalDateTime nextResetTime = now.withHour(resetHour).withMinute(resetMinute).withSecond(resetSecond).minusHours(adjustHours);
        if (now.isAfter(nextResetTime)) {
            nextResetTime = nextResetTime.plusDays(1);
        }
        Date date = DateTimeUtils.toDate(now);
        String dateStr = DateTimeUtils.toString(date, "dd/MM/YYYY");

        long endTime = nextResetTime.plusHours(adjustHours).atZone(utcZone).toInstant().toEpochMilli();
        int eventID = eventEnt.getId();
        JsonObject winBattleData = buildWinProgressData(userID, endTime, listMilestone, eventID, maxMilestone, dateStr);
        return ResponseUtils.toWinBattleProgressResponseBody(HttpStatus.OK.value(), winBattleData).toString();
    }

    private boolean isBot(long userID) {
        Profile profile = profileService.getMinProfileByID(userID);
        return profile.getBotType() != 0;
    }

    private int getProgress(long userID, int eventID, String date) {
        //check progress win battle in redis
        String winBattleProgressField = "win_battle_progress";
        Map<String, String> mapData = getMapEventData(userID, eventID, date);
        return ConvertUtils.toInt(mapData.get(winBattleProgressField));
    }

    private JsonObject buildWinProgressData(long userID, long endTime, JsonArray listReward, int eventID, int maxMilestone, String date) {
        JsonObject jsonObject = new JsonObject();
        JsonArray eventRewards = new JsonArray();

        Map<String, String> mapData = getMapEventData(userID, eventID, date);
        for (JsonElement e : listReward) {
            JsonObject item = e.getAsJsonObject();
            String winBattleClaimedField = "win_battle_" + item.get("milestone").getAsInt() + "_is_claim";
            item.addProperty("isClaimed", mapData.containsKey(winBattleClaimedField));
            eventRewards.add(item);
        }
        jsonObject.addProperty("endTime", endTime);

        int progress = getProgress(userID, eventID, date);
        jsonObject.addProperty("currentMilestone", progress);
        jsonObject.addProperty("maxMilestone", maxMilestone);
        jsonObject.add("milestones", eventRewards);
        jsonObject.addProperty("isReward", isMatchConditionToClaim(userID, date));
        return jsonObject;
    }

    private String getUserWinBattleListProgressKey(long userID) {
        return String.format(USER_WIN_BATTLE_LIST_PROGRESS_KEY, userID);
    }

    private void recordUserWinBattleProgress(String key, String value) {
        redisTemplateString.opsForList().rightPush(key, value);
        redisTemplateString.opsForList().trim(key, -8, -1);
        redisTemplateString.expire(key, Duration.ofDays(2));
    }

    public void listenEndGame(GameLog gameLog) {
        List<Long> userIDs = gameLog.getListUserID();
        long winID = gameLog.getWinID();
        for (Long userID : userIDs) {
            if (winID == userID) {
                if (!isBot(userID)) {
                    int userTrophy = leaderboardService.getProfileTrophy(userID);
                    if (userTrophy >= TROPHY_TO_UNLOCK) {
                        EventEnt eventEnt = getWinBattleEvent();
                        //get date from gamelog info
                        String dateStr = getDate(gameLog.getMatchEndAtMs(), eventEnt.getCustomData());
                        int eventID = eventEnt.getId();
                        String userWinBattleProgressKey = getUserWinBattleListProgressKey(userID);
                        recordUserWinBattleProgress(userWinBattleProgressKey, String.valueOf(dateStr));
                        String winBattleProgressField = "win_battle_progress";
                        recordDynamicField(userID, eventEnt.getId(), winBattleProgressField, 1, dateStr);
                        List<String> winDates = redisTemplateString.opsForList().range(userWinBattleProgressKey, -8, -1);
                        if (winDates != null) {
                            int milestone = (int) winDates.stream().filter(e -> e.equals(dateStr)).count();
                            boolean matchMileStone = milestone == 1 || milestone == 4 || milestone == 8;
                            if (matchMileStone) {
                                recordMilestone(milestone, userID, eventID, dateStr);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getDate(long matchEndAtMs, String customData) {
        ZoneId utcZone = ZoneId.systemDefault();
        JsonObject customDataJson = JSONUtil.DeSerialize(customData, JsonObject.class);
        int adjustHours = customDataJson.get("adjustHours").getAsInt();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(matchEndAtMs), utcZone).minusHours(adjustHours);
        Date date = DateTimeUtils.toDate(localDateTime);
        return DateTimeUtils.toString(date, "dd/MM/YYYY");
    }

    private void recordMilestone(int milestone, long userID, int eventID, String dateStr) {
        String winBattleClaimedField = "win_battle_" + milestone + "_is_claim";
        Map<String, String> mapData = getMapEventData(userID, eventID, dateStr);
        //user doesn't claim yet -> cache mystery box rare and upgrade count
        if (!mapData.containsKey(winBattleClaimedField)) {
            String mysteryBoxField = getUserMysteryBoxKey(userID, dateStr);
            int boxId = ERewardType.MYSTERY_BOX_RARE.getValue();
            redisTemplateString.opsForValue().set(mysteryBoxField, String.valueOf(boxId), 7, TimeUnit.DAYS);
            String key = getHashEventKey(userID, eventID, dateStr);
            String upgradeField = "upgrade_count";
            redisTemplateString.opsForHash().put(key, upgradeField, String.valueOf(MYSTERY_BOT_MAX_UPGRADE));
        }
        //send sse to notify client move to screen upgrade box
        sseService.emitNextMsg(sendSSEWinBattle(), userID);
    }

    private String sendSSEWinBattle() {
        JsonObject response = new JsonObject();
        response.addProperty("isReward", true);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.WIN_BATTLE);
    }

    protected String getHashEventKey(long userId, int eventID, String date) {
        return String.format(HASH_USER_WIN_BATTLE_INFO, eventID, userId, date);
    }

    public void recordDynamicField(long userId, int eventID, String field, long delta, String date) {
        String key = getHashEventKey(userId, eventID, date);
        redisTemplateString.opsForHash().increment(key, field, delta);
        redisTemplateString.expire(key, Duration.ofDays(2));
    }

    public void recordClaimField(long userId, int eventID, String field, String dateStr) {
        String key = getHashEventKey(userId, eventID, dateStr);
        redisTemplateString.opsForHash().put(key, field, String.valueOf(userId));
    }

    private String getUserMysteryBoxKey(long userID, String date) {
        return String.format(USER_MYSTERY_BOX_KEY, userID, date);
    }

    public String claimBox() {
        long userID = adminService.getSession().getId();
        String userWinBattleProgressKey = getUserWinBattleListProgressKey(userID);
        List<String> latestWinDate = redisTemplateString.opsForList().range(userWinBattleProgressKey, -1, -1);
        List<RewardEnt> rewardEnts = new ArrayList<>();
        if (latestWinDate != null) {
            String lastDateValue = latestWinDate.get(0);
            boolean isMatchConditionToClaim = isMatchConditionToClaim(userID, lastDateValue);
            if (!isMatchConditionToClaim) {
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), new JsonObject(), NetWorkAPI.UNKNOWN);
            }
            String mysteryBoxField = getUserMysteryBoxKey(userID, lastDateValue);
            String value = redisTemplateString.opsForValue().get(mysteryBoxField);
            int boxId = value == null ? -1 : Integer.parseInt(value);
            if (boxId == -1) {
                return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "Can not claim more!", NetWorkAPI.UNKNOWN);
            }
            ChestConfig config = MAP_MYSTERY_BOX_CONFIG.get(boxId);
            //random reward type
            int randRewardType = getRandRewardType(config.getChestItems());
            ERewardType eRewardType = ERewardType.findByValue(randRewardType);
            List<HeroEnt> listHero = heroService.findAll();
            List<HeroEnt> listOwnedHero = inventoryService.getListOwnedHero(userID);
            for (HeroEnt heroEnt : listOwnedHero) {
                if (listHero.stream().anyMatch(e -> e.getId() == heroEnt.getId())) {
                    heroEnt.setRarity(listHero.stream()
                            .filter(e -> e.getId() == heroEnt.getId())
                            .findFirst().get().getRarity());
                }
            }
            int ownedStatus;
            RewardEnt chestItem;
            List<RewardEnt> randomHero;
            if (eRewardType != null) {
                switch (eRewardType) {
                    case GOLD:
                        chestItem = config.getChestItems().stream().filter(e -> e.getERewardType().equals(ERewardType.GOLD)).findFirst().get();
                        rewardEnts.addAll(inventoryService.claimItem(userID,
                                ERewardType.PACK_GOLD_1,
                                chestItem.getBound(), EUpdateMoneyType.WIN_BATTLE));
                        break;
                    case EMERALD:
                        chestItem = config.getChestItems().stream().filter(e -> e.getERewardType().equals(ERewardType.EMERALD)).findFirst().get();
                        rewardEnts.addAll(inventoryService.claimItem(userID,
                                ERewardType.COMMON_EMERALD,
                                chestItem.getBound(), EUpdateMoneyType.WIN_BATTLE));
                        break;
                    case ROYAL_AMETHYST:
                        chestItem = config.getChestItems().stream().filter(e -> e.getERewardType().equals(ERewardType.ROYAL_AMETHYST)).findFirst().get();
                        rewardEnts.addAll(inventoryService.claimItem(userID,
                                ERewardType.ROYAL_AMETHYST,
                                chestItem.getBound(), EUpdateMoneyType.WIN_BATTLE));
                        break;
                    case RARE_CARD:
                        chestItem = config.getChestItems().stream().filter(e -> e.getERewardType().equals(ERewardType.RARE_CARD)).findFirst().get();
                        ownedStatus = getRandHeroCardType(chestItem);
                        randomHero = randomHero(listHero,listOwnedHero,EHeroRarity.MYTHIC,chestItem,ownedStatus,userID);
                        rewardEnts.addAll(randomHero);
                        break;
                    case EPIC_CARD:
                        chestItem = config.getChestItems().stream().filter(e -> e.getERewardType().equals(ERewardType.EPIC_CARD)).findFirst().get();
                        ownedStatus = getRandHeroCardType(chestItem);
                        randomHero = randomHero(listHero,listOwnedHero,EHeroRarity.EPIC,chestItem,ownedStatus,userID);
                        rewardEnts.addAll(randomHero);
                        break;
                }
                recordClaimMilestone(userID, lastDateValue, mysteryBoxField);
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), new JsonObject(), JsonBuilder.buildListReward(rewardEnts), -1).toString();
    }

    private void recordClaimMilestone(long userID, String lastDateValue, String mysteryBoxField) {
        EventEnt eventEnt = getWinBattleEvent();
        int eventID = eventEnt.getId();
        int progress = getProgress(userID, eventID, lastDateValue);
        String winBattleClaimedField = "win_battle_" + progress + "_is_claim";
        recordClaimField(userID, eventID, winBattleClaimedField, lastDateValue);
        redisTemplateString.delete(mysteryBoxField);
    }

    private EventEnt getWinBattleEvent() {
        Optional<EventEnt> optional = eventRepository.read().findByType(EEventType.WIN_BATTLE.getValue()).stream().findFirst();
        return optional.get();
    }

    public Map<String, String> getMapEventData(long userId, int eventID, String dateStr) {
        String key = getHashEventKey(userId, eventID, dateStr);
        if (key.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        HashOperations<String, String, String> hashOperations = redisTemplateString.opsForHash();
        return hashOperations.entries(key);
    }

    private boolean isMatchConditionToClaim(long userID, String dateStr) {
        EventEnt eventEnt = getWinBattleEvent();
        boolean isMatchConditionToClaim = false;
        int eventID = eventEnt.getId();
        Map<String, String> mapData = getMapEventData(userID, eventID, dateStr);
        int progress = getProgress(userID, eventID, dateStr);
        String winBattleClaimedField = "win_battle_" + progress + "_is_claim";
        boolean isMatchCondition = (progress == 1 || progress == 4 || progress == 8) && !mapData.containsKey(winBattleClaimedField);
        if (isMatchCondition) {
            isMatchConditionToClaim = true;
        }
        return isMatchConditionToClaim;
    }


    private List<RewardEnt> randomHero(List<HeroEnt> listHero,
                                       List<HeroEnt> listOwnedHero,
                                       EHeroRarity heroRarity,
                                       RewardEnt chestItem,
                                       int ownedStatus,
                                       long userID){
        int heroID;
        int quantity;
        List<HeroEnt> listHeroTemp = listHero.stream().filter(e -> e.getRarity().equals(heroRarity)).collect(Collectors.toList());
        List<Integer> listOwnedHeroID = listOwnedHero.stream()
                .filter(e -> e.getRarity().equals(heroRarity))
                .map(HeroEnt::getId)
                .collect(Collectors.toList());
        if (chestItem.getRef() != 0) {
            heroID = chestItem.getRef();
            quantity = chestItem.getBound() == chestItem.getOrgin() ? chestItem.getBound() : RandomUtils.RAND.nextInt(chestItem.getOrgin(), chestItem.getBound() + 1);
        } else if ((ownedStatus == EOwnedStatus.NOT_OWNED.ordinal() && listHeroTemp.size() > listOwnedHeroID.size()) || listOwnedHeroID.isEmpty()) {
            List<Integer> notOwnedHero = listHeroTemp.stream().map(HeroEnt::getId).filter(id -> !listOwnedHeroID.contains(id)).collect(Collectors.toList());
            heroID = notOwnedHero.get(RandomUtils.RAND.nextInt(notOwnedHero.size()));
            quantity = chestItem.getBound() == chestItem.getOrgin() ? chestItem.getBound() : RandomUtils.RAND.nextInt(chestItem.getOrgin(), chestItem.getBound() + 1);
        } else {
            heroID = listOwnedHeroID.get(RandomUtils.RAND.nextInt(listOwnedHeroID.size()));
            quantity = chestItem.getBound() == chestItem.getOrgin() ? chestItem.getBound() : RandomUtils.RAND.nextInt(chestItem.getOrgin(), chestItem.getBound() + 1);
        }
        return inventoryService.claimItem(userID,
                ERewardType.HERO_CARD,
                quantity, heroID, EUpdateMoneyType.CHEST_SHOP);
    }

    private int getRandHeroCardType(RewardEnt chestItem){
        List<RandomItem> listStatusCardRan = new ArrayList<>();
        listStatusCardRan.add(RandomItem.builder().data(EOwnedStatus.NOT_OWNED.ordinal()).percent(chestItem.getNotOwnedPercent()).build());
        listStatusCardRan.add(RandomItem.builder().data(EOwnedStatus.OWNED.ordinal()).percent(100 - chestItem.getNotOwnedPercent()).build());
        return RandomUtils.random(listStatusCardRan);
    }

    private int getRandRewardType(List<RewardEnt> chestItems){
        List<RandomItem> listStatusRewardTypeRan = new ArrayList<>();
        for (RewardEnt chestItem : chestItems) {
            listStatusRewardTypeRan.add(RandomItem.builder().data(chestItem.getERewardType().getValue()).percent(chestItem.getOwnedPercentRewardType()).build());
        }
        return RandomUtils.random(listStatusRewardTypeRan);
    }

    private int getRandMysteryBox(List<ChestConfig> chestConfigs){
        List<RandomItem> listStatusMysteryBoxRan = new ArrayList<>();
        for (ChestConfig chestItem : chestConfigs) {
            listStatusMysteryBoxRan.add(RandomItem.builder().data(chestItem.getId()).percent(chestItem.getAppearanceRate()).build());
        }
        return RandomUtils.random(listStatusMysteryBoxRan);
    }

}



