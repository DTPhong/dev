/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.shop;

import cc.bliss.match3.service.gamemanager.db.specification.InvoiceSpecification;
import cc.bliss.match3.service.gamemanager.db.specification.ProductSpecification;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.VerifyResult;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.InvoiceEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ProductEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ShopTrackingCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.common.LeaderboardService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Phong
 */
@Service
public class EmeraldPackService extends BaseService {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private GoogleProcessor googleProcessor;

    public JsonObject getSectionInfo(long userID) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", EShopSection.EMERALD_PACK.name());
        jsonObject.addProperty("remainTime", 0);
        jsonObject.addProperty("adsButton", Boolean.FALSE);
        jsonObject.addProperty("adsCountDown", 0);
        jsonObject.addProperty("section", EShopSection.EMERALD_PACK.getValue());

        Specification<ProductEnt> specification = Specification.where(ProductSpecification.withProductType(EProductType.EMERALD_PACK))
                .and(ProductSpecification.withStatus(EProductStatus.ACTIVE));
        List<ProductEnt> productEnts = productRepository.read().findAll(specification);

        Specification<InvoiceEnt> specificationInvoice = Specification.where(InvoiceSpecification.withProductType(EProductType.EMERALD_PACK.ordinal()))
                .and(InvoiceSpecification.withStatus(1));
        List<InvoiceEnt> invoiceEnts = invoiceRepository.read().findAll(specificationInvoice);
        JsonArray listCard = new JsonArray();
        for (ProductEnt productEnt : productEnts) {
            {
                JsonObject card = new JsonObject();
                card.addProperty("id", productEnt.getId());
                card.addProperty("title", productEnt.getTitle());
                card.addProperty("rewardType", productEnt.getIconID());
                card.addProperty("rewardRefId", 0);
                card.addProperty("rewardQuantity", productEnt.getDiamond());
                card.addProperty("rewardTag", productEnt.getTag());
                boolean isBuy = invoiceEnts.stream()
                        .anyMatch(e -> e.getProductId() == productEnt.getId());
                card.addProperty("specialDeal", !isBuy ? "1st Purchase Only" : "");
                card.addProperty("bonus", !isBuy ? productEnt.getDiamond() : 0);

                card.addProperty("heroLevel", 0);
                card.addProperty("heroCardOwned", 0);
                card.addProperty("heroCardRequire", 0);
                card.addProperty("heroTrophy", 0);

                card.addProperty("moneyType", ERewardType.EMERALD.getValue());
                card.addProperty("moneyRequire", productEnt.getAmount());
                card.addProperty("cardStatus", EShopCardStatus.ACTIVE.ordinal());
                listCard.add(card);
            }
        }
        jsonObject.add("listCard", listCard);
        jsonObject.add("listBigCard", new JsonArray());
        return jsonObject;
    }

    public List<RewardEnt> buyShop(int id, long userID, String receipt) {
        List<RewardEnt> list = new ArrayList<>();
        Specification<ProductEnt> specification = Specification
                .where(ProductSpecification.withProductType(EProductType.EMERALD_PACK))
                .and(ProductSpecification.withStatus(EProductStatus.ACTIVE))
                .and(ProductSpecification.withID(id));
        Optional<ProductEnt> optional = productRepository.read().findOne(specification);
        Profile profile = profileService.getMinProfileByID(userID);
        long goldBeforeAction = profile.getMoney();
        long emeraldBeforeAction = profile.getEmerald();
        InvoiceEnt.InvoiceEntBuilder invoiceEntBuilder = InvoiceEnt.builder();
        if (optional.isPresent()) {
            ProductEnt productEnt = optional.get();
            VerifyResult verifyResult = googleProcessor.processPaymentV3(receipt);
            if (verifyResult.getStatusCode().equals(StatusCode.success)){
                // verify success
                Specification<InvoiceEnt> specificationInvoice = Specification.where(InvoiceSpecification.withProductType(EProductType.EMERALD_PACK.ordinal()))
                        .and(InvoiceSpecification.withStatus(1));
                List<InvoiceEnt> invoiceEnts = invoiceRepository.read().findAll(specificationInvoice);

                boolean isProcessedReceipt = invoiceEnts.stream()
                        .anyMatch(e -> e.getPartnerId() != null && e.getPartnerId().contentEquals(verifyResult.getPartnerId()));
                if (!isProcessedReceipt){
                    boolean isFirstTimeBuy = invoiceEnts.stream()
                            .anyMatch(e -> e.getProductId() == productEnt.getId());
                    // cộng tiền
                    int deltaDiamond = isFirstTimeBuy ? productEnt.getDiamond() : productEnt.getDiamond() * 2;
                    list.addAll(inventoryService.claimItem(userID, ERewardType.findByValue(productEnt.getIconID()), deltaDiamond, EUpdateMoneyType.EMERALD_PACK));
                    // set field invoice
                    invoiceEntBuilder.status(1).partnerId(verifyResult.getPartnerId());
                    // log data superset
                    sendDiamondPackMessage(id, userID, profile, "EMERALD", productEnt, goldBeforeAction, emeraldBeforeAction);
                }
            }
        }
        invoiceEntBuilder
                .userId(userID)
                .productId(id)
                .productType(EProductType.EMERALD_PACK.ordinal())
                .data(receipt)
                .build();
        insertMatch3SchemaData(invoiceEntBuilder.build());
        return list;
    }

    //only use to send message to rabbitmq, consider when reuse this function
    private void sendDiamondPackMessage(int itemId, long userID, Profile profile, String currency, ProductEnt productEnt, long goldBeforeAction, long emeraldBeforeAction) {
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("item_receive_id", productEnt.getId());
        jsonObject.addProperty("item_receive_amount", productEnt.getDiamond());
        jsonObject.addProperty("item_receive_name", productEnt.getTitle());
        jsonArray.add(jsonObject);
        GMLocalQueue.addQueue(new ShopTrackingCmd(producer, profile, userTrophy, productEnt.getAmount(), productEnt.getTitle(),
                itemId, jsonArray, "EMERALD2DIAMOND_EXCHANGE", "EMERALD_PACK", itemId, currency, goldBeforeAction, emeraldBeforeAction, redisTemplateString));
    }

}
