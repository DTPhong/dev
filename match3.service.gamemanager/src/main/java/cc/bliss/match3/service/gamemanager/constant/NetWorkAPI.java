/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.constant;

/**
 * @author Phong
 */
public enum NetWorkAPI {

    UNKNOWN(-1),
    LOGIN(0),
    PROFILE_BY_ID(10),
    PROFILE_SEARCH(11),
    PROFILE_EDIT(12),
    UPDATE_MONEY(13),
    BATTLE_LOG_POST(20),
    BATTLE_LOG_SEARCH(21),
    BATTLE_LOG_BY_ID(22),
    HERO_LIST_BY_USER(30),
    HERO_UPGRADE(31),
    CARD_FUSION(32),
    CARD_CONVERT(33),
    HERO_SELECT(34),
    HERO_SKILL_UPGRADE(35),
    LEADERBOARD_LIST(40),
    FRIEND_LIST(41),
    FRIEND_REQUEST_LIST(42),
    SEND_FRIEND_REQUEST(43),
    FRIEND_LEADERBOARD_LIST(44),
    MAIL_BY_USER(50),
    MAIL_BY_ID(51),
    CREATE_TICKET(60),
    PULL_TICKET(61),
    DEL_TICKET(62),
    GET_LIST_TICKET(63),
    MAINTAIN(64),
    DEL_TICKET_BY_ROOM(65),
    GET_CONFIG(70),
    UPDATE_CONFIG(71),
    PULL_REWARD(80),
    OPEN_REWARD(81),
    CHEST_INFO(82),
    CLAN_GET_LIST(90),
    CLAN_JOIN(91),
    CLAN_INFO(92),
    CLAN_DETAIL(93),
    CLAN_SEND_CHAT(94),
    CLAN_CHAT_STREAM(95),
    CLAN_CREATE(96),
    CLAN_CHAT_HISTORY(97),
    SSE_TOAST(100),
    GET_REDDOT(110),
    UPDATE_REDDOT(111),
    GET_SHOP(120),
    REFRESH_SHOP(121),
    BUY_PACK(122),
    WATCH_AD(123),
    EXCHANGE_DIAMOND_TO_EMERALD(124),
    GET_LIST_GACHA(130),
    GACHA(131),
    EVENT(132),
    CLAIM_DAILY_EVENT(133),
    RUSH_MATCH_UPDATE(661),
    RUSH_MATCH_COMPLETED(662),
    RUSH_COMPLETED(663),
    RUSH_ROOM_UPDATE(664),
    WIN_BATTLE(665);

    private final int value;

    NetWorkAPI(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
