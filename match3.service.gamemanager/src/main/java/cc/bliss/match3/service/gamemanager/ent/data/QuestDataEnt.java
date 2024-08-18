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
public class QuestDataEnt implements IDataEnt{
    private long userId;
    private String userDisplayName;
    private String questName;
    private int questId;
    private long questExpiredAtMs;
    private String actionType;
    private long actionAtMs;
    private String id;
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

    private int questTypeId;    // EventId
    private String questTypeName;   // EventName

    @Override
    public String toJsonString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("user_id", userId);
        jsonObject.addProperty("user_display_name", userDisplayName);
        jsonObject.addProperty("quest_name", questName);
        jsonObject.addProperty("quest_id", questId);
        jsonObject.addProperty("quest_expired_at_ms", questExpiredAtMs);
        jsonObject.addProperty("action_type", actionType);
        jsonObject.addProperty("action_at_ms", actionAtMs);
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("user_gold", userGold);
        jsonObject.addProperty("user_emerald", userEmerald);
        jsonObject.addProperty("user_win_lose_streak", userWinLoseStreak);
        jsonObject.addProperty("user_created_at_ms", userCreatedAtMs);
        jsonObject.addProperty("user_is_new", userIsNew);
        jsonObject.addProperty("user_platform", userPlatform);
        jsonObject.addProperty("user_version", userVersion);
        jsonObject.addProperty("user_app", userApp);
        jsonObject.addProperty("user_device", userDevice);
        jsonObject.addProperty("user_ip", userIp);
        jsonObject.addProperty("userTrophy", userTrophy);
        jsonObject.addProperty("user_country_code", userCountryCode);
        jsonObject.addProperty("user_utm_source", userUtmSource);
        jsonObject.addProperty("user_utm_campaign", userUtmCampaign);
        jsonObject.addProperty("bot_hard_mode", botHardMode);
        jsonObject.add("list_item_receive", listItemReceive);
        jsonObject.addProperty("server_id", ModuleConfig.SERVER_ID);
        jsonObject.addProperty("quest_type_id", questTypeId);
        jsonObject.addProperty("quest_type_name", questTypeName);
        return jsonObject.toString();
    }
}
