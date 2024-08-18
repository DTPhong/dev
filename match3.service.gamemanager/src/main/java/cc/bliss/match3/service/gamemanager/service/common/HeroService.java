/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import bliss.lib.framework.util.ConvertUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.HeroRepository;
import cc.bliss.match3.service.gamemanager.db.InventoryRepository;
import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.db.match3.HeroWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.InventoryWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.ProfileWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.common.ShardEnt;
import cc.bliss.match3.service.gamemanager.ent.common.TicketEnt;
import cc.bliss.match3.service.gamemanager.ent.common.UpgradeConfigEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.InventoryEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.HeroCollectionTrackingCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.event.QuestEventService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.TicketService;
import cc.bliss.match3.service.gamemanager.util.JsonBuilder;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class HeroService extends BaseService {

    @Autowired
    private AdminService adminService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private QuestEventService questEventService;
    @Autowired
    private DailyQuestService dailyQuestService;
    @Autowired
    private HeroRepository heroRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TicketService ticketService;

    public List<Integer> findAllHeroID() {
        return heroRepository.read().findAll().stream().map(e -> e.getId()).collect(Collectors.toList());
    }

    public List<HeroEnt> findAll() {
        return heroRepository.read().findAll();
    }

    public synchronized String heroUpgrade(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        SessionObj session = adminService.getSession();
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(session.getId());
        if (optional.isPresent()) {
            InventoryEnt inventoryEnt = optional.get();
            Profile profile = profileService.getMinProfileByID(session.getId());
            long goldBeforeAction = profile.getMoney();
            int heroID = jsonObject.get("heroID").getAsInt();
            // get upgrade config
            HeroEnt heroEnt = getHero(heroID);
            if (heroEnt.getLevel() == GameConstant.HERO_MAX_LEVEL) {
                return ResponseUtils.toResponseBody(HttpStatus.CONFLICT.value(), "Đạt cấp tối đa !", NetWorkAPI.HERO_UPGRADE);
            }
            UpgradeConfigEnt upgradeConfigEnt = heroEnt.getLevelUpgradeConfig(heroEnt.getLevel() + 1);
            // clear item
            if (inventoryEnt.getShard(heroID) < upgradeConfigEnt.getShard()) {
                return ResponseUtils.toResponseBody(HttpStatus.CONFLICT.value(), "Không đủ shard !", NetWorkAPI.HERO_UPGRADE);
            }
            if (profile.getMoney() < upgradeConfigEnt.getGold()) {
                return ResponseUtils.toResponseBody(HttpStatus.CONFLICT.value(), "Không đủ gold !", NetWorkAPI.HERO_UPGRADE);
            }
            inventoryEnt.addShard(heroID, -upgradeConfigEnt.getShard());
            inventoryService.updateMoney(profile.getId(), -upgradeConfigEnt.getGold(), EUpdateMoneyType.UPGRADE_HERO);
            // upgrade hero
            inventoryEnt.levelUpHero(heroID, 1);
            inventoryRepository.write().updateHeroAndShard(inventoryEnt.getId(), inventoryEnt.getHero(), inventoryEnt.getShard());

            if (ModuleConfig.IS_DEBUG){
                InventoryEnt afterProfile = inventoryRepository.read().findById(session.getId()).get();
                if (!afterProfile.getHeroArr().toString().contentEquals(inventoryEnt.getHeroArr().toString())){
                    String log = String.format("Hero Upgrade UserID: %s request_changed: %s after: %s",
                            session.getId(), inventoryEnt.getHeroArr().toString(), afterProfile.getHeroArr().toString());
                    GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, InventoryService.class));
                }
            }
            questEventService.listUpgradeEvent(session.getId(), EQuestType.UPGRADE_HERO);
            dailyQuestService.listUpgradeEvent(session.getId(), EQuestType.UPGRADE_HERO);
            int userTrophy = leaderboardService.getProfileTrophy(session.getId());
            GMLocalQueue.addQueue(new HeroCollectionTrackingCmd(producer, profile, userTrophy, heroEnt, "LEVEL_UP_HERO", upgradeConfigEnt.getShard(),
                    upgradeConfigEnt.getGold(), heroEnt.getLevel(), heroEnt.getSkillLevel() + 1, goldBeforeAction, redisTemplateString));
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), getHeroJson(session.getId(), heroID), NetWorkAPI.HERO_UPGRADE);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.name(), NetWorkAPI.HERO_UPGRADE);
    }

    public boolean isShardEnoughToUpMaxLevel(HeroEnt heroEnt, int currentShard) {
        int heroLevel = heroEnt.getLevel();
        int maxLevel = heroEnt.getMaxLevel(heroLevel, currentShard);
        return maxLevel == 10;
    }

    public int getMaxLevel(HeroEnt heroEnt, int currentShard) {
        return heroEnt.getMaxLevel(heroEnt.getLevel(), currentShard);
    }

    public int getRemainShard(HeroEnt heroEnt, int currentShard) {
        return heroEnt.getRemainShard(heroEnt.getLevel(), currentShard);
    }

    public HeroEnt getHeroById(int heroiD) {
        return heroRepository.read().findById(heroiD).get();
    }

    public synchronized String skillUpgrade(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        SessionObj session = adminService.getSession();
        Optional<InventoryEnt> optional = inventoryRepository.read().findById(session.getId());
        if (optional.isPresent()) {
            InventoryEnt inventoryEnt = optional.get();
            Profile profile = profileService.getMinProfileByID(session.getId());
            long goldBeforeAction = profile.getMoney();
            int heroID = jsonObject.get("heroID").getAsInt();
            // get upgrade config
            HeroEnt heroEnt = getHero(heroID);
            if (heroEnt.getSkillLevel() == GameConstant.HERO_MAX_SKILL) {
                return ResponseUtils.toResponseBody(HttpStatus.CONFLICT.value(), "Đạt cấp tối đa !", NetWorkAPI.HERO_SKILL_UPGRADE);
            }
            UpgradeConfigEnt upgradeConfigEnt = heroEnt.getSkillUpgradeConfig(heroEnt.getSkillLevel() + 1);
            if (heroEnt.getLevel() < upgradeConfigEnt.getLevelRequire()) {
                return ResponseUtils.toResponseBody(HttpStatus.CONFLICT.value(), "Không đủ level !", NetWorkAPI.HERO_UPGRADE);
            }
            if (profile.getMoney() < upgradeConfigEnt.getGold()) {
                return ResponseUtils.toResponseBody(HttpStatus.CONFLICT.value(), "Không đủ gold !", NetWorkAPI.HERO_UPGRADE);
            }
            if (heroEnt.getTrophy() < upgradeConfigEnt.getTrophy()) {
                return ResponseUtils.toResponseBody(HttpStatus.CONFLICT.value(), "Không đủ trophy !", NetWorkAPI.HERO_UPGRADE);
            }
            // upgrade hero
            inventoryEnt.levelUpSkill(heroID, 1);
            inventoryRepository.write().updateHero(inventoryEnt.getId(), inventoryEnt.getHero());

            if (ModuleConfig.IS_DEBUG){
                InventoryEnt afterProfile = inventoryRepository.read().findById(session.getId()).get();
                if (!afterProfile.getHeroArr().toString().contentEquals(inventoryEnt.getHeroArr().toString())){
                    String log = String.format("Skill Upgrade UserID: %s request_changed: %s after: %s",
                            session.getId(), inventoryEnt.getHeroArr().toString(), afterProfile.getHeroArr().toString());
                    GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, InventoryService.class));
                }
            }
            inventoryService.claimItem(profile.getId(), ERewardType.GOLD, -upgradeConfigEnt.getGold(), EUpdateMoneyType.UPGRADE_SKILL);
            int userTrophy = leaderboardService.getProfileTrophy(session.getId());
            GMLocalQueue.addQueue(new HeroCollectionTrackingCmd(producer, profile, userTrophy, heroEnt, "LEVEL_UP_SKILL", 0,
                    upgradeConfigEnt.getGold(), heroEnt.getLevel(), heroEnt.getSkillLevel() + 2, goldBeforeAction, redisTemplateString));
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), getHeroJson(session.getId(), heroID), NetWorkAPI.HERO_SKILL_UPGRADE);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.name(), NetWorkAPI.HERO_SKILL_UPGRADE);
    }

    public HeroEnt getSelectHero() {
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        return getSelectHero(userID);
    }

    public HeroEnt getSelectHero(long userID) {
        List<HeroEnt> ownedHero = inventoryService.getListOwnedHero(userID);
        Profile profile = profileService.getMinProfileByID(userID);
        HeroEnt heroEnt = heroRepository.read().findById(profile.getSelectHero()).get();

        boolean isUsed = profile.getSelectHero() == heroEnt.getId();
        boolean isOwned = ownedHero.stream().anyMatch(e -> e.getId() == heroEnt.getId());
        int level = 1;
        int skillLevel = 0;
        if (isOwned) {
            HeroEnt owned = ownedHero.stream().filter(e -> e.getId() == heroEnt.getId()).findFirst().get();
            level = owned.getLevel();
            skillLevel = owned.getSkillLevel();
        }
        long curShard = inventoryService.getHeroShard(userID, profile.getSelectHero());

        int trophy = leaderboardService.getHeroTrophy(userID, profile.getSelectHero());
        heroEnt.setTransientField(level, isUsed, isOwned, curShard, trophy, skillLevel);

        return heroEnt;
    }

    public HeroEnt getHero(int heroID) {
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        return getHero(userID, heroID);
    }

    public HeroEnt getHero(long userID, int heroID) {
        List<HeroEnt> ownedHero = inventoryService.getListOwnedHero(userID);
        HeroEnt heroEnt = heroRepository.read().findById(heroID).get();

        boolean isUsed = heroID == heroEnt.getId();
        boolean isOwned = ownedHero.stream().anyMatch(e -> e.getId() == heroEnt.getId());
        int level = 1;
        int skillLevel = 0;
        if (isOwned) {
            HeroEnt owned = ownedHero.stream().filter(e -> e.getId() == heroEnt.getId()).findFirst().get();
            level = owned.getLevel();
            skillLevel = owned.getSkillLevel();
        }
        long curShard = inventoryService.getHeroShard(userID, heroID);
        int trophy = leaderboardService.getHeroTrophy(userID, heroID);
        heroEnt.setTransientField(level, isUsed, isOwned, curShard, trophy, skillLevel);
        return heroEnt;
    }

    public JsonObject getSelectHeroJson(long userID) {
        HeroEnt heroEnt = getSelectHero(userID);
        return JsonBuilder.buildHero(heroEnt);
    }

    public JsonObject getHeroJson(long userID, int heroID) {
        HeroEnt heroEnt = getHero(userID, heroID);
        return JsonBuilder.buildHero(heroEnt);
    }

    public List<HeroEnt> getListHeroEnt(int heroClass) {
        SessionObj session = adminService.getSession();
        Profile profile = null;
        List<HeroEnt> ownedHero = new ArrayList<>();
        List<ShardEnt> listShard = new ArrayList<>();
        Map<Integer, Integer> mapTrophy = Collections.EMPTY_MAP;
        List<HeroEnt> listHero = heroRepository.read().findAll();
        if (session != null) {
            long userID = session.getId();
            profile = profileService.getMinProfileByID(userID);
            ownedHero = inventoryService.getListOwnedHero(userID);
            listShard = inventoryService.getHeroShard(userID);
            List<Integer> listHeroIDs = listHero.stream().map(e -> e.getId()).collect(Collectors.toList());
            mapTrophy = leaderboardService.getHeroTrophy(userID, listHeroIDs);
        }

        for (HeroEnt heroEnt : listHero) {
            if (heroClass != 0 && heroClass != heroEnt.getHeroClass().ordinal()) {
                continue;
            }
            boolean isUsed = session != null ? profile.getSelectHero() == heroEnt.getId() : false;
            boolean isOwned = ownedHero.stream().anyMatch(e -> e.getId() == heroEnt.getId());
            int level = 0;
            int skillLevel = 0;
            if (isOwned) {
                HeroEnt owned = ownedHero.stream().filter(e -> e.getId() == heroEnt.getId()).findFirst().get();
                level = owned.getLevel();
                skillLevel = owned.getSkillLevel();
            }
            long curShard = listShard.stream().anyMatch(e -> e.getHeroID() == heroEnt.getId())
                    ? listShard.stream().filter(e -> e.getHeroID() == heroEnt.getId()).findFirst().get().getAmount()
                    : 0;

            heroEnt.setTransientField(level, isUsed, isOwned, curShard, mapTrophy.getOrDefault(heroEnt.getId(), 0), skillLevel);
        }
        return listHero;
    }

    /**
     * @param heroClass 0=default WARRIOR, ASSASSIN, GUARDIAN, ENCHANTER, MAGE
     * @return
     */
    public JsonArray getHeroJsonArr(int heroClass) {
        JsonArray jsonArray = new JsonArray();
        List<HeroEnt> listHero = getListHeroEnt(heroClass);
        for (HeroEnt heroEnt : listHero) {
            JsonObject hero = JsonBuilder.buildHero(heroEnt);
            jsonArray.add(hero);
        }
        return jsonArray;
    }

    private String getListHero(JsonObject jsonObject) {
        int heroClass = 0;
        if (jsonObject.has("heroClass")) {
            heroClass = jsonObject.get("heroClass").getAsInt();
        }
        JsonArray jsonArray = getHeroJsonArr(heroClass);
        JsonObject response = JsonBuilder.toSearchResponseData(jsonArray, jsonArray.size(), 1, 0, jsonArray.size());
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.HERO_LIST_BY_USER);
    }

    public Map<Integer, List<Integer>> getMapAllHeroByRarity() {
        Map<Integer, List<Integer>> heroMap = new HashMap<>();
        List<HeroEnt> heroEntList = heroRepository.read().findAll();
        for (HeroEnt heroEnt : heroEntList) {
            int rariry = heroEnt.getRarity().ordinal() + 13; // convert ERARITY => EREWARD_CARD
            if (heroMap.containsKey(rariry)){
                heroMap.get(rariry).add(heroEnt.getId());
            } else {
                List<Integer> heroIDList = new ArrayList<>();
                heroIDList.add(heroEnt.getId());
                heroMap.put(rariry, heroIDList);
            }
        }
        return heroMap;
    }

    public String getListHero(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        return getListHero(jsonObject);
    }

    public String getHeroByIdResponse(int heroID) {
        HeroEnt heroEnt = getHero(heroID);
        JsonObject response = JsonBuilder.buildHero(heroEnt);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.HERO_LIST_BY_USER);
    }

    /**
     * Trả về thông tin heroID của userID
     * Nếu UserID là người -> thông tin hero sẽ lấy từ DB
     * Nếu UserID là Bot -> thông tin hero sẽ lấy từ matching ticket
     * @param request
     * @return
     */
    public String getHeroByIdResponse(HttpServletRequest request) {
        int heroID = ConvertUtils.toInt(request.getParameter("heroID"));
        int userID = ConvertUtils.toInt(request.getParameter("userID"));
        HeroEnt heroEnt = getHero(userID, heroID);

        // Nếu userId là Bot -> update heroLevel theo thông tin của matching ticket
        Profile userProfile = profileService.getProfileByID(userID);
        if(userProfile.getBotType() != EBotType.USER.ordinal()){
            long matchingTicketId = ConvertUtils.toInt(request.getParameter("opponentID"));
            if(matchingTicketId > 0){
                int botHeroLv = ticketService.getBotHeroLv(matchingTicketId);
                if(botHeroLv >= 1){
                    heroEnt.setLevel(botHeroLv);
                }
            } else{
                System.out.println("GAME SV chưa gắn opponent ID khi getHero của user, request: "+ request);
            }
        }

        JsonObject response = JsonBuilder.buildHero(heroEnt);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.HERO_LIST_BY_USER);
    }

    public String getSelectHeroResponse() {
        HeroEnt heroEnt = getSelectHero();
        JsonObject response = JsonBuilder.buildHero(heroEnt);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.HERO_SELECT);
    }

    public String getSelectHeroResponse(int userID) {
        HeroEnt heroEnt = getSelectHero(userID);
        JsonObject response = JsonBuilder.buildHero(heroEnt);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.HERO_SELECT);
    }

    public String selectHero(HttpServletRequest request) {
        JsonObject jsonObject = RequestUtils.requestToJson(request);
        int heroID = jsonObject.get("heroID").getAsInt();
        SessionObj session = adminService.getSession();
        long userID = session.getId();
        Profile profile = profileRepository.read().findById(userID).get();

        profile.setSelectHero(heroID);
        profileRepository.write().updateSelectHero(profile.getId(), profile.getSelectHero());

        if (ModuleConfig.IS_DEBUG){
            Profile afterProfile = profileRepository.read().findById(userID).get();
            if (afterProfile.getSelectHero() != profile.getSelectHero()){
                String log = String.format("Select Hero UserID: %s request_changed: %s after: %s",
                        session.getId(), profile.getSelectHero(), afterProfile.getSelectHero());
                GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, InventoryService.class));
            }
        }

        return getListHero(jsonObject);
    }

    public List<Integer> getHeroIdsByRarity(EHeroRarity rarity) {
        return heroRepository.read()
                .findAllByRarityEquals(rarity)
                .stream().map(HeroEnt::getId).collect(Collectors.toList());
    }

}
