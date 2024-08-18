package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.constant.StringConstant;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.ent.enums.ETriggerType;
import cc.bliss.match3.service.gamemanager.ent.enums.EUpdateMoneyType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.GachaTrackingCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ConfigService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class GachaService extends BaseService {

    @Autowired
    private ConfigService configService;
    @Autowired
    private EventService eventService;
    @Autowired
    private AdminService adminService;
    /**
     * hash_gacha_info_{userID}
     */
    private static final int MAX_GACHA_COUNT = 50;
    public static final String HASH_USER_GACHA_INFO = "hash_gacha_info_%s";
    private static final String FIELD_PITY = "pity_%s";
    private static final String FIELD_GACHA_COUNT = "gacha_count_%s";
    private static final String FIELD_GACHA_INSURANCE = "insurance";
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private TriggerService triggerService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private HeroService heroService;
    @Autowired
    private Producer producer;
    @Autowired
    private LeaderboardService leaderboardService;
    // config
    private int DEFAULT_EPIC_PITY;
    private int DEFAULT_LEGENDARY_PITY;
    private int RESET_EPIC_PITY;
    private int RESET_LEGENDARY_PITY;

    public void refreshConfig(){
        DEFAULT_EPIC_PITY = ConvertUtils.toInt(configService.getConfig("DEFAULT_EPIC_PITY"), 1);
        DEFAULT_LEGENDARY_PITY = ConvertUtils.toInt(configService.getConfig("DEFAULT_LEGENDARY_PITY"), 1);
        RESET_EPIC_PITY = ConvertUtils.toInt(configService.getConfig("RESET_EPIC_PITY"), 2);
        RESET_LEGENDARY_PITY = ConvertUtils.toInt(configService.getConfig("RESET_LEGENDARY_PITY"), 2);
    }

    public String getRedisKey(long userID) {
        return String.format(HASH_USER_GACHA_INFO, userID);
    }

    private String getField(String format, ERewardType eRewardType) {
        return String.format(format, eRewardType.name());
    }


    private Map<String, String> getMapEventData(long userId) {
        String key = getRedisKey(userId);
        HashOperations<String, String, String> hashOperations = redisTemplateString.opsForHash();
        return hashOperations.entries(key);
    }

    private JsonObject buildEvent(EventEnt eventEnt, TriggerEnt triggerEnt) {
        JsonObject jsonObject = new JsonObject();
        JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
        int moneyType = ConvertUtils.toInt(customData.get("moneyType").getAsInt());
        int heroID = ConvertUtils.toInt(customData.get("heroID").getAsInt());
        jsonObject.addProperty("id", eventEnt.getId());
        jsonObject.addProperty("moneyType", moneyType);
        jsonObject.addProperty("thumbnail", eventEnt.getThumbnail());
        jsonObject.addProperty("title", eventEnt.getTitle());
        jsonObject.addProperty("endTime", triggerEnt.getEndTime().getTime());
        jsonObject.addProperty("heroID", heroID);
        return jsonObject;
    }

    private JsonObject buildGachaObject(EventEnt eventEnt, int gachaCount, int moneyType){
        List<EventEnt> eventEnts = new ArrayList<>();
        eventEnts.add(eventEnt);
        JsonObject jsonObject = buildGachaObject(eventEnts, gachaCount, moneyType);
        return jsonObject;
    }

    private JsonObject buildGachaObject(List<EventEnt> eventEnts, int gachaCount, int moneyType){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("gachaCount", gachaCount);
        jsonObject.addProperty("gachaMax", MAX_GACHA_COUNT);
        jsonObject.addProperty("moneyType", moneyType);
        JsonArray jsonArray = new JsonArray();
        for (EventEnt eventEnt : eventEnts) {
            TriggerEnt triggerEnt = triggerService.getTriggerByEventID(eventEnt.getId());
            jsonArray.add(buildEvent(eventEnt,triggerEnt));
        }
        jsonObject.add("listGacha", jsonArray);
        return jsonObject;
    }

    public String getListGacha() {
        SessionObj sessionObj = adminService.getSession();
        List<EventEnt> eventEnts = eventService.getListEventByTriggerType(ETriggerType.GACHA_EVENT);
        Map<String, String> mapData = getMapEventData(sessionObj.getId());
        long endTime = 0;
        List<EventEnt> eventAmethyst = new ArrayList<>();
        List<EventEnt> eventRoyalAmethyst = new ArrayList<>();
        for (EventEnt eventEnt : eventEnts) {
            JsonObject customData = JSONUtil.DeSerialize(eventEnt.getCustomData(), JsonObject.class);
            int moneyType = ConvertUtils.toInt(customData.get("moneyType").getAsInt());
            if (moneyType == ERewardType.AMETHYST.getValue()){
                eventAmethyst.add(eventEnt);
            } else if (moneyType == ERewardType.ROYAL_AMETHYST.getValue()){
                TriggerEnt triggerEnt = triggerService.getTriggerByEventID(eventEnt.getId());
                endTime = triggerEnt.getEndTime().getTime();
                eventRoyalAmethyst.add(eventEnt);
            }
        }
        JsonObject gachaAmethyst = buildGachaObject(eventAmethyst,
                ConvertUtils.toInt(mapData.get(getField(FIELD_GACHA_COUNT, ERewardType.AMETHYST))),
                ERewardType.AMETHYST.getValue());
        JsonObject gachaRoyalAmethyst = buildGachaObject(eventRoyalAmethyst,
                ConvertUtils.toInt(mapData.get(getField(FIELD_GACHA_COUNT, ERewardType.ROYAL_AMETHYST))),
                ERewardType.ROYAL_AMETHYST.getValue());
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("gachaAmethyst", gachaAmethyst);
        jsonObject.add("gachaRoyalAmethyst", gachaRoyalAmethyst);
        jsonObject.addProperty("endTime", endTime);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.GET_LIST_GACHA);
    }

    public synchronized String gacha(HttpServletRequest httpServletRequest) {
        JsonObject req = RequestUtils.requestToJson(httpServletRequest);
        int gachaID = req.get("gachaID").getAsInt();
        int quantity = req.get("quantity").getAsInt();
        boolean isUseEmerald = req.get("useEmerald").getAsBoolean();
        // validate
        if (quantity != 1 && quantity != 10) {
            return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.GACHA);
        }
        SessionObj sessionObj = adminService.getSession();
        List<EventEnt> eventEnts = eventService.getListEventByTriggerType(ETriggerType.GACHA_EVENT);
        EventEnt gachaEvent = null;
        List<RewardEnt> currency = Collections.EMPTY_LIST;
        for (EventEnt eventEnt : eventEnts) {
            if (eventEnt.getId() == gachaID) {
                gachaEvent = eventEnt;
                break;
            }
        }
        if (gachaEvent == null) {
            return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.GACHA);
        }
        Map<String, String> mapData = getMapEventData(sessionObj.getId());
        Profile profile = profileService.getMinProfileByID(sessionObj.getId());
        JsonObject customData = JSONUtil.DeSerialize(gachaEvent.getCustomData(), JsonObject.class);
        int moneyType = ConvertUtils.toInt(customData.get("moneyType").getAsInt());
        String gachaCountField = getField(FIELD_GACHA_COUNT, ERewardType.findByValue(moneyType));
        int gachaCount = ConvertUtils.toInt(mapData.get(gachaCountField));
        String epicPityField = getField(FIELD_PITY, ERewardType.EPIC_CARD);
        int epicPity = ConvertUtils.toInt(mapData.get(epicPityField), DEFAULT_EPIC_PITY);
        String legendaryPityField = getField(FIELD_PITY, ERewardType.LEGENDARY_CARD);
        int legendaryPity = ConvertUtils.toInt(mapData.get(legendaryPityField), DEFAULT_LEGENDARY_PITY);

        String legendaryInsuranceField = getField(FIELD_GACHA_INSURANCE, ERewardType.LEGENDARY_CARD);
        String epicInsuranceField = getField(FIELD_GACHA_INSURANCE, ERewardType.EPIC_CARD);
        int legendaryInsurance = ConvertUtils.toInt(mapData.get(getField(FIELD_GACHA_INSURANCE,  ERewardType.LEGENDARY_CARD)), 0);
        int epicInsurance = ConvertUtils.toInt(mapData.get(getField(FIELD_GACHA_INSURANCE,  ERewardType.EPIC_CARD)), 0);
        String redisKey = getRedisKey(sessionObj.getId());
        HashOperations<String, String, String> hashOperations = redisTemplateString.opsForHash();
        Map<Integer, List<Integer>> mapHeroByRarity = getMapHeroByRarity(customData);
        Map<Integer, List<Integer>> mapAllHeroByRarity = heroService.getMapAllHeroByRarity();
        Map<Integer, Integer> mapPityEpic = configService.getPityConfig("PITY_EPIC_CONFIG");
        Map<Integer, Integer> mapPityLegend = configService.getPityConfig("PITY_LEGENDARY_CONFIG");
        int maxEpicPity = getMaxPityPercent(mapPityEpic);
        int maxLegendPity = getMaxPityPercent(mapPityLegend);
        // init data
        if (!mapData.containsKey(epicPityField)){
            redisTemplateString.opsForHash().put(redisKey, epicPityField, String.valueOf(DEFAULT_EPIC_PITY));
        }
        if (!mapData.containsKey(legendaryPity)){
            redisTemplateString.opsForHash().put(redisKey, legendaryPityField, String.valueOf(DEFAULT_LEGENDARY_PITY));
        }
        // check ,charge currency
        long delta = 0;
        if (isUseEmerald) {
            delta = quantity * 160;
            if (profile.getEmerald() < delta){
                return ResponseUtils.toErrorBody(StringConstant.COMMON_NOT_ENOUGH_MONEY, NetWorkAPI.GACHA);
            }
            currency = inventoryService.claimItem(sessionObj.getId(), ERewardType.EMERALD, -delta, EUpdateMoneyType.GACHA);
        } else {
            if ((moneyType == ERewardType.AMETHYST.getValue() && profile.getAmethyst() < quantity)
            || (moneyType == ERewardType.ROYAL_AMETHYST.getValue() && profile.getRoyalAmethyst() < quantity)){
                return ResponseUtils.toErrorBody(StringConstant.COMMON_NOT_ENOUGH_MONEY, NetWorkAPI.GACHA);
            }
            currency = inventoryService.claimItem(sessionObj.getId(), ERewardType.findByValue(moneyType), -quantity, EUpdateMoneyType.GACHA);
        }
        // logic gacha
        List<RewardEnt> rewardEnts = new ArrayList<>();
        // init variable
        int heroID = 0;
        int mainLegendHero = ConvertUtils.toInt(customData.get("heroID").getAsInt());
        // check count
        for (int i = 0; i < quantity; i++) {
            List<Integer> listLegendCard = legendaryInsurance != 0
                    ? mapHeroByRarity.get(ERewardType.LEGENDARY_CARD.getValue())
                    : mapAllHeroByRarity.get(ERewardType.LEGENDARY_CARD.getValue());
            List<Integer> listEpicCard = epicInsurance != 0
                    ? mapHeroByRarity.get(ERewardType.EPIC_CARD.getValue())
                    : mapAllHeroByRarity.get(ERewardType.EPIC_CARD.getValue());
            List<Integer> listRareCard = mapHeroByRarity.get(ERewardType.RARE_CARD.getValue());
            if (gachaCount == 49) {
                // trường hợp gacha ra legend thì 50% ra main hero, 50% ra các hero còn lại
                boolean isGachaMainHero = RandomUtils.RAND.nextBoolean();
                heroID = listLegendCard.get(RandomUtils.RAND.nextInt(listLegendCard.size()));

                redisTemplateString.opsForHash().put(redisKey, gachaCountField, String.valueOf(0));
                redisTemplateString.opsForHash().put(redisKey, legendaryPityField, String.valueOf(RESET_LEGENDARY_PITY));
                gachaCount = 0;
                legendaryPity = 1;
                if (!mapHeroByRarity.get(ERewardType.LEGENDARY_CARD.getValue()).contains(heroID)){
                    legendaryInsurance = redisTemplateString.opsForHash().increment(redisKey, legendaryInsuranceField, 1).intValue();
                }
            } else if (gachaCount == 9 || gachaCount == 19 || gachaCount == 29 || gachaCount == 39) {
                heroID = listEpicCard.get(RandomUtils.RAND.nextInt(listEpicCard.size()));

                gachaCount = redisTemplateString.opsForHash().increment(redisKey, gachaCountField, 1).intValue();
                legendaryPity = redisTemplateString.opsForHash().increment(redisKey, legendaryPityField, 1).intValue();
                if (!mapHeroByRarity.get(ERewardType.EPIC_CARD.getValue()).contains(heroID)){
                    epicInsurance = redisTemplateString.opsForHash().increment(redisKey, epicInsuranceField, 1).intValue();
                }
            } else {
                // check pity
                heroID = getRandomHeroByPity(epicPity, mapPityEpic, legendaryPity, mapPityLegend, mapHeroByRarity, maxEpicPity, maxLegendPity, mainLegendHero);
                if (listLegendCard.contains(heroID)) {
                    legendaryPity = RESET_LEGENDARY_PITY;
                    redisTemplateString.opsForHash().put(redisKey, legendaryPityField, String.valueOf(RESET_LEGENDARY_PITY));
                }
                if (listEpicCard.contains(heroID)) {
                    epicPity = RESET_EPIC_PITY;
                    redisTemplateString.opsForHash().put(redisKey, epicPityField, String.valueOf(RESET_EPIC_PITY));
                }
                if (listRareCard.contains(heroID)) {
                    epicPity++;
                    legendaryPity++;
                    redisTemplateString.opsForHash().increment(redisKey, epicPityField, 1);
                    redisTemplateString.opsForHash().increment(redisKey, legendaryPityField, 1);
                }
                gachaCount = redisTemplateString.opsForHash().increment(redisKey, gachaCountField, 1).intValue();
            }
            if (!mapHeroByRarity.get(ERewardType.EPIC_CARD.getValue()).contains(heroID)){
                epicInsurance = redisTemplateString.opsForHash().increment(redisKey, epicInsuranceField, 1).intValue();
            } else {
                epicInsurance = 0;
                redisTemplateString.opsForHash().delete(redisKey, epicInsuranceField);
            }
            if (!mapHeroByRarity.get(ERewardType.LEGENDARY_CARD.getValue()).contains(heroID)){
                legendaryInsurance = redisTemplateString.opsForHash().increment(redisKey, legendaryInsuranceField, 1).intValue();
            } else {
                legendaryInsurance = 0;
                redisTemplateString.opsForHash().delete(redisKey, legendaryInsuranceField);
            }
            int quantityReward = getGachaQuantity(mapHeroByRarity, heroID);
            rewardEnts.addAll(inventoryService.claimItem(sessionObj.getId(), ERewardType.HERO_CARD, quantityReward, heroID, EUpdateMoneyType.GACHA));
            gachaCount++;
        }

        // end logic gacha
        TriggerEnt triggerEnt = triggerService.getTriggerByEventID(gachaEvent.getId());
        JsonObject jsonObject = buildGachaObject(gachaEvent, gachaCount, moneyType);
        JsonArray jsonArray = JsonBuilder.buildListReward(rewardEnts);
        jsonObject.add("reward", jsonArray);
        JsonArray currencyArr = new JsonArray();
        if (currency.size() >= 1){
            currencyArr.add(JsonBuilder.buildSSEUpdateMoneyJson(currency.get(0)));
        }
        int userTrophy = leaderboardService.getProfileTrophy(profile.getId());
        JsonArray data = buildReceiveItemName(jsonArray);
        long startSeasonTime = triggerEnt.getStartTime().getTime();
        long endSeasonTime = triggerEnt.getStartTime().getTime();
        GMLocalQueue.addQueue(new GachaTrackingCmd(producer, profile, userTrophy, gachaEvent, quantity, isUseEmerald, delta, moneyType, data, startSeasonTime, endSeasonTime, redisTemplateString));
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, currencyArr, NetWorkAPI.GACHA).toString();
    }

    //only use for tracking data
    private JsonArray buildReceiveItemName(JsonArray data) {
        for (JsonElement jsonElement : data) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String title = jsonObject.get("title").getAsString();
            int rewardType = jsonObject.get("rewardType").getAsInt();
            if (ERewardType.HERO_CARD.getValue() == rewardType) {
                String receiveItemName = title + " " + heroService.getHero(jsonObject.get("refID").getAsInt()).getTitle();
                jsonObject.addProperty("receiveItemName", receiveItemName);
            }
        }
        return data;
    }

    private int getGachaQuantity(Map<Integer, List<Integer>> mapHeroByRarity, int heroID){
        if (mapHeroByRarity.get(ERewardType.LEGENDARY_CARD.getValue()).contains(heroID)){
            return GameConstant.GACHA_LEGENDARY_QUANTITY;
        } else if (mapHeroByRarity.get(ERewardType.EPIC_CARD.getValue()).contains(heroID)){
            return GameConstant.GACHA_EPIC_QUANTITY;
        } else {
            return GameConstant.GACHA_RARE_QUANTITY;
        }
    }

    private int getMaxPityPercent(Map<Integer, Integer> mapPity){
        int pity = 1;
        for (Integer i : mapPity.keySet()) {
            if (i > pity){
                pity = 1;
            }
        }
        return mapPity.get(pity);
    }

    private List<RandomItem> buildRandomRarityItem(int epicPity,
                                                   Map<Integer, Integer> mapPityEpic,
                                                   int legendaryPity,
                                                   Map<Integer, Integer> mapPityLegend,
                                                   int maxEpicPity,
                                                   int maxLegendPity) {
        int maxPercent = 10000;
        int epicPercent = mapPityEpic.getOrDefault(epicPity, maxEpicPity);
        int legendPercent = mapPityLegend.getOrDefault(legendaryPity, maxLegendPity);
        int rarePercent = maxPercent - epicPercent - legendPercent;
        List<RandomItem> randomItems = new ArrayList<>();
        randomItems.add(RandomItem.builder().data(ERewardType.RARE_CARD.getValue()).percent(rarePercent).build());
        randomItems.add(RandomItem.builder().data(ERewardType.EPIC_CARD.getValue()).percent(epicPercent).build());
        randomItems.add(RandomItem.builder().data(ERewardType.LEGENDARY_CARD.getValue()).percent(legendPercent).build());
        return randomItems;
    }

    private int getRandomHeroByPity(int epicPity,
                                    Map<Integer, Integer> mapPityEpic,
                                    int legendaryPity,
                                    Map<Integer, Integer> mapPityLegend,
                                    Map<Integer, List<Integer>> mapHeroByRarity,
                                    int maxEpicPity,
                                    int maxLegendPity,
                                    int mainLegendHero) {
        List<RandomItem> randomRarityItems = buildRandomRarityItem(epicPity, mapPityEpic, legendaryPity, mapPityLegend, maxEpicPity, maxLegendPity);
        int rarity = RandomUtils.random(randomRarityItems);
        List<Integer> listHero = mapHeroByRarity.get(rarity);
        if (rarity == ERewardType.LEGENDARY_CARD.getValue()){
            // trường hợp gacha ra legend thì 50% ra main hero, 50% ra các hero còn lại
            boolean isGachaMainHero = RandomUtils.RAND.nextBoolean();
            if (isGachaMainHero){
                return mainLegendHero;
            } else {
                List<Integer> listRemainLegendHero = new ArrayList<>();
                for (Integer hero : listHero) {
                    if (hero != mainLegendHero)
                        listRemainLegendHero.add(hero);
                }
                return listRemainLegendHero.get(RandomUtils.RAND.nextInt(listRemainLegendHero.size()));
            }
        }
        return listHero.get(RandomUtils.RAND.nextInt(listHero.size()));
    }

    private Map<Integer, List<Integer>> getMapHeroByRarity(JsonObject customData) {
        Map<Integer, List<Integer>> heroMap = new HashMap<>();
        JsonArray configArr = customData.get("heroConfig").getAsJsonArray();
        for (JsonElement jsonElement : configArr) {
            JsonObject config = jsonElement.getAsJsonObject();
            JsonArray heroConfig = config.get("heroConfig").getAsJsonArray();

            int rarity = config.get("rarity").getAsInt();
            List<Integer> heroList = new ArrayList<>();
            for (JsonElement element : heroConfig) {
                int heroID = element.getAsInt();
                heroList.add(heroID);
            }

            heroMap.put(rarity, heroList);
        }
        return heroMap;
    }
}
