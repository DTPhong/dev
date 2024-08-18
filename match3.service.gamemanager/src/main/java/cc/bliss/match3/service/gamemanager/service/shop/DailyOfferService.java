/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.shop;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.ShopCardDTO;
import cc.bliss.match3.service.gamemanager.ent.common.UpgradeConfigEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ShopTrackingCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RandomItem;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class DailyOfferService extends BaseService {

    protected static final String HASH_DAILY_OFFER = "h_shop_%s_%s";
    protected static final String FIELD_DAILY_OFFER = "list_daily_offer_card";
    protected static final String FIELD_NEXT_REFRESH_BTN = "next_refresh";
    protected static final String FIELD_FREE_GOLD = "free_gold";
    protected final int RANDOM_CARD_NUM = 5;
    protected final int PERCENT_LEGENDARY_CARD = 0;
    protected final int PERCENT_LEGENDARY_CARD_NOT_OWNED = 5;
    protected final int LEGENDARY_CARD_ORIGIN = 3;
    protected final int LEGENDARY_CARD_BOUND = 3;
    protected final int PERCENT_EPIC_CARD = 30;
    protected final int PERCENT_EPIC_CARD_NOT_OWNED = 30;
    protected final int EPIC_CARD_ORIGIN = 5;
    protected final int EPIC_CARD_BOUND = 7;
    protected final int PERCENT_RARE_CARD = 100 - PERCENT_LEGENDARY_CARD - PERCENT_EPIC_CARD;
    protected final int PERCENT_RARE_CARD_NOT_OWNED = 30;
    protected final int RARE_CARD_ORIGIN = 10;
    protected final int RARE_CARD_BOUND = 15;
    @Autowired
    protected InventoryService inventoryService;
    @Autowired
    protected HeroService heroService;
    @Autowired
    protected ProfileService profileService;
    @Autowired
    protected AdminService adminService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private DailyQuestService dailyQuestService;

    Type listType = new TypeToken<ArrayList<ShopCardDTO>>() {
    }.getType();

    protected String getRedisKey(long userID) {
        return String.format(HASH_DAILY_OFFER, userID, DateTimeUtils.getNow(DATE_FORMAT));
    }

    protected Map<String, String> getMapData(long userID) {
        return hashOperations.entries(getRedisKey(userID));
    }

    public String buyShop(int id, long userID) {
        List<RewardEnt> list = new ArrayList<>();
        List<RewardEnt> currency = new ArrayList<>();
        Map<String, String> mapData = getMapData(userID);
        Profile profile = profileService.getProfileByID(userID);
        long goldBeforeAction = profile.getMoney();
        long emeraldBeforeAction = profile.getEmerald();
        if (id == -1) {
            if (mapData.containsKey(FIELD_FREE_GOLD)) {
                return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
            }
            list.addAll(inventoryService.claimItem(userID, ERewardType.GOLD, 100, EUpdateMoneyType.DAIlY_OFFER));
            hashOperations.increment(getRedisKey(userID), FIELD_FREE_GOLD, 1);
            sendDailyOfferMessage(id, userID, profile, null, list, goldBeforeAction, emeraldBeforeAction);
        } else {
            List<ShopCardDTO> shopCardDTOs = getShopCard(userID, mapData);
            ShopCardDTO shopCardDTO = shopCardDTOs.get(id);

            long deltaMoney = getShopCardPrice(ERewardType.findByValue(shopCardDTO.getRewardType()), shopCardDTO.getRewardQuantity());
            // not enough money
            if (profile.getMoney() < deltaMoney) {
                return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
            }
            // already purchase
            if (shopCardDTO.getStatus() != EShopCardStatus.ACTIVE.ordinal()){
                return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
            }
            currency = inventoryService.updateMoney(userID, -deltaMoney, EUpdateMoneyType.DAIlY_OFFER);
            list.addAll(inventoryService.claimItem(userID,
                    ERewardType.findByValue(shopCardDTO.getRewardType()),
                    shopCardDTO.getRewardQuantity(),
                    shopCardDTO.getRewardRefID(), EUpdateMoneyType.DAIlY_OFFER));
            shopCardDTO.setStatus(EShopCardStatus.PURCHASED.ordinal());
            hashOperations.put(getRedisKey(userID), FIELD_DAILY_OFFER, JSONUtil.Serialize(shopCardDTOs));
            sendDailyOfferMessage(id, userID, profile, shopCardDTO, list, goldBeforeAction, emeraldBeforeAction);
        }
        JsonArray currencyArr = new JsonArray();
        if (currency.size() >= 1){
            currencyArr.add(JsonBuilder.buildSSEUpdateMoneyJson(currency.get(0)));
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(),
                getSectionInfo(userID),
                JsonBuilder.buildListReward(list),
                ERewardType.NONE.getValue(),
                currencyArr
        ).toString();
    }

    //only use to send message to rabbitmq, consider when reuse this function
    private void sendDailyOfferMessage(int itemId, long userID, Profile profile, ShopCardDTO shopCardDTO, List<RewardEnt> rewardEnts,
                                       long goldBeforeAction, long emeraldBeforeAction) {
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        long price = 0;
        String itemName = "";
        if (shopCardDTO != null) {
            price = getShopCardPrice(ERewardType.findByValue(shopCardDTO.getRewardType()), shopCardDTO.getRewardQuantity());
            ERewardType eRewardType = ERewardType.findByValue(shopCardDTO.getRewardType());
            if (ERewardType.RARE_CARD.equals(eRewardType) || ERewardType.EPIC_CARD.equals(eRewardType) || ERewardType.LEGENDARY_CARD.equals(eRewardType)) {
                String heroName = heroService.getHero(shopCardDTO.getRewardRefID()).getTitle();
                itemName = "Mảnh tướng " + heroName;
            }
        } else {
            itemName = rewardEnts.get(0).getTitle();
        }
        JsonArray jsonArray = new JsonArray();
        for (RewardEnt item : rewardEnts) {
            String title;
            if (item.getERewardType().equals(ERewardType.HERO_CARD)) {
                String heroName = heroService.getHero(item.getRef()).getTitle();
                title = item.getTitle() + " " + heroName;
            } else {
                title = item.getTitle();
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item_receive_id", item.getERewardType().getValue());
            jsonObject.addProperty("item_receive_amount", item.getDelta());
            jsonObject.addProperty("item_receive_name", title);
            jsonArray.add(jsonObject);
        }
        GMLocalQueue.addQueue(new ShopTrackingCmd(producer, profile, userTrophy, price, itemName,
                itemId, jsonArray, "BUY_ITEM", "DAILY_OFFER", itemId, "GOLD", goldBeforeAction, emeraldBeforeAction, redisTemplateString));
    }

    public JsonObject getSectionInfo(long userID) {
        Map<String, String> mapData = getMapData(userID);
        List<ShopCardDTO> shopCardDTOs = getShopCard(userID, mapData);
        long nextAdsBtn = ConvertUtils.toLong(mapData.get(FIELD_NEXT_REFRESH_BTN));

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", "DAILY OFFER");
        jsonObject.addProperty("nextRefresh", DateTimeUtils.getBeginDate(DateTimeUtils.addDays(1)).getTime());
        jsonObject.addProperty("adsButton", Boolean.TRUE);
        jsonObject.addProperty("adsNextRefresh", nextAdsBtn);
        jsonObject.addProperty("section", EShopSection.DAILY_OFFER.getValue());

        JsonArray listCard = new JsonArray();
        {
            JsonObject card = new JsonObject();
            card.addProperty("id", -1);
            card.addProperty("title", "Gift");
            card.addProperty("rewardType", ERewardType.PACK_GOLD_1.getValue());
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", 100);
            card.addProperty("rewardTag", "");

            card.addProperty("heroLevel", 0);
            card.addProperty("heroCardOwned", 0);
            card.addProperty("heroCardRequire", 0);
            card.addProperty("heroTrophy", 0);
            card.addProperty("heroIsOwned", false);

            card.addProperty("moneyType", ERewardType.NONE.getValue());
            card.addProperty("moneyRequire", 0);
            card.addProperty("cardStatus", mapData.containsKey(FIELD_FREE_GOLD)
                    ? EShopCardStatus.PURCHASED.ordinal()
                    : EShopCardStatus.ACTIVE.ordinal());
            listCard.add(card);
        }
        for (int i = 0; i < shopCardDTOs.size(); i++) {
            ShopCardDTO shopCardDTO = shopCardDTOs.get(i);
            JsonObject card = new JsonObject();
            card.addProperty("id", i);
            card.addProperty("rewardType", ERewardType.HERO_CARD.getValue());
            card.addProperty("rewardRefId", shopCardDTO.getRewardRefID());
            card.addProperty("rewardQuantity", shopCardDTO.getRewardQuantity());
            card.addProperty("rewardTag", "");

            card.addProperty("title", "");
            card.addProperty("heroLevel", 0);
            card.addProperty("heroCardOwned", 0);
            card.addProperty("heroCardRequire", 0);
            card.addProperty("heroTrophy", 0);
            card.addProperty("heroIsOwned", false);

            card.addProperty("moneyType", ERewardType.GOLD.getValue());
            card.addProperty("moneyRequire", getShopCardPrice(ERewardType.findByValue(shopCardDTO.getRewardType()), shopCardDTO.getRewardQuantity()));
            card.addProperty("cardStatus", shopCardDTO.getStatus() == EShopCardStatus.PURCHASED.ordinal()
                    ? EShopCardStatus.PURCHASED.ordinal()
                    : EShopCardStatus.ACTIVE.ordinal());
            listCard.add(card);
        }
        jsonObject.add("listCard", listCard);
        jsonObject.add("listBigCard", new JsonArray());
        return jsonObject;
    }

    protected long getShopCardPrice(ERewardType eRewardType, long quantity) {
        switch (eRewardType) {
            case LEGENDARY_CARD:
                return 24990 * quantity;
            case EPIC_CARD:
                return 3750 * quantity;
            case RARE_CARD:
                return 150 * quantity;
            default:
                return 150 * quantity;
        }
    }

    protected List<ShopCardDTO> getShopCard(long userID, Map<String, String> mapData) {
        List<ShopCardDTO> shopCardDTOs = Collections.EMPTY_LIST;
        String value = mapData.get(FIELD_DAILY_OFFER);
        if (value != null && !value.isEmpty()) {
            shopCardDTOs = JSONUtil.DeSerialize(value, listType);
        }
        if (shopCardDTOs.isEmpty()) {
            shopCardDTOs = initShopCard(userID);
        }
        return shopCardDTOs;
    }

    protected List<ShopCardDTO> initShopCard(long userID) {
        List<HeroEnt> listHero = heroService.findAll();
        List<HeroEnt> listOwnedHero = inventoryService.getListOwnedHero(userID);
        List<HeroEnt> listOwnedHeroMatchCondition = listOwnedHeroMatchCondition(userID, listOwnedHero, listHero);
        List<Integer> listHeroEnoughShardUpMaxLevel = listHeroEnoughShardUpMaxLevel(userID, listOwnedHero, listHero);
        List<HeroEnt> heroesMatchCondition = listHeroMatchCondition(listOwnedHero, listHero, listHeroEnoughShardUpMaxLevel);
        List<HeroEnt> listOwnedHeroMaxLevel = listOwnedHeroMaxLevel(listOwnedHero);
        for (HeroEnt heroEnt : listOwnedHeroMatchCondition) {
            heroEnt.setRarity(listHero.stream()
                    .filter(e -> e.getId() == heroEnt.getId())
                    .findFirst().get().getRarity());
        }
        List<ShopCardDTO> shopCardDTOs = new ArrayList<>();
        for (int i = 0; i < RANDOM_CARD_NUM; i++) {
            ShopCardDTO shopCardDTO = randomCardV2(heroesMatchCondition, listOwnedHeroMatchCondition, userID, listOwnedHeroMaxLevel, listOwnedHero, listHero.size());
            shopCardDTOs.add(shopCardDTO);
        }
        hashOperations.put(getRedisKey(userID), FIELD_DAILY_OFFER, JSONUtil.Serialize(shopCardDTOs));
        redisTemplateString.expire(getRedisKey(userID), Duration.ofDays(3));
        return shopCardDTOs;
    }

    private List<HeroEnt> listOwnedHeroLessThan10(List<HeroEnt> listOwnedHero) {
        return listOwnedHero.stream()
                .filter(hero -> hero.getLevel() < 10).collect(Collectors.toList());
    }

    private List<HeroEnt> listOwnedHeroMaxLevel(List<HeroEnt> listOwnedHero) {
        return listOwnedHero.stream()
                .filter(hero -> hero.getLevel() == 10).collect(Collectors.toList());
    }

    private boolean isShardEnoughToUpMaxLevel(HeroEnt heroEnt, List<HeroEnt> listHeroes, long userId) {
        int heroId = heroEnt.getId();
        int currentShard = (int) inventoryService.getHeroShard(userId, heroId);
        heroEnt.setUpgradeConfig(listHeroes.stream()
                .filter(e -> e.getId() == heroEnt.getId())
                .findFirst().get().getUpgradeConfig());
        return heroService.isShardEnoughToUpMaxLevel(heroEnt, currentShard);
    }

    private List<HeroEnt> listOwnedHeroMatchCondition(long userId, List<HeroEnt> listOwnedHero, List<HeroEnt> listHeroes) {
        List<HeroEnt> listOwnedHeroLessThan10 = listOwnedHeroLessThan10(listOwnedHero);
        List<HeroEnt> listHeroMatchCondition = new ArrayList<>();
        for (HeroEnt heroEnt : listOwnedHeroLessThan10) {
            boolean isShardEnoughToUpMaxLevel = isShardEnoughToUpMaxLevel(heroEnt, listHeroes, userId);
            if (!isShardEnoughToUpMaxLevel) {
                listHeroMatchCondition.add(heroEnt);
            }
        }
        return listHeroMatchCondition;
    }

    private List<Integer> listHeroEnoughShardUpMaxLevel(long userId, List<HeroEnt> listOwnedHero, List<HeroEnt> listHeroes) {
        List<HeroEnt> listOwnedHeroLessThan10  = listOwnedHeroLessThan10(listOwnedHero);
        List<HeroEnt> listHeroEnoughShardUpMaxLevel = new ArrayList<>();
        for (HeroEnt heroEnt : listOwnedHeroLessThan10) {
            boolean isShardEnoughToUpMaxLevel = isShardEnoughToUpMaxLevel(heroEnt, listHeroes, userId);
            if (isShardEnoughToUpMaxLevel) {
                listHeroEnoughShardUpMaxLevel.add(heroEnt);
            }
        }
        return listHeroEnoughShardUpMaxLevel.stream().map(HeroEnt::getId).collect(Collectors.toList());
    }

    private List<HeroEnt> listHeroMatchCondition(List<HeroEnt> listOwnedHero, List<HeroEnt> heroes, List<Integer> listHeroEnoughShardUpMaxLevelId) {
        Set<Integer> heroesMaxLevel = listOwnedHero.stream()
                .filter(hero -> hero.getLevel() == 10).map(HeroEnt::getId).collect(Collectors.toSet());
        return heroes.stream()
                .filter(hero -> !listHeroEnoughShardUpMaxLevelId.contains(hero.getId()))
                .filter(hero -> !heroesMaxLevel.contains(hero.getId()))
                .collect(Collectors.toList());
    }
    private int getRandType(int percentNotOwned){
        List<RandomItem> listStatusCardRan = new ArrayList<>();
        listStatusCardRan.add(RandomItem.builder().data(EOwnedStatus.NOT_OWNED.ordinal()).percent(percentNotOwned).build());
        listStatusCardRan.add(RandomItem.builder().data(EOwnedStatus.OWNED.ordinal()).percent(100 - percentNotOwned).build());
        return RandomUtils.random(listStatusCardRan);
    }

    private int getRandRarity(){
        List<RandomItem> listTypeCardRan = new ArrayList<>();
        listTypeCardRan.add(RandomItem.builder().data(ERewardType.LEGENDARY_CARD.getValue()).percent(PERCENT_LEGENDARY_CARD).build());
        listTypeCardRan.add(RandomItem.builder().data(ERewardType.EPIC_CARD.getValue()).percent(PERCENT_EPIC_CARD).build());
        listTypeCardRan.add(RandomItem.builder().data(ERewardType.RARE_CARD.getValue()).percent(PERCENT_RARE_CARD).build());
        return RandomUtils.random(listTypeCardRan);
    }
    protected ShopCardDTO randomCard(List<HeroEnt> listHero, List<HeroEnt> listOwnedHero) {
        int cardTypeInt = getRandRarity();
        ERewardType cardType = ERewardType.findByValue(cardTypeInt);
        int heroID = 1;
        int quantity = 10;
        int cardOrigin = 0;
        int cardBound = 0;
        int ownedStatus = 0;
        List<Integer> listOwnedHeroID = Collections.EMPTY_LIST;
        List<Integer> notOwnedHero = Collections.EMPTY_LIST;
        switch (cardType) {
            case LEGENDARY_CARD: {
                ownedStatus = getRandType(PERCENT_LEGENDARY_CARD_NOT_OWNED);
                listHero = listHero.stream().filter(e -> e.getRarity().equals(EHeroRarity.LEGENDARY)).collect(Collectors.toList());
                listOwnedHeroID = listOwnedHero.stream()
                        .filter(e -> e.getRarity().equals(EHeroRarity.LEGENDARY))
                        .map(e -> e.getId())
                        .collect(Collectors.toList());
                cardOrigin = LEGENDARY_CARD_ORIGIN;
                cardBound = LEGENDARY_CARD_BOUND;
            }
            break;
            case EPIC_CARD: {
                ownedStatus = getRandType(PERCENT_EPIC_CARD_NOT_OWNED);
                listHero = listHero.stream().filter(e -> e.getRarity().equals(EHeroRarity.EPIC)).collect(Collectors.toList());
                listOwnedHeroID = listOwnedHero.stream()
                        .filter(e -> e.getRarity().equals(EHeroRarity.EPIC))
                        .map(e -> e.getId())
                        .collect(Collectors.toList());
                cardOrigin = EPIC_CARD_ORIGIN;
                cardBound = EPIC_CARD_BOUND;
            }
            break;
            case RARE_CARD: {
                ownedStatus = getRandType(PERCENT_RARE_CARD_NOT_OWNED);
                listHero = listHero.stream().filter(e -> e.getRarity().equals(EHeroRarity.MYTHIC)).collect(Collectors.toList());
                listOwnedHeroID = listOwnedHero.stream()
                        .filter(e -> e.getRarity().equals(EHeroRarity.MYTHIC))
                        .map(e -> e.getId())
                        .collect(Collectors.toList());
                cardOrigin = RARE_CARD_ORIGIN;
                cardBound = RARE_CARD_BOUND;
            }
            break;
        }


        List<Integer> finalListOwnedHeroID = listOwnedHeroID;
        if ((ownedStatus == EOwnedStatus.NOT_OWNED.ordinal() && listHero.size() > listOwnedHeroID.size()) || listOwnedHeroID.size() == 0) {
            notOwnedHero = listHero.stream().filter(e -> !finalListOwnedHeroID.contains(e.getId())).map(e -> e.getId()).collect(Collectors.toList());
            heroID = notOwnedHero.get(RandomUtils.RAND.nextInt(notOwnedHero.size()));
            quantity = cardOrigin == cardBound ? cardOrigin : RandomUtils.RAND.nextInt(cardOrigin, cardBound + 1);
        } else {
            heroID = listOwnedHeroID.get(RandomUtils.RAND.nextInt(listOwnedHeroID.size()));
            quantity = cardOrigin == cardBound ? cardOrigin : RandomUtils.RAND.nextInt(cardOrigin, cardBound + 1);
        }
        return ShopCardDTO.builder().rewardType(cardType.getValue()).rewardRefID(heroID).rewardQuantity(quantity).build();
    }

    private int shardWillSell(float shardWillSell) {
        if (shardWillSell < 2) {
            return 1;
        }
        else if (shardWillSell < 5) {
            return 2;
        } else {
            return 5 * (Math.round(shardWillSell/5));
        }
    }

    private int calculateCardQuantity(int heroId, long userId, List<HeroEnt> listOwnedHero) {
        HeroEnt heroEnt = heroService.getHeroById(heroId);
        HeroEnt heroOwned = listOwnedHero.stream().filter(hero -> hero.getId() == heroEnt.getId()).findFirst().get();
        int shardWillSell;
        int currentShard = (int) inventoryService.getHeroShard(userId, heroId);
        int remainShard = heroService.getRemainShard(heroOwned, currentShard);
        int currentLevel = heroService.getMaxLevel(heroOwned, currentShard);
        UpgradeConfigEnt upgradeCurrentLevel = heroEnt.getLevelUpgradeConfig(currentLevel + 1);
        UpgradeConfigEnt upgradeNextLevel = heroEnt.getLevelUpgradeConfig(currentLevel + 2);
        int shardRequireCurrentLevel = upgradeCurrentLevel.getShard();
        int shardRequireNextLevel = upgradeNextLevel.getShard();

        if (remainShard < shardRequireCurrentLevel) {
            double shardRequire60Percent = shardRequireCurrentLevel * 0.6;
            int yShard;
            float xShard;
            if (remainShard >= shardRequire60Percent) {
                yShard = shardRequireCurrentLevel - remainShard + shardRequireNextLevel;
            } else {
                yShard = shardRequireCurrentLevel - remainShard;
            }
            xShard = (float) yShard / 5;
            shardWillSell = shardWillSell(xShard);
        } else {
            shardWillSell = 10;
        }
        return shardWillSell;
    }

    protected ShopCardDTO randomCardV2(List<HeroEnt> listHero, List<HeroEnt> listOwnedHeroMatchCondition, long userId, List<HeroEnt> listOwnedHeroMaxLevel,
                                       List<HeroEnt> listOwnedHero, int heroesSize) {
        int cardTypeInt = getRandRarity();
        ERewardType cardType = ERewardType.findByValue(cardTypeInt);
        int heroID;
        int quantity;
        int ownedStatus = 0;
        int cardOrigin = 0;
        int cardBound = 0;
        List<Integer> listOwnedHeroID = Collections.emptyList();
        List<Integer> notOwnedHeroID;
        switch (cardType) {
            case LEGENDARY_CARD: {
                ownedStatus = getRandType(PERCENT_LEGENDARY_CARD_NOT_OWNED);
                listHero = listHero.stream().filter(e -> e.getRarity().equals(EHeroRarity.LEGENDARY)).collect(Collectors.toList());
                listOwnedHeroID = listOwnedHeroMatchCondition.stream()
                        .filter(e -> e.getRarity().equals(EHeroRarity.LEGENDARY))
                        .map(HeroEnt::getId)
                        .collect(Collectors.toList());
                cardOrigin = LEGENDARY_CARD_ORIGIN;
                cardBound = LEGENDARY_CARD_BOUND;
            }
            break;
            case EPIC_CARD: {
                ownedStatus = getRandType(PERCENT_EPIC_CARD_NOT_OWNED);
                listHero = listHero.stream().filter(e -> e.getRarity().equals(EHeroRarity.EPIC)).collect(Collectors.toList());
                listOwnedHeroID = listOwnedHeroMatchCondition.stream()
                        .filter(e -> e.getRarity().equals(EHeroRarity.EPIC))
                        .map(HeroEnt::getId)
                        .collect(Collectors.toList());
                cardOrigin = EPIC_CARD_ORIGIN;
                cardBound = EPIC_CARD_BOUND;
            }
            break;
            case RARE_CARD: {
                ownedStatus = getRandType(PERCENT_RARE_CARD_NOT_OWNED);
                listHero = listHero.stream().filter(e -> e.getRarity().equals(EHeroRarity.MYTHIC)).collect(Collectors.toList());
                listOwnedHeroID = listOwnedHeroMatchCondition.stream()
                        .filter(e -> e.getRarity().equals(EHeroRarity.MYTHIC))
                        .map(HeroEnt::getId)
                        .collect(Collectors.toList());
                cardOrigin = RARE_CARD_ORIGIN;
                cardBound = RARE_CARD_BOUND;
            }
            break;
        }
        List<Integer> finalListOwnedHeroID = listOwnedHeroID;
        List<Integer> heroesLevel10 = listOwnedHeroMaxLevel.stream().map(HeroEnt::getId).collect(Collectors.toList());
        List<Integer> heroesOwnedId = listOwnedHero.stream().map(HeroEnt::getId).collect(Collectors.toList());
        if ((ownedStatus == EOwnedStatus.NOT_OWNED.ordinal() && listOwnedHero.size() < heroesSize) || listOwnedHeroID.isEmpty()) {
            notOwnedHeroID = listHero.stream().map(HeroEnt::getId).filter(id -> !finalListOwnedHeroID.contains(id)).collect(Collectors.toList());
            if (notOwnedHeroID.isEmpty()){
                if (heroesLevel10.isEmpty()) {
                    heroID = heroesOwnedId.get(RandomUtils.RAND.nextInt(heroesOwnedId.size()));
                    quantity = calculateCardQuantity(heroID, userId, listOwnedHero);
                } else {
                    heroID = heroesLevel10.get(RandomUtils.RAND.nextInt(heroesLevel10.size()));
                    quantity = 10;
                }
            } else {
                heroID = notOwnedHeroID.get(RandomUtils.RAND.nextInt(notOwnedHeroID.size()));
                quantity = 1;
            }

        } else {
            heroID = listOwnedHeroID.get(RandomUtils.RAND.nextInt(listOwnedHeroID.size()));
            quantity = calculateCardQuantity(heroID, userId, listOwnedHero);
        }
        return ShopCardDTO.builder().rewardType(cardType.getValue()).rewardRefID(heroID).rewardQuantity(quantity).build();
    }

    public void refreshShop(long userID) {
        String redisKey = getRedisKey(userID);
        long nextRefresh = ConvertUtils.toLong(hashOperations.get(redisKey, FIELD_NEXT_REFRESH_BTN));
        if (System.currentTimeMillis() < nextRefresh) {
            return;
        }
        hashOperations.delete(redisKey, FIELD_DAILY_OFFER);
        hashOperations.delete(redisKey, FIELD_FREE_GOLD);
        hashOperations.put(redisKey, FIELD_NEXT_REFRESH_BTN, String.valueOf(System.currentTimeMillis() + Duration.ofHours(8).toMillis()));
        GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, userID, EQuestType.WATCH_ADS_QUEST, 1));
    }
}
