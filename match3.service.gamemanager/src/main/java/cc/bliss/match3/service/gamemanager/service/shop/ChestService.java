/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.shop;

import bliss.lib.framework.util.ConvertUtils;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.ChestConfig;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ShopTrackingCmd;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.system.GameLogService;
import cc.bliss.match3.service.gamemanager.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class ChestService extends DailyOfferService {

    private static final Map<Integer, ChestConfig> MAP_CHEST_CONFIG = new NonBlockingHashMap<>();
    private static final String HASH_CHEST = "h_chest_%s";
    private static final String FIELD_AD_CHEST_WATCHED = "ad_chest_watched";
    private static final String FIELD_AD_CHEST_NEXT = "ad_chest_next";
    private static final int AD_WATCH_REQUIRE = 5;
    private static final Map<Long, Long> MAP_CHECK_CLAIM_MATCH_RESULT_TIME = new NonBlockingHashMap<>();

    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private GameLogService gameLogService;
    @Autowired
    private DailyQuestService dailyQuestService;
    static {
        {
            // Beginner chest
            ChestConfig config = new ChestConfig();
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(100).orgin(100).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(10).orgin(10).ref(2).build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.NONE.getValue());
            MAP_CHEST_CONFIG.put(ERewardType.BEGINNER_CHEST.getValue(), config);
        }
        {
            //WoodenChest
            ChestConfig config = new ChestConfig();
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(100).orgin(100).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(3).orgin(3)
                        .notOwnedPercent(10).build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.NONE.getValue());
            MAP_CHEST_CONFIG.put(ERewardType.WOODEN_CHEST.getValue(), config);
        }
        {
            //SLIVER_CHEST
            ChestConfig config = new ChestConfig();
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(600).orgin(600).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(5).orgin(5)
                        .notOwnedPercent(20).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(5).orgin(5)
                        .notOwnedPercent(20).build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.EMERALD.getValue());
            config.setPrice(390);
            MAP_CHEST_CONFIG.put(ERewardType.SLIVER_CHEST.getValue(), config);
        }
        {
            //GOLDEN_CHEST
            ChestConfig config = new ChestConfig();
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(1000).orgin(1000).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(20).orgin(20)
                        .notOwnedPercent(20).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(20).orgin(20)
                        .notOwnedPercent(20).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EPIC_CARD).bound(3).orgin(3)
                        .notOwnedPercent(30).build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.EMERALD.getValue());
            config.setPrice(1980);
            MAP_CHEST_CONFIG.put(ERewardType.GOLDEN_CHEST.getValue(), config);
        }
        {
            //SHINY_JEWEL_CHEST
            ChestConfig config = new ChestConfig();
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(4500).orgin(4500).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(40).orgin(40)
                        .notOwnedPercent(20).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(40).orgin(40)
                        .notOwnedPercent(20).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EPIC_CARD).bound(5).orgin(5)
                        .notOwnedPercent(30).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.EPIC_CARD).bound(5).orgin(5)
                        .notOwnedPercent(30).build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.EMERALD.getValue());
            config.setPrice(5980);
            MAP_CHEST_CONFIG.put(ERewardType.SHINY_JEWEL_CHEST.getValue(), config);
        }
        {
            //ADS_CHEST
            ChestConfig config = new ChestConfig();
            List<RewardEnt> chestSlot = new ArrayList<>();
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.GOLD).bound(500).orgin(500).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(5).orgin(5)
                        .notOwnedPercent(5).build();
                chestSlot.add(rewardEnt);
            }
            {
                RewardEnt rewardEnt = RewardEnt.builder()
                        .eRewardType(ERewardType.RARE_CARD).bound(5).orgin(5)
                        .notOwnedPercent(5).build();
                chestSlot.add(rewardEnt);
            }
            config.setChestItems(chestSlot);
            config.setMoneyType(ERewardType.NONE.getValue());
            MAP_CHEST_CONFIG.put(ERewardType.ADS_CHEST.getValue(), config);
        }
        //VALOR CHEST
        ChestConfig config = new ChestConfig();
        List<RewardEnt> chestSlot = new ArrayList<>();
        {
            RewardEnt rewardEnt = RewardEnt.builder()
                    .eRewardType(ERewardType.GOLD).bound(100).orgin(100).build();
            chestSlot.add(rewardEnt);
        }
        {
            RewardEnt rewardEnt = RewardEnt.builder()
                    .eRewardType(ERewardType.RARE_CARD).bound(10).orgin(10)
                    .notOwnedPercent(40)
                    .build();
            chestSlot.add(rewardEnt);
        }
        config.setChestItems(chestSlot);
        config.setMoneyType(ERewardType.NONE.getValue());
        MAP_CHEST_CONFIG.put(ERewardType.VALOR_CHEST.getValue(), config);
    }

    @Override
    public JsonObject getSectionInfo(long userID) {
        Map<String, String> mapData = getMapData(userID);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", EShopSection.CHEST.name());
        jsonObject.addProperty("remainTime", 0);
        jsonObject.addProperty("adsButton", Boolean.FALSE);
        jsonObject.addProperty("adsCountDown", 0);
        jsonObject.addProperty("section", EShopSection.CHEST.getValue());

        JsonArray listCard = new JsonArray();
        {
            JsonObject card = new JsonObject();
            int id = ERewardType.SLIVER_CHEST.getValue();
            ChestConfig chestConfig = MAP_CHEST_CONFIG.get(id);
            card.addProperty("id", id);
            card.addProperty("title", "Silver Chest");
            card.addProperty("rewardType", id);
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", 1);
            card.addProperty("rewardTag", "");

            card.addProperty("heroLevel", 0);
            card.addProperty("heroCardOwned", 0);
            card.addProperty("heroCardRequire", 0);
            card.addProperty("heroTrophy", 0);

            card.addProperty("moneyType", chestConfig.getMoneyType());
            card.addProperty("moneyRequire", chestConfig.getPrice());
            card.addProperty("cardStatus", EShopCardStatus.ACTIVE.ordinal());
            listCard.add(card);
        }
        {
            JsonObject card = new JsonObject();
            int id = ERewardType.GOLDEN_CHEST.getValue();
            ChestConfig chestConfig = MAP_CHEST_CONFIG.get(id);
            card.addProperty("id", id);
            card.addProperty("title", "Golden Chest");
            card.addProperty("rewardType", id);
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", 1);
            card.addProperty("rewardTag", "");

            card.addProperty("heroLevel", 0);
            card.addProperty("heroCardOwned", 0);
            card.addProperty("heroCardRequire", 0);
            card.addProperty("heroTrophy", 0);

            card.addProperty("moneyType", chestConfig.getMoneyType());
            card.addProperty("moneyRequire", chestConfig.getPrice());
            card.addProperty("cardStatus", EShopCardStatus.ACTIVE.ordinal());
            listCard.add(card);
        }
        {
            JsonObject card = new JsonObject();
            int id = ERewardType.SHINY_JEWEL_CHEST.getValue();
            ChestConfig chestConfig = MAP_CHEST_CONFIG.get(id);
            card.addProperty("id", id);
            card.addProperty("title", "Shiny Jewel Chest");
            card.addProperty("rewardType", id);
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", 1);
            card.addProperty("rewardTag", "");

            card.addProperty("heroLevel", 0);
            card.addProperty("heroCardOwned", 0);
            card.addProperty("heroCardRequire", 0);
            card.addProperty("heroTrophy", 0);

            card.addProperty("moneyType", chestConfig.getMoneyType());
            card.addProperty("moneyRequire", chestConfig.getPrice());
            card.addProperty("cardStatus", EShopCardStatus.ACTIVE.ordinal());
            listCard.add(card);
        }
        jsonObject.add("listCard", listCard);
        JsonArray listBigCard = new JsonArray();
        {
            JsonObject card = new JsonObject();
            int id = ERewardType.ADS_CHEST.getValue();
            card.addProperty("id", id);
            card.addProperty("title", "SHOP_ADS_CHEST_NAME");
            card.addProperty("description", "SHOP_ADS_CHEST_INFOR");
            card.addProperty("rewardType", id);
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", 1);
            card.addProperty("rewardTag", "");

            card.addProperty("adsButton", Boolean.TRUE);
            card.addProperty("adsProgress", ConvertUtils.toInt(mapData.get(FIELD_AD_CHEST_WATCHED)));
            card.addProperty("adsRequire", AD_WATCH_REQUIRE);
            card.addProperty("adsCountDown", ConvertUtils.toLong(mapData.get(FIELD_AD_CHEST_NEXT)));
            listBigCard.add(card);
        }
        jsonObject.add("listBigCard", listBigCard);
        return jsonObject;
    }

    @Override
    public String buyShop(int id, long userID) {
        List<RewardEnt> list = new ArrayList<>();
        Map<String, String> mapData = getMapData(userID);
        List<HeroEnt> listHero = heroService.findAll();
        List<HeroEnt> listOwnedHero = inventoryService.getListOwnedHero(userID);
        List<RewardEnt> currency = new ArrayList<>();
        for (HeroEnt heroEnt : listOwnedHero) {
            if (listHero.stream().anyMatch(e -> e.getId() == heroEnt.getId())) {
                heroEnt.setRarity(listHero.stream()
                        .filter(e -> e.getId() == heroEnt.getId())
                        .findFirst().get().getRarity());
            }
        }
        ChestConfig chestConfig = MAP_CHEST_CONFIG.get(id);
        Profile profile = profileService.getMinProfileByID(userID);
        long goldBeforeAction = profile.getMoney();
        long emeraldBeforeAction = profile.getEmerald();
        if (id == ERewardType.ADS_CHEST.getValue()) {
            int curAdWatched = ConvertUtils.toInt(mapData.get(FIELD_AD_CHEST_WATCHED));
            long adNextClaim = ConvertUtils.toLong(mapData.get(FIELD_AD_CHEST_NEXT));
            if (adNextClaim > System.currentTimeMillis()){
                return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
            }
            if (curAdWatched >= AD_WATCH_REQUIRE) {
                list.addAll(randomReward(userID, chestConfig, listHero, listOwnedHero));
                hashOperations.delete(getRedisKey(userID), FIELD_AD_CHEST_WATCHED);
            } else {
                list.addAll(inventoryService.claimItem(userID,
                        ERewardType.GOLD,
                        100, EUpdateMoneyType.CHEST_SHOP));
                hashOperations.increment(getRedisKey(userID), FIELD_AD_CHEST_WATCHED, 1);
                GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, userID, EQuestType.WATCH_ADS_QUEST, 1));
            }
            hashOperations.put(getRedisKey(userID), FIELD_AD_CHEST_NEXT, String.valueOf(System.currentTimeMillis() + Duration.ofHours(4).toMillis()));
            JsonArray currencyArr = new JsonArray();
            if (currency.size() >= 1){
                currencyArr.add(JsonBuilder.buildSSEUpdateMoneyJson(currency.get(0)));
            }
            sendChestMessage("SHOP_ADS_CHEST_NAME", id, userID, profile, chestConfig, list, "FREE", "WATCH_ADS", goldBeforeAction, emeraldBeforeAction);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(),
                    getSectionInfo(userID),
                    JsonBuilder.buildListReward(list),
                    list.size() == 1 ? 0 : id,
                    currencyArr
            ).toString();
        } else {
            if (chestConfig.getMoneyType() == ERewardType.EMERALD.getValue()) {
                if (profile.getEmerald() < chestConfig.getPrice()) {
                    return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
                }
                currency = inventoryService.claimItem(userID, ERewardType.findByValue(chestConfig.getMoneyType()), -chestConfig.getPrice(), EUpdateMoneyType.CHEST_SHOP);
            } else if (chestConfig.getMoneyType() == ERewardType.GOLD.getValue()) {
                if (profile.getMoney() < chestConfig.getPrice()) {
                    return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
                }
                currency = inventoryService.claimItem(userID, ERewardType.findByValue(chestConfig.getMoneyType()), -chestConfig.getPrice(), EUpdateMoneyType.CHEST_SHOP);
            } else {
                return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
            }
            list.addAll(randomReward(userID, chestConfig, listHero, listOwnedHero));
            JsonArray currencyArr = new JsonArray();
            if (currency.size() >= 1){
                currencyArr.add(JsonBuilder.buildSSEUpdateMoneyJson(currency.get(0)));
            }
            String itemName = ERewardType.findByValue(id).name();
            sendChestMessage(itemName, id, userID, profile, chestConfig, list, "EMERALD", "BUY_ITEM", goldBeforeAction, emeraldBeforeAction);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(),
                    getSectionInfo(userID),
                    JsonBuilder.buildListReward(list),
                    id,
                    currencyArr
            ).toString();
        }
    }

    @Override
    protected String getRedisKey(long userID) {
        return String.format(HASH_CHEST, userID);
    }

    //only use to send message to rabbitmq, consider when reuse this function
    private void sendChestMessage(String itemName, int itemId, long userID, Profile profile, ChestConfig chestConfig, List<RewardEnt> rewardEnts,
                                  String currency, String actionType, long goldBeforeAction, long emeraldBeforeAction) {
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        long price = 0;
        if (chestConfig != null) {
            price = chestConfig.getPrice();
        }
        JsonArray jsonArray = new JsonArray();
        for (RewardEnt item : rewardEnts) {
            JsonObject jsonObject = new JsonObject();
            String title;
            if (item.getERewardType().equals(ERewardType.HERO_CARD)) {
                String heroName = heroService.getHero(item.getRef()).getTitle();
                title = item.getTitle() + " " + heroName;
            } else {
                title = item.getTitle();
            }
            jsonObject.addProperty("item_receive_id", item.getERewardType().getValue());
            jsonObject.addProperty("item_receive_amount", item.getDelta());
            jsonObject.addProperty("item_receive_name", title);
            jsonArray.add(jsonObject);
        }
        GMLocalQueue.addQueue(new ShopTrackingCmd(producer, profile, userTrophy, price, itemName,
                itemId, jsonArray, actionType, "CHEST", itemId, currency, goldBeforeAction, emeraldBeforeAction, redisTemplateString));
    }

    public String watchAds(long userID) {
        int id = ERewardType.ADS_CHEST.getValue();
        return buyShop(id, userID);
    }

    public List<RewardEnt> randomChestReward(long userID, int chestID){
        List<HeroEnt> listHero = heroService.findAll();
        List<HeroEnt> listOwnedHero = inventoryService.getListOwnedHero(userID);
        ChestConfig chestConfig = MAP_CHEST_CONFIG.get(chestID);
        for (HeroEnt heroEnt : listOwnedHero) {
            if (listHero.stream().anyMatch(e -> e.getId() == heroEnt.getId())){
                heroEnt.setRarity(listHero.stream()
                        .filter(e -> e.getId() == heroEnt.getId())
                        .findFirst().get().getRarity());
            }
        }
        return randomReward(userID, chestConfig, listHero, listOwnedHero);
    }

    /**
     * @param chestItem
     * @return random OWNED/NOT_OWNED hero
     */
    private int getRandType(RewardEnt chestItem){
        List<RandomItem> listStatusCardRan = new ArrayList<>();
        listStatusCardRan.add(RandomItem.builder().data(EOwnedStatus.NOT_OWNED.ordinal()).percent(chestItem.getNotOwnedPercent()).build());
        listStatusCardRan.add(RandomItem.builder().data(EOwnedStatus.OWNED.ordinal()).percent(100 - chestItem.getNotOwnedPercent()).build());
        return RandomUtils.random(listStatusCardRan);
    }

    /**
     * @param listHero
     * @param listOwnedHero
     * @param heroRarity
     * @param chestItem
     * @param ownedStatus
     * @param userID
     * @return random hero by config
     */
    private List<RewardEnt> randomHero(List<HeroEnt> listHero,
                                       List<HeroEnt> listOwnedHero,
                                       EHeroRarity heroRarity,
                                       RewardEnt chestItem,
                                       int ownedStatus,
                                       long userID){
        int heroID = 1;
        int quantity = chestItem.getBound();
        List<HeroEnt> listHeroTemp = listHero.stream().filter(e -> e.getRarity().equals(heroRarity)).collect(Collectors.toList());
        List<Integer> listOwnedHeroID = listOwnedHero.stream()
                .filter(e -> e.getRarity().equals(heroRarity))
                .map(e -> e.getId())
                .collect(Collectors.toList());
        if (chestItem.getRef() != 0) {
            heroID = chestItem.getRef();
            quantity = chestItem.getBound() == chestItem.getOrgin() ? chestItem.getBound() : RandomUtils.RAND.nextInt(chestItem.getOrgin(), chestItem.getBound() + 1);
        } else if ((ownedStatus == EOwnedStatus.NOT_OWNED.ordinal() && listHeroTemp.size() > listOwnedHeroID.size()) || listOwnedHeroID.size() == 0) {
            List<Integer> notOwnedHero = listHeroTemp.stream().filter(e -> !listOwnedHeroID.contains(e.getId())).map(e -> e.getId()).collect(Collectors.toList());
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

    private List<RewardEnt> randomReward(long userID, ChestConfig chestConfig,
                                         List<HeroEnt> listHero, List<HeroEnt> listOwnedHero) {

        List<RewardEnt> rewardEnts = new ArrayList<>();
        for (int i = 0; i < listOwnedHero.size(); i++) {
            HeroEnt heroEnt = listOwnedHero.get(i);
            if (!listHero.stream().anyMatch(e -> e.getId() == heroEnt.getId())){
                listOwnedHero.remove(i);
                i--;
            }
        }
        for (RewardEnt chestItem : chestConfig.getChestItems()) {
            int ownedStatus = getRandType(chestItem);
            switch (chestItem.getERewardType()) {
                case GOLD: {
                    if (chestItem.getBound() == chestItem.getOrgin()) {
                        rewardEnts.addAll(inventoryService.claimItem(userID,
                                ERewardType.PACK_GOLD_1,
                                chestItem.getBound(), EUpdateMoneyType.CHEST_SHOP));
                    }
                }
                break;
                case RARE_CARD: {
                    List<RewardEnt> randomHero = randomHero(listHero,listOwnedHero,EHeroRarity.MYTHIC,chestItem,ownedStatus,userID);
                    rewardEnts.addAll(randomHero);
                }
                break;
                case EPIC_CARD: {
                    List<RewardEnt> randomHero = randomHero(listHero,listOwnedHero,EHeroRarity.EPIC,chestItem,ownedStatus,userID);
                    rewardEnts.addAll(randomHero);
                }
                break;
                case LEGENDARY_CARD: {
                    List<RewardEnt> randomHero = randomHero(listHero,listOwnedHero,EHeroRarity.LEGENDARY,chestItem,ownedStatus,userID);
                    rewardEnts.addAll(randomHero);
                }
                break;
            }
        }
        return rewardEnts;
    }

    public String getChestInfo(int id) {
        JsonObject jsonObject = new JsonObject();
        List<RewardEnt> collapseReward = new ArrayList<>();
        ChestConfig chestConfig = MAP_CHEST_CONFIG.get(id);
        if (chestConfig != null) {
            for (RewardEnt chestItem : chestConfig.getChestItems()) {
                Optional<RewardEnt> optional = collapseReward.stream().filter(e -> e.getERewardType().equals(chestItem.getERewardType())).findFirst();
                if (optional.isPresent()) {
                    optional.get().setOrgin(optional.get().getOrgin() + chestItem.getOrgin());
                    optional.get().setBound(optional.get().getBound() + chestItem.getBound());
                } else {
                    RewardEnt rewardEnt = new RewardEnt();
                    rewardEnt.setERewardType(chestItem.getERewardType());
                    rewardEnt.setOrgin(chestItem.getOrgin());
                    rewardEnt.setBound(chestItem.getBound());
                    collapseReward.add(rewardEnt);
                }
            }
        }

        JsonArray array = new JsonArray();
        for (RewardEnt rewardEnt : collapseReward) {
            JsonObject jo = new JsonObject();
            jo.addProperty("rewardType", rewardEnt.getERewardType().getValue());
            jo.addProperty("originQuantity", rewardEnt.getOrgin());
            jo.addProperty("boundQuantity", rewardEnt.getBound());
            array.add(jo);
        }
        jsonObject.add("chestReward", array);
        jsonObject.addProperty("chestId", id);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonObject, NetWorkAPI.CHEST_INFO);
    }

    //for match result body
    public JsonObject buildValorChest() {
        JsonObject jsonObject = new JsonObject();
        JsonArray listChest = new JsonArray();
        {
            JsonObject card = new JsonObject();
            int id = ERewardType.VALOR_CHEST.getValue();
            card.addProperty("id", id);
            card.addProperty("title", "Valor Chest");
            card.addProperty("rewardType", id);
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", 1);
            card.addProperty("rewardTag", "");
            listChest.add(card);
        }
        jsonObject.add("listChest", listChest);
        return jsonObject;
    }

    //for match result watch Ads
    public String claimValorChest(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int id = jsonObject.get("id").getAsInt();
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        long latestGameLogId = gameLogService.getLatestId(userID);
        long userLatestGameLog = MAP_CHECK_CLAIM_MATCH_RESULT_TIME.get(userID) == null ? -1 : MAP_CHECK_CLAIM_MATCH_RESULT_TIME.get(userID);
        if (latestGameLogId == -1) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "User does not match condition to claim reward!", NetWorkAPI.UNKNOWN);
        }
        if (latestGameLogId == userLatestGameLog) {
            return ResponseUtils.toResponseBody(HttpStatus.BAD_REQUEST.value(), "User already claim for this match!", NetWorkAPI.UNKNOWN);
        }
        List<HeroEnt> listHero = heroService.findAll();
        List<HeroEnt> listOwnedHero = inventoryService.getListOwnedHero(userID);
        for (HeroEnt heroEnt : listOwnedHero) {
            if (listHero.stream().anyMatch(e -> e.getId() == heroEnt.getId())) {
                heroEnt.setRarity(listHero.stream()
                        .filter(e -> e.getId() == heroEnt.getId())
                        .findFirst().get().getRarity());
            }
        }
        ChestConfig chestConfig = MAP_CHEST_CONFIG.get(id);
        List<RewardEnt> list = new ArrayList<>(randomReward(userID, chestConfig, listHero, listOwnedHero));
        MAP_CHECK_CLAIM_MATCH_RESULT_TIME.put(userID, latestGameLogId);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), buildValorChest(), JsonBuilder.buildListReward(list), id).toString();
    }
}
