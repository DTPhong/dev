/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.system;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.ent.common.RewardTrophyRoad;
import cc.bliss.match3.service.gamemanager.ent.enums.EHeroClass;
import cc.bliss.match3.service.gamemanager.ent.enums.EHeroRarity;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ConfigEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TrophyRoadMileStoneEnt;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.event.GachaService;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Phong
 */
@Service
public class ConfigService extends BaseService {

    @Autowired
    private HeroService heroService;
    @Autowired
    private GachaService gachaService;

    public String getConfig() {
        JsonObject response = new JsonObject();
        {
            // gem
            Optional<ConfigEnt> gemConfig = configRepository.read().findById("GEM_CONFIG");
            if (gemConfig.isPresent()) {
                JsonArray data = JSONUtil.DeSerialize(gemConfig.get().getValue(), JsonArray.class);
                response.add("combatConfig", data);
            }
        }
        {
            // gem ratio
            Optional<ConfigEnt> gemConfig = configRepository.read().findById("GEM_RATIO_CONFIG");
            if (gemConfig.isPresent()) {
                JsonObject data = JSONUtil.DeSerialize(gemConfig.get().getValue(), JsonObject.class);
                response.add("gemRatioConfig", data);
            }
        }
        {
            // hero
            JsonArray data = heroService.getHeroJsonArr(0);
            response.add("heroConfig", data);
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.GET_CONFIG);
    }

    public Map<Integer, Integer> getPityConfig(String configKey){
        ConfigEnt configEnt = configRepository.read().getById(configKey);
        Type type = new TypeToken<Map<Integer, Integer>>() {
        }.getType();
        return JSONUtil.DeSerialize(configEnt.getValue(), type);
    }

    public ConfigEnt getConfig(String configKey){
        return configRepository.read().getById(configKey);
    }

    @Caching(evict = {
            @CacheEvict(value = "version", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "version_optional", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "config", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "config_optional", allEntries = true, cacheManager = "cacheManagerObject")
    })
    public String updateConfig(HttpServletRequest request) {
        JsonArray jsonArray = RequestUtils.requestToArr(request);
        {
            // gem
            Optional<ConfigEnt> gemConfig = configRepository.read().findById("GEM_CONFIG");
            JsonArray dataArr = new JsonArray();
            if (gemConfig.isPresent()) {
                for (JsonElement jsonElement : jsonArray) {
                    if (jsonElement.getAsJsonObject().has("ATK")) {
                        JsonObject data = new JsonObject();
                        if (jsonElement.getAsJsonObject().get("1000").getAsString().contentEquals("Formula")) {
                            continue;
                        }
                        data.addProperty("classHero", jsonElement.getAsJsonObject().get("1000").getAsString());
                        data.addProperty("atk", jsonElement.getAsJsonObject().get("ATK").getAsFloat());
                        data.addProperty("ap", jsonElement.getAsJsonObject().get("AP").getAsFloat());
                        data.addProperty("ar", jsonElement.getAsJsonObject().get("AR").getAsFloat());
                        data.addProperty("hp", jsonElement.getAsJsonObject().get("HP").getAsFloat());
                        data.addProperty("mp", jsonElement.getAsJsonObject().get("MP").getAsFloat());
                        dataArr.add(data);
                    }
                }
                gemConfig.get().setValue(dataArr.toString());
                configRepository.write().saveAndFlush(gemConfig.get());
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UPDATE_CONFIG);
    }

    public String updateHeroConfig(HttpServletRequest request) {
        JsonArray jsonArray = RequestUtils.requestToArr(request);
        for (JsonElement jsonElement : jsonArray) {
            if (jsonElement.getAsJsonObject().has("HERO ID")) {
                int heroID = jsonElement.getAsJsonObject().get("HERO ID").getAsInt();
                Optional<HeroEnt> optional = heroRepository.read().findById(heroID);
                if (optional.isPresent()) {
                    HeroEnt heroEnt = optional.get();
                    if (jsonElement.getAsJsonObject().has("SHIELD")) {
                        heroEnt.setShield(jsonElement.getAsJsonObject().get("SHIELD").getAsInt());
                    }
                    if (jsonElement.getAsJsonObject().has("POWER")) {
                        heroEnt.setPower(jsonElement.getAsJsonObject().get("POWER").getAsInt());
                    }
                    if (jsonElement.getAsJsonObject().has("HP")) {
                        heroEnt.setHp(jsonElement.getAsJsonObject().get("HP").getAsInt());
                    }
                    if (jsonElement.getAsJsonObject().has("CLASS")) {
                        heroEnt.setHeroClass(EHeroClass.findByName(jsonElement.getAsJsonObject().get("CLASS").getAsString()));
                    }
                    if (jsonElement.getAsJsonObject().has("RARITY")) {
                        heroEnt.setRarity(EHeroRarity.findByName(jsonElement.getAsJsonObject().get("RARITY").getAsString()));
                    }
                    if (jsonElement.getAsJsonObject().has("HERO NAME")) {
                        heroEnt.setTitle(jsonElement.getAsJsonObject().get("HERO NAME").getAsString());
                    }
                    heroRepository.write().saveAndFlush(heroEnt);
                }
            }
        }
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UPDATE_CONFIG);
    }

    @Caching(evict = {
            @CacheEvict(value = "version", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "version_optional", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "config", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "config_optional", allEntries = true, cacheManager = "cacheManagerObject")
    })
    public String updateGemRatioConfig(HttpServletRequest request) {
        Optional<ConfigEnt> gemConfig = configRepository.read().findById("GEM_RATIO_CONFIG");
        if (!gemConfig.isPresent()) {
            return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.name(), NetWorkAPI.UPDATE_CONFIG);
        }
        JsonArray jsonArray = RequestUtils.requestToArr(request);
        JsonObject data = new JsonObject();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (!jsonObject.has("Test prop")) {
                continue;
            }
            switch (jsonObject.get("Test prop").getAsString()) {
                case "ATK RED": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("redGemRatio", ratio);
                    break;
                }
                case "ATK GREEN": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("greenGemRatio", ratio);
                    break;
                }
                case "ATK BLUE": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("blueGemRatio", ratio);
                    break;
                }
                case "ATK YELLOW": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("yellowGemRatio", ratio);
                    break;
                }
                case "HEAL": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("hpGemRatio", ratio);
                    break;
                }
                case "MANA RED": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("redManaRatio", ratio);
                    break;
                }
                case "MANA GREEN": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("greenManaRatio", ratio);
                    break;
                }
                case "MANA BLUE": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("blueManaRatio", ratio);
                    break;
                }
                case "MANA YELLOW": {
                    double ratio = jsonObject.get("Gem Ratio (GR)").getAsDouble();
                    data.addProperty("yellowManaRatio", ratio);
                    break;
                }
                default:
                    break;
            }
        }
        gemConfig.get().setValue(data.toString());
        configRepository.write().saveAndFlush(gemConfig.get());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UPDATE_CONFIG);
    }

    @Caching(evict = {
            @CacheEvict(value = "version", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "version_optional", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "config", allEntries = true, cacheManager = "cacheManagerObject"),
            @CacheEvict(value = "config_optional", allEntries = true, cacheManager = "cacheManagerObject")
    })
    public String updatePityConfig(HttpServletRequest request){
        JsonArray jsonArray = RequestUtils.requestToArr(request);
        Map<Integer, Integer> mapEpic = new HashMap<>();
        Map<Integer, Integer> mapLegend = new HashMap<>();
        for (JsonElement jsonElement : jsonArray){
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("Pity")){
                int pity = jsonObject.get("Pity").getAsInt();
                if (jsonObject.has("Epic")){
                    mapEpic.put(pity, jsonObject.get("Epic").getAsInt());
                }
                if (jsonObject.has("Legend")){
                    mapLegend.put(pity, jsonObject.get("Legend").getAsInt());
                }
            }
        }
        {
            ConfigEnt configEnt = new ConfigEnt();
            configEnt.setId("PITY_EPIC_CONFIG");
            configEnt.setValue(JSONUtil.Serialize(mapEpic));
            configRepository.write().saveAndFlush(configEnt);
        }
        {
            ConfigEnt configEnt = new ConfigEnt();
            configEnt.setId("PITY_LEGENDARY_CONFIG");
            configEnt.setValue(JSONUtil.Serialize(mapLegend));
            configRepository.write().saveAndFlush(configEnt);
        }
        gachaService.refreshConfig();
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UPDATE_CONFIG);
    }

    @CacheEvict(value = "trophyroad", allEntries = true)
    public String updateTrophyRoadConfig(HttpServletRequest request) {
        JsonArray jsonArray = RequestUtils.requestToArr(request);

        for (JsonElement jsonElement : jsonArray){
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String type = jsonObject.get("Type").getAsString();
            int milestone = jsonObject.get("Trophy").getAsInt();

            String reward = jsonObject.get("Reward").getAsString();

            int id = jsonObject.get("Id").getAsInt();
            String config = null;
            if (jsonObject.get("Config") != null) {
                config = jsonObject.get("Config").getAsString();
            }
            TrophyRoadMileStoneEnt trophyRoadMileStoneEnt = new TrophyRoadMileStoneEnt();
            if (type.equals("FREE")) {
                trophyRoadMileStoneEnt.setType(0);
            } else if (type.equals("ADS")) {
                trophyRoadMileStoneEnt.setType(1);
            } else {
                trophyRoadMileStoneEnt.setType(2);
            }
            trophyRoadMileStoneEnt.setMilestone(milestone);

            if (!reward.equals("None")) {
                int amount = jsonObject.get("Amount").getAsInt();
                RewardTrophyRoad rewardTrophyRoad = setRewardType(reward, amount, config);
                List<RewardTrophyRoad> rewardTrophyRoadList = new ArrayList<>();
                rewardTrophyRoadList.add(rewardTrophyRoad);
                String result = ModuleConfig.GSON_BUILDER.toJson(rewardTrophyRoadList);
                trophyRoadMileStoneEnt.setRewards(result);
                trophyRoadMileStoneEnt.setId(id);

                Optional<TrophyRoadMileStoneEnt> optional = trophyRoadRepository.read().findById(id);
                if (optional.isPresent()) {
                    TrophyRoadMileStoneEnt trophyRoadMileStone = optional.get();
                    trophyRoadMileStone.setMilestone(trophyRoadMileStoneEnt.getMilestone());
                    trophyRoadMileStone.setType(trophyRoadMileStoneEnt.getType());
                    trophyRoadMileStone.setRewards(result);
                    trophyRoadRepository.write().saveAndFlush(trophyRoadMileStone);
                } else {
                    trophyRoadRepository.write().saveAndFlush(trophyRoadMileStoneEnt);
                }
            }
        }


        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), HttpStatus.OK.name(), NetWorkAPI.UPDATE_CONFIG);
    }

    private RewardTrophyRoad setRewardType(String reward, int delta, String config) {
        RewardTrophyRoad rewardTrophyRoad = new RewardTrophyRoad();
        switch (reward) {
            case "Gold":
                rewardTrophyRoad.setRewardType(ERewardType.PACK_GOLD_1.getValue());
                break;
            case "Emerald":
                rewardTrophyRoad.setRewardType(ERewardType.COMMON_EMERALD.getValue());
                break;
            case "Vip Gacha":
                rewardTrophyRoad.setRewardType(ERewardType.ROYAL_AMETHYST.getValue());
                break;
            case "Normal Gacha":
                rewardTrophyRoad.setRewardType(ERewardType.AMETHYST.getValue());
                break;
            case "Rare Random Card":
                rewardTrophyRoad.setRewardType(ERewardType.RARE_CARD.getValue());
                break;
            case "Epic Random Card":
                rewardTrophyRoad.setRewardType(ERewardType.EPIC_CARD.getValue());
                break;
            case "Legend Random Card":
                rewardTrophyRoad.setRewardType(ERewardType.LEGENDARY_CARD.getValue());
                break;
            case "Ivy Card":
            case "Aaron Card":
            case "Victoria Card":
            case "Nataluna Card":
            case "Noah Card":
            case "Morgan Card":
            case "Bucky Card":
            case "Xiao Bao Card":
            case "Bermus Card":
            case "Naga Card":
                rewardTrophyRoad.setRewardType(ERewardType.HERO_CARD.getValue());
                break;
            case "Ivy":
            case "Aaron":
            case "Victoria":
            case "Nataluna":
            case "Kelsey":
            case "Frozz":
            case "Noah":
            case "Morgan":
            case "Bucky":
            case "Xiao Bao":
            case "Giong":
            case "Bermus":
            case "Naga":
            case "Aine":
                rewardTrophyRoad.setRewardType(ERewardType.HERO.getValue());
                break;
            case "WoodenChest":
                rewardTrophyRoad.setRewardType(ERewardType.WOODEN_CHEST.getValue());
                break;
            case "Beginner Chest":
                rewardTrophyRoad.setRewardType(ERewardType.BEGINNER_CHEST.getValue());
                break;
            case "SilverChest":
                rewardTrophyRoad.setRewardType(ERewardType.SLIVER_CHEST.getValue());
                break;
            case "GoldenChest":
                rewardTrophyRoad.setRewardType(ERewardType.GOLDEN_CHEST.getValue());
                break;
            case "ShinyJewelChest":
                rewardTrophyRoad.setRewardType(ERewardType.SHINY_JEWEL_CHEST.getValue());
                break;
            case "BlackCrownChest":
                rewardTrophyRoad.setRewardType(ERewardType.BLACK_CROWN_CHEST.getValue());
                break;
            case "Thumbnail":
                rewardTrophyRoad.setRewardType(ERewardType.THUMBNAIL.getValue());
                break;
            default:
                break;
        }
        int ref = getRefByConfig(config, rewardTrophyRoad.getRewardType(), reward);
        rewardTrophyRoad.setRef(ref);
        rewardTrophyRoad.setDelta(delta);
        return rewardTrophyRoad;
    }
    private int getRefByConfig(String config, int rewardType, String type) {
        int ref = 0;
        ERewardType eRewardType = ERewardType.findByValue(rewardType);
        String heroName;
        switch (eRewardType) {
            case HERO_CARD:
                heroName = type.replaceFirst(" Card", "");
                ref = heroRepository.read().findByTitle(heroName).getId();
                return ref;
            case HERO:
                ref = heroRepository.read().findByTitle(type).getId();
                return ref;
            default:
                if (config != null) {
                    ref = heroRepository.read().findByTitle(config).getId();
                } else {
                    return ref;
                }
                return ref;
        }
    }

}
