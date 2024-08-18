package cc.bliss.match3.service.gamemanager.ent.data;

import bliss.lib.framework.util.ConvertUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserPropertyChangeEnt implements IDataEnt{

    String userApp = "";
    String userCountryCode = "";
    long userCreatedAtMs;
    String userDevice = "";
    String userDisplayName = "";
    String userEmail = "";
    long userEmerald;
    long userGold;
    long userId;
    String userIp = "";
    int userIsNew;
    String userAppleLinkId = "";
    String userGgLinkId = "";
    private String userLoginMethod = "";
    private int userWinLoseStreak;
    private String userPlatform = "";
    private int userTrophy;
    private String userUtmCampaign = "";
    private String userUtmSource = "";
    private String userVersion = "";
    int changeItemId;
    String changeItemName = "";
    long changeItemAmount;
    long actionAtMs;
    long beforeChangeItemAmount;
    long afterChangeItemAmount;
    String changeType = "";
    long changeRefId;
    String changeReason = "";
    int adminId;
    String adminName = "";
    String adminEmail = "";
    String adminReason = "";

    public static UserPropertyChangeEnt buildUserPropertyChangeEnt(
            Profile profile, String changeItemName, int refId,
            long quantity, long before, long after, String changeType){
        UserPropertyChangeEnt userPropertyChangeEnt = new UserPropertyChangeEnt();
        userPropertyChangeEnt.setActionAtMs(System.currentTimeMillis());
        userPropertyChangeEnt.setUserApp(profile.getPackageID());
        userPropertyChangeEnt.setUserCreatedAtMs(profile.getDateCreated().getTime());
        userPropertyChangeEnt.setUserDevice(profile.getDeviceID());
        userPropertyChangeEnt.setUserDisplayName(profile.getDisplayName());
        userPropertyChangeEnt.setUserEmail(profile.getGmail());
        userPropertyChangeEnt.setUserEmerald(profile.getEmerald());
        userPropertyChangeEnt.setUserGold(profile.getMoney());
        userPropertyChangeEnt.setUserId(profile.getId());
        // Not implement yet
        userPropertyChangeEnt.setUserIp("");
        userPropertyChangeEnt.setUserIsNew(0);
        userPropertyChangeEnt.setUserCountryCode("");
        userPropertyChangeEnt.setUserLoginMethod("");
        userPropertyChangeEnt.setUserWinLoseStreak(0);
        userPropertyChangeEnt.setUserPlatform("");
        userPropertyChangeEnt.setUserUtmCampaign("");
        userPropertyChangeEnt.setUserUtmSource("");
        userPropertyChangeEnt.setUserVersion("");
        // End
        userPropertyChangeEnt.setUserAppleLinkId(profile.getAppleID());
        userPropertyChangeEnt.setUserGgLinkId(profile.getGoogleId());
        userPropertyChangeEnt.setChangeItemName(changeItemName);
        userPropertyChangeEnt.setChangeItemAmount(quantity);
        userPropertyChangeEnt.setChangeItemId(refId);
        userPropertyChangeEnt.setChangeType(changeType);
        userPropertyChangeEnt.setBeforeChangeItemAmount(before);
        userPropertyChangeEnt.setAfterChangeItemAmount(after);
        return userPropertyChangeEnt;
    }

    @Override
    public String toJsonString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("user_app", ConvertUtils.toString(userApp));
        jsonObject.addProperty("user_country_code", ConvertUtils.toString(userCountryCode));
        jsonObject.addProperty("user_created_at_ms", userCreatedAtMs);
        jsonObject.addProperty("user_device", ConvertUtils.toString(userDevice));
        jsonObject.addProperty("user_display_name", ConvertUtils.toString(userDisplayName));
        jsonObject.addProperty("user_email", ConvertUtils.toString(userEmail));
        jsonObject.addProperty("user_emerald", userEmerald);
        jsonObject.addProperty("user_gold", userGold);
        jsonObject.addProperty("user_id", userId);
        jsonObject.addProperty("user_ip", ConvertUtils.toString(userIp));
        jsonObject.addProperty("user_is_new", userIsNew);
        jsonObject.addProperty("user_gg_link_id", ConvertUtils.toString(userGgLinkId));
        jsonObject.addProperty("user_apple_link_id", ConvertUtils.toString(userAppleLinkId));
        jsonObject.addProperty("user_login_method", ConvertUtils.toString(userLoginMethod));
        jsonObject.addProperty("user_win_lose_streak", userWinLoseStreak);
        jsonObject.addProperty("user_platform", ConvertUtils.toString(userPlatform));
        jsonObject.addProperty("user_trophy", userTrophy);
        jsonObject.addProperty("user_utm_campaign", ConvertUtils.toString(userUtmCampaign));
        jsonObject.addProperty("user_utm_source", ConvertUtils.toString(userUtmSource));
        jsonObject.addProperty("user_version", ConvertUtils.toString(userVersion));
        jsonObject.addProperty("change_item_id", changeItemId);
        jsonObject.addProperty("change_item_name", ConvertUtils.toString(changeItemName));
        jsonObject.addProperty("change_item_amount", changeItemAmount);
        jsonObject.addProperty("action_at_ms", actionAtMs);
        jsonObject.addProperty("before_change_item_amount", beforeChangeItemAmount);
        jsonObject.addProperty("after_change_item_amount", afterChangeItemAmount);
        jsonObject.addProperty("change_type", ConvertUtils.toString(changeType));
        jsonObject.addProperty("server_id", ModuleConfig.SERVER_ID);
        return jsonObject.toString();
    }
}
