package cc.bliss.match3.service.gamemanager.ent.data;

import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GachaDataEnt implements IDataEnt{
    //gacha info
    private String actionType;
    private int actionCost;
    private String actionCostCurrency;
    private long actionAtMs;
    private String id;
    private int bannerId;
    private String bannerName;
    private long startSeasonTime;
    private long endSeasonTime;
    //user info
    private long userId;
    private String userDisplayName;
    private long userAmethyst;
    private long userRoyalAmethyst;
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
    private JsonArray listItemReceive;

    @Override
    public String toJsonString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("user_id", userId);
        jsonObject.addProperty("user_display_name", userDisplayName);
        jsonObject.addProperty("user_amethyst", userAmethyst);
        jsonObject.addProperty("user_royal_amethyst", userRoyalAmethyst);
        jsonObject.addProperty("action_type", actionType);
        jsonObject.addProperty("action_cost", actionCost);
        jsonObject.addProperty("action_cost_currency", actionCostCurrency);
        jsonObject.addProperty("action_at_ms", actionAtMs);
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("banner_id", bannerId);
        jsonObject.addProperty("banner_name", bannerName);
        jsonObject.addProperty("season_start_at_ms", startSeasonTime);
        jsonObject.addProperty("season_end_at_ms", endSeasonTime);
        jsonObject.addProperty("user_gold", userGold);
        jsonObject.addProperty("user_emerald", userEmerald);
        jsonObject.addProperty("user_win_lose_streak", userWinLoseStreak);
        jsonObject.addProperty("user_created_at_ms", userCreatedAtMs);
        jsonObject.addProperty("user_is_new", userIsNew);
        jsonObject.addProperty("user_platform", userPlatform);
        jsonObject.addProperty("user_version", userVersion);
        jsonObject.addProperty("user_app", userApp);
        jsonObject.addProperty("user_trophy", userTrophy);
        jsonObject.addProperty("user_device", userDevice);
        jsonObject.addProperty("user_ip", userIp);
        jsonObject.addProperty("user_country_code", userCountryCode);
        jsonObject.addProperty("user_utm_source", userUtmSource);
        jsonObject.addProperty("user_utm_campaign", userUtmCampaign);
        jsonObject.addProperty("bot_hard_mode", botHardMode);
        jsonObject.add("list_item_receive", listItemReceive);
        jsonObject.addProperty("server_id", ModuleConfig.SERVER_ID);
        return jsonObject.toString();
    }
}
