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
public class LoginDataEnt implements IDataEnt{

    private long actionAtMs;
    private String userApp;
    private String userCountryCode;
    private long userCreatedAtMs;
    private String userCurrencyCode;
    private String userDevice;
    private String userDisplayName;
    private String userEmail;
    private long userEmerald;
    private long userGold;
    private long userId;
    private String userIp;
    private int userIsNew;
    private String userLinkAccId;
    private String userLoginMethod;
    private int userWinLoseStreak;
    private String userPlatform;
    private long userTrophy;
    private String userUtmCampaign;
    private String userUtmSource;
    private String userVersion;
    private String botHardMode;

    @Override
    public String toJsonString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action_at_ms", actionAtMs);
        jsonObject.addProperty("user_app", userApp);
        jsonObject.addProperty("user_country_code", userCountryCode);
        jsonObject.addProperty("user_created_at_ms", userCreatedAtMs);
        jsonObject.addProperty("user_currency_code", userCurrencyCode);
        jsonObject.addProperty("user_device", userDevice);
        jsonObject.addProperty("user_display_name", userDisplayName);
        jsonObject.addProperty("user_email", userEmail);
        jsonObject.addProperty("user_emerald", userEmerald);
        jsonObject.addProperty("user_gold", userGold);
        jsonObject.addProperty("user_id", userId);
        jsonObject.addProperty("user_ip", userIp);
        jsonObject.addProperty("user_is_new", userIsNew);
        jsonObject.addProperty("user_link_acc_id", userLinkAccId);
        jsonObject.addProperty("user_login_method", userLoginMethod);
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
