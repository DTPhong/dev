/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.InventoryRepository;
import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.db.match3.HeroWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.InventoryWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.ProfileWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.ShardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.UpdateMoneyResult;
import cc.bliss.match3.service.gamemanager.ent.common.UpgradeConfigEnt;
import cc.bliss.match3.service.gamemanager.ent.data.UserPropertyChangeEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.InventoryEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.HeroCollectionTrackingCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.ListenClaimFeatureCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static cc.bliss.match3.service.gamemanager.ent.enums.ERewardType.HERO_CARD;

/**
 * @author Phong
 */
@Service
public class InventoryService extends BaseService {

    @Autowired
    private AdminService adminService;
    @Autowired
    private HeroService heroService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private DailyQuestService dailyQuestService;
    @Autowired
    private ProfileService profileService;

    public int countOwnedHero(long userID){
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(userID);
        if (optional.isPresent()) {
            JsonArray heroJson = optional.get().getHeroArr();
            return heroJson.size();
        }
        return 0;
    }

    public List<HeroEnt> getListOwnedHero(long userID) {
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(userID);
        List<HeroEnt> heroEnts = new ArrayList<>();
        if (optional.isPresent()) {
            JsonArray heroJson = optional.get().getHeroArr();
            for (JsonElement jsonElement : heroJson) {
                HeroEnt heroEnt = new HeroEnt();
                heroEnt.setId(jsonElement.getAsJsonObject().get("id").getAsInt());
                heroEnt.setLevel(jsonElement.getAsJsonObject().get("level").getAsInt());
                heroEnt.setSkillLevel(jsonElement.getAsJsonObject().has("skillLevel") ? jsonElement.getAsJsonObject().get("skillLevel").getAsInt() : 0);
                heroEnts.add(heroEnt);
            }
        }
        if (!optional.isPresent()) {
            initHero(userID);
        }
        return heroEnts;
    }

    public long getHeroShard(long userID, int heroID) {
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(userID);
        if (optional.isPresent()) {
            List<ShardEnt> shardEnts = optional.get().getShardArr();
            for (ShardEnt shardEnt : shardEnts) {
                if (shardEnt.getHeroID() == heroID) {
                    return shardEnt.getAmount();
                }
            }
        }
        return 0;
    }

    public List<ShardEnt> getHeroShard(long userID) {
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(userID);
        if (optional.isPresent()) {
            return optional.get().getShardArr();
        }
        return Collections.EMPTY_LIST;
    }

    public long addShard(long userID, int heroID, long delta) {
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(userID);
        InventoryEnt inventoryEnt;
        if (optional.isPresent()) {
            inventoryEnt = optional.get();
        } else {
            inventoryEnt = new InventoryEnt();
            inventoryEnt.setId(userID);
            insertMatch3SchemaData(inventoryEnt);
        }

        long before = inventoryEnt.getShard(heroID);
        queueQuestUpdates(userID, heroID, delta);
        if (!inventoryEnt.getHeroIdArr().contains(heroID)) {
            HeroEnt heroEnt = heroRepository.read().findById(heroID).get();
            UpgradeConfigEnt upgradeConfigEnt = heroEnt.getDefaultLeveConfig();
            int level = upgradeConfigEnt.getLevel();
            inventoryEnt.addHero(heroID, level);
            delta--;
        }
        inventoryEnt.addShard(heroID, delta);
        inventoryRepository.write().updateHeroAndShard(inventoryEnt.getId(), inventoryEnt.getHero(), inventoryEnt.getShard());
        return before + delta;
    }

    public int addHero(long userID, int heroID) {
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(userID);
        InventoryEnt inventoryEnt;
        if (optional.isPresent()) {
            inventoryEnt = optional.get();
        } else {
            inventoryEnt = new InventoryEnt();
            inventoryEnt.setId(userID);
            insertMatch3SchemaData(inventoryEnt);
        }
        HeroEnt heroEnt = heroRepository.read().findById(heroID).get();
        UpgradeConfigEnt upgradeConfigEnt = heroEnt.getDefaultLeveConfig();
        int level = upgradeConfigEnt.getLevel();
        inventoryEnt.addHero(heroID, level);
        inventoryRepository.write().updateHero(inventoryEnt.getId(), inventoryEnt.getHero());

        Profile profile = profileService.getMinProfileByID(userID);
        long goldBeforeAction = profile.getMoney();
        int userTrophy = leaderboardService.getProfileTrophy(userID);
        GMLocalQueue.addQueue(new HeroCollectionTrackingCmd(producer, profile, userTrophy, heroEnt, "UNLOCK_HERO", 1, 0, level, 1, goldBeforeAction, redisTemplateString));
        return 1;
    }

    private void initHero(long userID) {
        Optional<Profile> optionalProfile = profileRepository.read().findById(userID);
        if (optionalProfile.isPresent()) {
            // add hero
            addHero(userID,ModuleConfig.HERO_DEFAULT_ID);
            // add gacha
            Profile profile = optionalProfile.get();
            profile.setRoyalAmethyst(1);
            profileRepository.write().updateRoyalAmethystByProfileId(profile.getId(), 1);
            // init ftue quest
            dailyQuestService.initDailyQuestFTUE(profile);
        }
    }

    public List<RewardEnt> updateMoney(long userID, long delta, EUpdateMoneyType updateMoneyType) {
        return claimItem(userID, ERewardType.GOLD, delta, updateMoneyType);
    }

    public List<RewardEnt> claimItem(long userID, ERewardType eRewardType, long quantity, EUpdateMoneyType updateMoneyType) {
        return claimItem(userID, eRewardType, quantity, 0, updateMoneyType);
    }

    public List<RewardEnt> claimItem(long userID, ERewardType eRewardType, long quantity, int refID, EUpdateMoneyType updateMoneyType) {
        List<RewardEnt> rewardEnts = new ArrayList<>();
        Profile profile = profileService.getMinProfileByID(userID);
        long after = 0;
        long before = 0;

        switch (eRewardType) {
            case GOLD:
            case PACK_GOLD_1:
            case PACK_GOLD_2:
            case PACK_GOLD_3:
            case PACK_GOLD_4:
            case PACK_GOLD_5:
            case PACK_GOLD_6:
                UpdateMoneyResult goldResult = profileService.updateMoney(userID, quantity, updateMoneyType);
                after = goldResult.getAfter();
                before = goldResult.getBefore();
                break;

            case EMERALD:
            case COMMON_EMERALD:
            case PACK_EMERALD_1:
            case PACK_EMERALD_2:
            case PACK_EMERALD_3:
            case PACK_EMERALD_4:
            case PACK_EMERALD_5:
            case PACK_EMERALD_6:
                UpdateMoneyResult emeraldResult = profileService.updateEmerald(userID, quantity, updateMoneyType);
                after = emeraldResult.getAfter();
                before = emeraldResult.getBefore();
                break;

            case ROYAL_AMETHYST:
            case CASKET_OF_AMETHYST:
            case TUB_OF_AMETHYST:
                UpdateMoneyResult amethystResult = profileService.updateRoyalAmethyst(userID, quantity, updateMoneyType);
                after = amethystResult.getAfter();
                before = amethystResult.getBefore();
                break;

            case AMETHYST:
                UpdateMoneyResult regularAmethystResult = profileService.updateAmethyst(userID, quantity, updateMoneyType);
                after = regularAmethystResult.getAfter();
                before = regularAmethystResult.getBefore();
                break;

            case RARE_CARD:
            case LEGENDARY_CARD:
            case EPIC_CARD:
                refID = getOrAssignHeroRefID(eRewardType, refID);
                after = addShard(userID, refID, quantity);
                before = after - quantity;
                eRewardType = HERO_CARD;
                break;

            case HERO_CARD:
                after = addShard(userID, refID, quantity);
                before = after - quantity;
                break;
        }

        RewardEnt rewardEnt = new RewardEnt();
        rewardEnt.setERewardType(eRewardType);
        rewardEnt.setAfter(after);
        rewardEnt.setDelta(quantity);
        rewardEnt.setRef(refID);
        rewardEnt.setBefore(before);
        rewardEnts.add(rewardEnt);
        return rewardEnts;
    }

    private int getOrAssignHeroRefID(ERewardType eRewardType, int refID) {
        if (refID == 0) {
            List<Integer> heroIDs = heroService
                    .findAll()
                    .stream()
                    .filter(e -> e.getRarity().equals(getRarityForRewardType(eRewardType)))
                    .map(HeroEnt::getId)
                    .collect(Collectors.toList());
            refID = heroIDs.get(RandomUtils.RAND.nextInt(heroIDs.size()));
        }
        return refID;
    }

    private EHeroRarity getRarityForRewardType(ERewardType eRewardType) {
        switch (eRewardType) {
            case RARE_CARD:
                return EHeroRarity.MYTHIC;
            case LEGENDARY_CARD:
                return EHeroRarity.LEGENDARY;
            case EPIC_CARD:
                return EHeroRarity.EPIC;
            default:
                throw new IllegalArgumentException("Invalid ERewardType for hero card: " + eRewardType);
        }
    }

    private void queueQuestUpdates(long userID, int refID, long quantity) {
        EHeroRarity rarity = heroRepository.read().findById(refID).get().getRarity();
        if (rarity.equals(EHeroRarity.MYTHIC)) {
            GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, userID, EQuestType.COLLECT_RARE_CARD, quantity));
        } else if (rarity.equals(EHeroRarity.EPIC)) {
            GMLocalQueue.addQueue(new ListenClaimFeatureCmd(dailyQuestService, userID, EQuestType.COLLECT_EPIC_CARD, quantity));
        }
    }
}
