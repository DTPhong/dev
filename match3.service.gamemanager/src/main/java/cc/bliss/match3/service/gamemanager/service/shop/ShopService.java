/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.shop;

import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.ent.enums.EShopSection;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.InventoryService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.LockUtil;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class ShopService extends BaseService {

    @Autowired
    DailyOfferService dailyOfferService;
    @Autowired
    WishCrytalService wishCrytalService;
    @Autowired
    ChestService chestService;
    @Autowired
    CoinPackService coinPackService;
    @Autowired
    EmeraldPackService emeraldPackService;
    @Autowired
    AdminService adminService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    ProfileService profileService;
    @Autowired
    private LockUtil lockUtil;

    public String getShop() {
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(dailyOfferService.getSectionInfo(userID));
        jsonArray.add(wishCrytalService.getSectionInfo(userID));
        jsonArray.add(chestService.getSectionInfo(userID));
        jsonArray.add(coinPackService.getSectionInfo(userID));
        if(ModuleConfig.IS_TEST){
            jsonArray.add(emeraldPackService.getSectionInfo(userID));
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonArray, NetWorkAPI.GET_SHOP);
    }

    public String refreshShop(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int section = jsonObject.get("section").getAsInt();
        EShopSection eShopSection = EShopSection.findByValue(section);
        JsonArray jsonArray = new JsonArray();
        switch (eShopSection) {
            case DAILY_OFFER:
                dailyOfferService.refreshShop(userID);
                jsonArray.add(dailyOfferService.getSectionInfo(userID));
                break;
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonArray, NetWorkAPI.REFRESH_SHOP);
    }

    public String buyPack(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        String response;
        try {
            if (!lockUtil.acquireLock(session.getId(), NetWorkAPI.BUY_PACK)){
                return ResponseUtils.toErrorBody("Too many request !!!", NetWorkAPI.BUY_PACK);
            }
            
        } catch (Exception e){
            response = ResponseUtils.toErrorBody(e.getMessage(), NetWorkAPI.BUY_PACK);
        } finally {
            lockUtil.releaseLock(session.getId(), NetWorkAPI.BUY_PACK);
        }
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int section = jsonObject.get("section").getAsInt();
        EShopSection eShopSection = EShopSection.findByValue(section);
        int packID = jsonObject.get("packID").getAsInt();
        JsonObject body = new JsonObject();
        List<RewardEnt> list = new ArrayList<>();
        switch (eShopSection) {
            case DAILY_OFFER:
                return dailyOfferService.buyShop(packID, userID);
            case WISH_CRYSTAL:
                return wishCrytalService.buyShop(packID, userID);
            case CHEST:
                return chestService.buyShop(packID, userID);
            case COIN_PACK:
                list.addAll(coinPackService.buyShop(packID, userID));
                body = coinPackService.getSectionInfo(userID);
                JsonArray updateRewardBody = JsonBuilder.buildListReward(list.stream().filter(e -> e.getDelta() > 0).collect(Collectors.toList()));
                JsonArray updateCurrency = JsonBuilder.buildListReward(list);
                return ResponseUtils.toResponseBody(
                        HttpStatus.OK.value(),
                        body,
                        updateRewardBody,
                        ERewardType.NONE.getValue(),
                        updateCurrency).toString();
            case EMERALD_PACK:
                String receipt = null;
                if (jsonObject.get("receipt") != null) {
                     receipt = jsonObject.get("receipt").getAsString();
                }
                list.addAll(emeraldPackService.buyShop(packID, userID, receipt));
                body = emeraldPackService.getSectionInfo(userID);
                break;
        }
        JsonArray updateRewardBody = JsonBuilder.buildListReward(list);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), body, updateRewardBody, ERewardType.NONE.getValue()).toString();
    }

    public String watchAds(HttpServletRequest request) {
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int section = jsonObject.get("section").getAsInt();
        EShopSection eShopSection = EShopSection.findByValue(section);
        JsonObject body = new JsonObject();
        List<RewardEnt> list = new ArrayList<>();
        switch (eShopSection) {
            case CHEST:
                return chestService.watchAds(userID);
        }
        JsonArray updateRewardBody = JsonBuilder.buildListReward(list);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), body, updateRewardBody, ERewardType.NONE.getValue()).toString();
    }
}
