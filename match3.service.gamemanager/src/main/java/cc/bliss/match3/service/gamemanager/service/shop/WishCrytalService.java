/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.shop;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.ShopCardDTO;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ShopTrackingCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Phong
 */
@Service
public class WishCrytalService extends DailyOfferService {

    protected static final String FIELD_FREE_EMERALD = "free_emerald";
    private static final String FIELD_BUY_AMETHYST = "buy_amethyst_%s";

    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private DailyQuestService dailyQuestService;

    @Override
    protected List<ShopCardDTO> getShopCard(long userID, Map<String, String> mapData) {
        List<ShopCardDTO> cardDTOs = new ArrayList<>();
        {
            int status = mapData.containsKey(String.format(FIELD_BUY_AMETHYST, 0))
                    ? EShopCardStatus.PURCHASED.ordinal()
                    : EShopCardStatus.ACTIVE.ordinal();
            ShopCardDTO cardDTO = ShopCardDTO.builder()
                    .id(0)
                    .rewardQuantity(50)
                    .rewardType(ERewardType.CASKET_OF_AMETHYST.getValue())
                    .status(status)
                    .moneyType(ERewardType.EMERALD.getValue())
                    .moneyRequire(8000)
                    .title("A Casket of Amethyst")
                    .build();
            cardDTOs.add(cardDTO);
        }
        {
            int status = mapData.containsKey(String.format(FIELD_BUY_AMETHYST, 1))
                    ? EShopCardStatus.PURCHASED.ordinal()
                    : EShopCardStatus.ACTIVE.ordinal();
            ShopCardDTO cardDTO = ShopCardDTO.builder()
                    .id(1)
                    .rewardQuantity(150)
                    .rewardType(ERewardType.TUB_OF_AMETHYST.getValue())
                    .status(status)
                    .moneyType(ERewardType.EMERALD.getValue())
                    .moneyRequire(24000)
                    .title("A Tub of Amethyst")
                    .build();
            cardDTOs.add(cardDTO);
        }
        return cardDTOs;
    }

    @Override
    public String buyShop(int id, long userID) {
        List<RewardEnt> list = new ArrayList<>();
        List<RewardEnt> currency = new ArrayList<>();
        Map<String, String> mapData = getMapData(userID);
        Profile profile = profileService.getProfileByID(userID);
        long goldBeforeAction = profile.getMoney();
        long emeraldBeforeAction = profile.getEmerald();
        if (id == -1) {
            int countClaim = ConvertUtils.toInt(mapData.get(FIELD_FREE_EMERALD), -1);
            if (countClaim >= 2) {
                return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
            }
            if (countClaim == 1){
                GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, userID, EQuestType.WATCH_ADS_QUEST, 1));
            }
            list.addAll(inventoryService.claimItem(userID, ERewardType.COMMON_EMERALD, 10, EUpdateMoneyType.WISH_CRYTAL));
            hashOperations.increment(getRedisKey(userID), FIELD_FREE_EMERALD, 1);
            sendWishCrystalMessage(id, userID, profile, null, list, "FREE", goldBeforeAction, emeraldBeforeAction);
        } else {
            List<ShopCardDTO> shopCardDTOs = getShopCard(userID, mapData);
            ShopCardDTO shopCardDTO = shopCardDTOs.get(id);

            long deltaMoney = shopCardDTO.getMoneyRequire();
            if (profile.getEmerald() < deltaMoney) {
                return ResponseUtils.toErrorBody(HttpStatus.BAD_REQUEST.name(), NetWorkAPI.UNKNOWN);
            }
            currency = inventoryService.claimItem(userID, ERewardType.EMERALD, -deltaMoney, EUpdateMoneyType.WISH_CRYTAL);
            list.addAll(inventoryService.claimItem(userID,
                    ERewardType.findByValue(shopCardDTO.getRewardType()),
                    shopCardDTO.getRewardQuantity(),
                    shopCardDTO.getRewardRefID(), EUpdateMoneyType.WISH_CRYTAL));
            hashOperations.increment(getRedisKey(userID), String.format(FIELD_BUY_AMETHYST, shopCardDTO.getId()), 1);
            sendWishCrystalMessage(id, userID, profile, shopCardDTO, list, "EMERALD", goldBeforeAction, emeraldBeforeAction);
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
    private void sendWishCrystalMessage(int itemId, long userID, Profile profile, ShopCardDTO shopCardDTO, List<RewardEnt> rewardEnts, String currency, long goldBeforeAction, long emeraldBeforeAction) {
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        long price = 0;
        if (shopCardDTO != null) {
            price = shopCardDTO.getMoneyRequire();
        }
        String itemName;
        if (itemId == -1) {
            itemName = "Gift";
        } else {
            itemName = shopCardDTO.getTitle();
        }
        JsonArray jsonArray = new JsonArray();
        for (RewardEnt item : rewardEnts) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item_receive_id", item.getERewardType().getValue());
            jsonObject.addProperty("item_receive_amount", item.getDelta());
            jsonObject.addProperty("item_receive_name", item.getTitle());
            jsonArray.add(jsonObject);
        }
        GMLocalQueue.addQueue(new ShopTrackingCmd(producer, profile, userTrophy, price, itemName,
                itemId, jsonArray, "EMERALD2AMETHYST_EXCHANGE", "WISH_CRYSTAL", itemId, currency, goldBeforeAction, emeraldBeforeAction, redisTemplateString));
    }

    @Override
    public JsonObject getSectionInfo(long userID) {
        Map<String, String> mapData = getMapData(userID);
        List<ShopCardDTO> shopCardDTOs = getShopCard(userID, mapData);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", "WISH CRYTAL");
        jsonObject.addProperty("nextRefresh", DateTimeUtils.getBeginDate(DateTimeUtils.addDays(1)).getTime());
        jsonObject.addProperty("adsButton", Boolean.FALSE);
        jsonObject.addProperty("adsNextRefresh", DateTimeUtils.getBeginDate(DateTimeUtils.addDays(1)).getTime());
        jsonObject.addProperty("section", EShopSection.WISH_CRYSTAL.getValue());

        JsonArray listCard = new JsonArray();
        {
            JsonObject card = new JsonObject();
            card.addProperty("id", -1);
            card.addProperty("title", "Gift");
            card.addProperty("rewardType", ERewardType.COMMON_EMERALD.getValue());
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", 10);
            card.addProperty("rewardTag", "");

            card.addProperty("heroLevel", 0);
            card.addProperty("heroCardOwned", 0);
            card.addProperty("heroCardRequire", 0);
            card.addProperty("heroTrophy", 0);

            card.addProperty("moneyType", ERewardType.NONE.getValue());
            card.addProperty("moneyRequire", 0);
            int countClaim = ConvertUtils.toInt(mapData.get(FIELD_FREE_EMERALD), -1);
            if (countClaim == 2){
                // countdown state
                card.addProperty("cardStatus", EShopCardStatus.PURCHASED.ordinal());
                card.addProperty("cardNextClaim", DateTimeUtils.getBeginDate(DateTimeUtils.addDays(1)).getTime());
            } else if (countClaim == 1) {
                // ads state
                card.addProperty("cardStatus", EShopCardStatus.ACTIVE.ordinal());
                card.addProperty("cardNextClaim", 0);
            } else {
                // free state
                card.addProperty("cardStatus", EShopCardStatus.ACTIVE.ordinal());
                card.addProperty("cardNextClaim", -1);
            }
            listCard.add(card);
        }
        for (ShopCardDTO shopCardDTO : shopCardDTOs) {
            JsonObject card = new JsonObject();
            card.addProperty("id", shopCardDTO.getId());
            card.addProperty("title", shopCardDTO.getTitle());
            card.addProperty("rewardType", shopCardDTO.getRewardType());
            card.addProperty("rewardRefId", 0);
            card.addProperty("rewardQuantity", shopCardDTO.getRewardQuantity());
            card.addProperty("rewardTag", "");

            card.addProperty("heroLevel", 0);
            card.addProperty("heroCardOwned", 0);
            card.addProperty("heroCardRequire", 0);
            card.addProperty("heroTrophy", 0);

            card.addProperty("moneyType", shopCardDTO.getMoneyType());
            card.addProperty("moneyRequire", shopCardDTO.getMoneyRequire());
            card.addProperty("cardStatus", shopCardDTO.getStatus());
            listCard.add(card);
        }
        jsonObject.add("listCard", listCard);
        jsonObject.add("listBigCard", new JsonArray());
        return jsonObject;
    }

}
