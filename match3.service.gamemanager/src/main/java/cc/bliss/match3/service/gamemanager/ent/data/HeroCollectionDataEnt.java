package cc.bliss.match3.service.gamemanager.ent.data;

import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HeroCollectionDataEnt implements IDataEnt{

    //tracking action info
    private long actionAtMs;
    private String actionType;
    private int heroCardCost;
    private int heroDiamondCost;
    private int heroEmeraldCost;
    private String heroFaction;
    private int heroGoldCost;
    private int heroHp;
    private int heroId;
    private int heroLevel;
    private String heroName;
    private int heroPower;
    private String heroRarity;
    private int heroSkillLevel;
    private int heroTrophy;
    //user info
    private long userId;
    private String userDisplayName;
    private long userGold;
    private long userEmerald;
    private long userTrophy;
    private int userWinLoseStreak;
    private long userCreatedAtMs;
    private int userIsNew;
    private String userPlatform;
    private String userVersion;
    private String userApp;
    private String userDevice;
    private String userIp;
    private String userCountryCode;
    private String userCurrencyCode;
    private String userUtmCampaign;
    private String userUtmSource;
    private String botHardMode;

    @Override
    public String toJsonString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action_at_ms", actionAtMs);
        jsonObject.addProperty("action_type", actionType);
        jsonObject.addProperty("hero_card_cost", heroCardCost);
        jsonObject.addProperty("hero_diamond_cost", heroDiamondCost);
        jsonObject.addProperty("hero_emerald_cost", heroEmeraldCost);
        jsonObject.addProperty("hero_faction", heroFaction);
        jsonObject.addProperty("hero_gold_cost", heroGoldCost);
        jsonObject.addProperty("hero_hp", heroHp);
        jsonObject.addProperty("hero_id", heroId);
        jsonObject.addProperty("hero_level", heroLevel);
        jsonObject.addProperty("hero_name", heroName);
        jsonObject.addProperty("hero_power", heroPower);
        jsonObject.addProperty("hero_rarity", heroRarity);
        jsonObject.addProperty("hero_skill_level", heroSkillLevel);
        jsonObject.addProperty("hero_trophy", heroTrophy);
        jsonObject.addProperty("user_app", userApp);
        jsonObject.addProperty("user_country_code", userCountryCode);
        jsonObject.addProperty("user_created_at_ms", userCreatedAtMs);
        jsonObject.addProperty("user_currency_code", userCurrencyCode);
        jsonObject.addProperty("user_device", userDevice);
        jsonObject.addProperty("user_display_name", userDisplayName);
        jsonObject.addProperty("user_emerald", userEmerald);
        jsonObject.addProperty("user_gold", userGold);
        jsonObject.addProperty("user_id", userId);
        jsonObject.addProperty("user_ip", userIp);
        jsonObject.addProperty("user_is_new", userIsNew);
        jsonObject.addProperty("user_win_lose_streak", userWinLoseStreak);
        jsonObject.addProperty("user_platform", userPlatform);
        jsonObject.addProperty("user_trophy", userTrophy);
        jsonObject.addProperty("user_utm_campaign", userUtmCampaign);
        jsonObject.addProperty("user_utm_source", userUtmSource);
        jsonObject.addProperty("user_version", userVersion);
        jsonObject.addProperty("bot_hard_mode", botHardMode);
        jsonObject.addProperty("server_id", ModuleConfig.SERVER_ID);
        return jsonObject.toString();
    }
}
