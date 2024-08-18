package cc.bliss.match3.service.gamemanager.ent.data;

import bliss.lib.framework.util.ConvertUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.ent.common.Statistic;
import cc.bliss.match3.service.gamemanager.ent.common.TicketEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.EBotType;
import cc.bliss.match3.service.gamemanager.ent.enums.EHeroFaction;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class MatchingTicketEnt implements IDataEnt{

    private long ticketCreatedAtMs;
    private long ticketEndedAtMs;
    private String ticketType;
    private long matchId;
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
    private String userAppleLinkId;
    private String userGgLinkId;
    private String userLoginMethod;
    private int userWinLoseStreak;
    private String userPlatform;
    private int userTrophy;
    private String userUtmCampaign;
    private String userUtmSource;
    private String userVersion;
    private String botHardMode;
    private JsonObject ticketsComparing;
    private int heroId;
    private String heroName;
    private int heroLevel;
    private int heroSkillLevel;
    private int heroTrophy;
    private String heroFaction;
    private int heroWinRate;
    private int serverId;

    public static MatchingTicketEnt buildCancelTicket(Profile profile, TicketEnt ticketEnt, HeroEnt heroSelect, Statistic statistic){
        MatchingTicketEnt matchingTicketEnt = new MatchingTicketEnt();
        matchingTicketEnt.setTicketCreatedAtMs(ticketEnt.getInitTime());
        matchingTicketEnt.setTicketEndedAtMs(System.currentTimeMillis());
        matchingTicketEnt.setTicketType("CANCELED");
        matchingTicketEnt.setMatchId(0);
        matchingTicketEnt.setUserApp(profile.getPackageID());
        matchingTicketEnt.setUserCreatedAtMs(profile.getDateCreated().getTime());
        matchingTicketEnt.setUserDevice(profile.getDeviceID());
        matchingTicketEnt.setUserDisplayName(profile.getDisplayName());
        matchingTicketEnt.setUserEmail(profile.getGmail());
        matchingTicketEnt.setUserEmerald(profile.getEmerald());
        matchingTicketEnt.setUserGold(profile.getMoney());
        matchingTicketEnt.setUserId(profile.getId());
        // Not implement yet
        matchingTicketEnt.setUserIp("");
        matchingTicketEnt.setUserIsNew(0);
        matchingTicketEnt.setUserCountryCode("");
        matchingTicketEnt.setUserLoginMethod("");
        matchingTicketEnt.setUserWinLoseStreak(0);
        matchingTicketEnt.setUserPlatform("");
        matchingTicketEnt.setUserUtmCampaign("");
        matchingTicketEnt.setUserUtmSource("");
        matchingTicketEnt.setUserVersion("");
        // End
        matchingTicketEnt.setUserAppleLinkId(profile.getAppleID());
        matchingTicketEnt.setUserGgLinkId(profile.getGoogleId());
        matchingTicketEnt.setUserTrophy(profile.getTrophy());
        matchingTicketEnt.setTicketsComparing(new JsonObject());
        matchingTicketEnt.setBotHardMode("");
        matchingTicketEnt.setHeroId(heroSelect.getId());
        matchingTicketEnt.setHeroName(heroSelect.getTitle());
        matchingTicketEnt.setHeroLevel(heroSelect.getLevel());
        matchingTicketEnt.setHeroSkillLevel(heroSelect.getSkillLevel());
        matchingTicketEnt.setHeroTrophy(heroSelect.getTrophy());

        EHeroFaction eHeroFaction = EHeroFaction.fromValue(heroSelect.getFaction());
        matchingTicketEnt.setHeroFaction(eHeroFaction!=null ? eHeroFaction.toString() : "");
        if (statistic.getTotalHand() == 0 ){
            matchingTicketEnt.setHeroWinRate(-1);
        } else {
            matchingTicketEnt.setHeroWinRate(ConvertUtils.toInt(statistic.getWinHand() * 100 / statistic.getTotalHand()));
        }
        return matchingTicketEnt;
    }

    public static JsonObject buildTicketComparing(TicketEnt cur, TicketEnt opponent){
        JsonArray comparingTicket = new JsonArray();
        for (Map.Entry<Long, Long> longLongEntry : cur.getComparingTicket().entrySet()) {
            Long key = longLongEntry.getKey();
            Long value = longLongEntry.getValue();
            JsonObject comparingTicketObject = new JsonObject();
            comparingTicketObject.addProperty("user_id", key);
            comparingTicketObject.addProperty("ticket_created_at_ms", value);
            comparingTicket.add(comparingTicketObject);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("comparing_tickets", comparingTicket);
        jsonObject.addProperty("bot_id",cur.getBotID());

        JsonObject opponentTicket = new JsonObject();
        if (cur.getBotID() == -1 && opponent != null){
            opponentTicket.addProperty("user_id", opponent.getBotID());
            opponentTicket.addProperty("ticket_created_at_ms", opponent.getInitTime());
        } else {
            opponentTicket.addProperty("user_id", -1);
            opponentTicket.addProperty("ticket_created_at_ms", 0);
        }
        jsonObject.add("matched_ticket", opponentTicket);
        return jsonObject;
    }

    public static MatchingTicketEnt buildMatchedTicket(Profile profile, TicketEnt ticketEnt, HeroEnt heroSelect, Statistic statistic, int roomID, int botType, TicketEnt opponent){
        MatchingTicketEnt matchingTicketEnt = new MatchingTicketEnt();
        matchingTicketEnt.setTicketCreatedAtMs(ticketEnt.getInitTime());
        matchingTicketEnt.setTicketEndedAtMs(System.currentTimeMillis());
        matchingTicketEnt.setTicketType("MATCHED");
        matchingTicketEnt.setMatchId(roomID);
        matchingTicketEnt.setUserApp(profile.getPackageID());
        matchingTicketEnt.setUserCreatedAtMs(profile.getDateCreated().getTime());
        matchingTicketEnt.setUserDevice(profile.getDeviceID());
        matchingTicketEnt.setUserDisplayName(profile.getDisplayName());
        matchingTicketEnt.setUserEmail(profile.getGmail());
        matchingTicketEnt.setUserEmerald(profile.getEmerald());
        matchingTicketEnt.setUserGold(profile.getMoney());
        matchingTicketEnt.setUserId(profile.getId());
        // Not implement yet
        matchingTicketEnt.setUserIp("");
        matchingTicketEnt.setUserIsNew(0);
        matchingTicketEnt.setUserCountryCode("");
        matchingTicketEnt.setUserLoginMethod("");
        matchingTicketEnt.setUserWinLoseStreak(0);
        matchingTicketEnt.setUserPlatform("");
        matchingTicketEnt.setUserUtmCampaign("");
        matchingTicketEnt.setUserUtmSource("");
        matchingTicketEnt.setUserVersion("");
        // End
        matchingTicketEnt.setUserAppleLinkId(profile.getAppleID());
        matchingTicketEnt.setUserGgLinkId(profile.getGoogleId());
        matchingTicketEnt.setUserTrophy(profile.getTrophy());
        matchingTicketEnt.setTicketsComparing(new JsonObject());
        EBotType eBotType = EBotType.fromValue(botType);
        matchingTicketEnt.setBotHardMode(eBotType != null ? eBotType.name() : "");
        matchingTicketEnt.setHeroId(heroSelect.getId());
        matchingTicketEnt.setHeroName(heroSelect.getTitle());
        matchingTicketEnt.setHeroLevel(heroSelect.getLevel());
        matchingTicketEnt.setHeroSkillLevel(heroSelect.getSkillLevel());
        matchingTicketEnt.setHeroTrophy(heroSelect.getTrophy());

        EHeroFaction eHeroFaction = EHeroFaction.fromValue(heroSelect.getFaction());
        matchingTicketEnt.setHeroFaction(eHeroFaction!=null ? eHeroFaction.toString() : "");
        if (statistic.getTotalHand() == 0 ){
            matchingTicketEnt.setHeroWinRate(-1);
        } else {
            matchingTicketEnt.setHeroWinRate(ConvertUtils.toInt(statistic.getWinHand() * 100 / statistic.getTotalHand()));
        }

        JsonObject jsonObject = buildTicketComparing(ticketEnt, opponent);
        matchingTicketEnt.setTicketsComparing(jsonObject);
        return matchingTicketEnt;
    }

    @Override
    public String toJsonString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ticket_created_at_ms", ticketCreatedAtMs);
        jsonObject.addProperty("ticket_ended_at_ms", ticketEndedAtMs);
        jsonObject.addProperty("ticket_type", ticketType);
        jsonObject.addProperty("match_id", matchId);
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
        jsonObject.addProperty("user_gg_link_id", userGgLinkId);
        jsonObject.addProperty("user_apple_link_id", userAppleLinkId);
        jsonObject.addProperty("user_login_method", userLoginMethod);
        jsonObject.addProperty("user_win_lose_streak", userWinLoseStreak);
        jsonObject.addProperty("user_platform", userPlatform);
        jsonObject.addProperty("user_trophy", userTrophy);
        jsonObject.addProperty("user_utm_campaign", userUtmCampaign);
        jsonObject.addProperty("user_utm_source", userUtmSource);
        jsonObject.addProperty("user_version", userVersion);
        jsonObject.addProperty("bot_hard_mode", botHardMode);
        jsonObject.add("tickets_comparing", ticketsComparing);
        jsonObject.addProperty("hero_id", heroId);
        jsonObject.addProperty("hero_name", heroName);
        jsonObject.addProperty("hero_level", heroLevel);
        jsonObject.addProperty("hero_skill_level", heroSkillLevel);
        jsonObject.addProperty("hero_trophy", heroTrophy);
        jsonObject.addProperty("hero_faction", heroFaction);
        jsonObject.addProperty("hero_win_rate", heroWinRate);
        jsonObject.addProperty("server_id", ModuleConfig.SERVER_ID);
        return jsonObject.toString();
    }
}
