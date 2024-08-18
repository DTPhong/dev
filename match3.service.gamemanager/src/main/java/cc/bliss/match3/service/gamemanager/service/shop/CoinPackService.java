/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.shop;

import cc.bliss.match3.service.gamemanager.db.match3.ProductWriteRepository;
import cc.bliss.match3.service.gamemanager.db.specification.ProductSpecification;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ProductEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ShopTrackingCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Phong
 */
@Service
public class CoinPackService extends BaseService {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ProfileService  profileService;
    @Autowired
    private LeaderboardService leaderboardService;

    public JsonObject getSectionInfo(long userID) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", EShopSection.COIN_PACK.name());
        jsonObject.addProperty("remainTime", 0);
        jsonObject.addProperty("adsButton", Boolean.FALSE);
        jsonObject.addProperty("adsCountDown", 0);
        jsonObject.addProperty("section", EShopSection.COIN_PACK.getValue());

        Specification<ProductEnt> specification = Specification.where(ProductSpecification.withProductType(EProductType.EMERALD_TO_GOLD))
                .and(ProductSpecification.withStatus(EProductStatus.ACTIVE));
        List<ProductEnt> productEnts = productRepository.read().findAll(specification);
        JsonArray listCard = new JsonArray();
        for (ProductEnt productEnt : productEnts) {
            {
                JsonObject card = new JsonObject();
                card.addProperty("id", productEnt.getId());
                card.addProperty("title", productEnt.getTitle());
                card.addProperty("rewardType", productEnt.getIconID());
                card.addProperty("rewardRefId", 0);
                card.addProperty("rewardQuantity", productEnt.getGold());
                card.addProperty("rewardTag", productEnt.getTag());
                card.addProperty("specialDeal", "");

                card.addProperty("heroLevel", 0);
                card.addProperty("heroCardOwned", 0);
                card.addProperty("heroCardRequire", 0);
                card.addProperty("heroTrophy", 0);

                card.addProperty("moneyType", ERewardType.EMERALD.getValue());
                card.addProperty("moneyRequire", productEnt.getAmountEmerald());
                card.addProperty("cardStatus", EShopCardStatus.ACTIVE.ordinal());
                listCard.add(card);
            }
        }
        jsonObject.add("listCard", listCard);
        jsonObject.add("listBigCard", new JsonArray());
        return jsonObject;
    }

    public List<RewardEnt> buyShop(int id, long userID) {
        List<RewardEnt> list = new ArrayList<>();
        Specification<ProductEnt> specification = Specification
                .where(ProductSpecification.withProductType(EProductType.EMERALD_TO_GOLD))
                .and(ProductSpecification.withStatus(EProductStatus.ACTIVE))
                .and(ProductSpecification.withID(id));
        Optional<ProductEnt> optional = productRepository.read().findOne(specification);
        Profile profile = profileService.getMinProfileByID(userID);
        long goldBeforeAction = profile.getMoney();
        long emeraldBeforeAction = profile.getEmerald();
        if (optional.isPresent()) {
            ProductEnt productEnt = optional.get();

            if (profile.getEmerald() < productEnt.getAmountEmerald()) {
                return list;
            }
            list.addAll(inventoryService.claimItem(userID, ERewardType.EMERALD, -productEnt.getAmountEmerald(), EUpdateMoneyType.COIN_PACK));
            list.addAll(inventoryService.claimItem(userID, ERewardType.GOLD, productEnt.getGold(), EUpdateMoneyType.COIN_PACK));
            sendCoinPackMessage(id, userID, profile, "EMERALD", productEnt, goldBeforeAction, emeraldBeforeAction);
        }
        return list;
    }

    //only use to send message to rabbitmq, consider when reuse this function
    private void sendCoinPackMessage(int itemId, long userID, Profile profile, String currency, ProductEnt productEnt, long goldBeforeAction, long emeraldBeforeAction) {
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("item_receive_id", productEnt.getId());
        jsonObject.addProperty("item_receive_amount", productEnt.getGold());
        jsonObject.addProperty("item_receive_name", productEnt.getTitle());
        jsonArray.add(jsonObject);
        GMLocalQueue.addQueue(new ShopTrackingCmd(producer, profile, userTrophy, productEnt.getAmountEmerald(), productEnt.getTitle(),
                itemId, jsonArray, "EMERALD2GOLD_EXCHANGE", "COIN_PACK", itemId, currency, goldBeforeAction, emeraldBeforeAction, redisTemplateString));
    }

}
