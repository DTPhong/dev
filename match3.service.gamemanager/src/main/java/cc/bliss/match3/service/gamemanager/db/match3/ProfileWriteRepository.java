/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * @author Phong
 */
@Repository
public interface ProfileWriteRepository extends WriteRepository<Profile, Long> {

    @Modifying
    @Transactional
    @Query(value = "update users set money = money + :delta where id = :profileId", nativeQuery = true)
    void updateGoldByProfileId(@Param("profileId") long profileId,@Param("delta") long deltaGold);

    @Modifying
    @Transactional
    @Query(value = "update users set emerald = emerald + :delta where id = :profileId", nativeQuery = true)
    void updateEmeraldByProfileId(@Param("profileId") long profileId,@Param("delta") long deltaEmerald);

    @Modifying
    @Transactional
    @Query(value = "update users set amethyst = amethyst + :delta where id = :profileId", nativeQuery = true)
    void updateAmethystByProfileId(@Param("profileId") long profileId,@Param("delta") long deltaEmerald);

    @Modifying
    @Transactional
    @Query(value = "update users set royal_amethyst = royal_amethyst + :delta where id = :profileId", nativeQuery = true)
    void updateRoyalAmethystByProfileId(@Param("profileId") long profileId,@Param("delta") long deltaEmerald);

    @Modifying
    @Transactional
    @Query(value = "update users set tutorial = :value where id = :profileId", nativeQuery = true)
    void updateTutorialByProfileId(@Param("profileId") long profileId, @Param("value") long newValue);

    @Modifying
    @Transactional
    @Query(value = "update users set last_login = :lastLogin, deviceid = :deviceID, is_new = :isNew, version = :version where id = :profileId",
            nativeQuery = true)
    void updateLoginField(
            @Param("profileId") long profileId,
            @Param("lastLogin") Date lastLogin,
            @Param("deviceID") String deviceID,
            @Param("version") String version,
            @Param("isNew") int isNew);

    @Modifying
    @Transactional
    @Query(value = "update users set trophy_road_ticket_expired = :trophy_road_ticket_expired where id = :profileId",
            nativeQuery = true)
    void updateTrophyRoadTicketExpired(@Param("profileId") long profileId, @Param("trophy_road_ticket_expired") Date trophy_road_ticket_expired);

    @Modifying
    @Transactional
    @Query(value = "update users set last_login = last_login where id = :profileId",
            nativeQuery = true)
    void testWriteDB(@Param("profileId") long profileId);

    @Modifying
    @Transactional
    @Query(value = "update users set username = :username where id = :profileId", nativeQuery = true)
    void updateUsername(@Param("profileId") long profileId, @Param("username") String newValue);

    @Modifying
    @Transactional
    @Query(value = "update users set version = :version where id = :profileId", nativeQuery = true)
    void updateVersion(@Param("profileId") long profileId, @Param("version") String version);

    @Modifying
    @Transactional
    @Query(value = "update users set frame = :value where id = :profileId", nativeQuery = true)
    void updateFrame(@Param("profileId") long profileId, @Param("value") long newValue);

    @Modifying
    @Transactional
    @Query(value = "update users set avatar_path = :value where id = :profileId", nativeQuery = true)
    void updateAvatarPath(@Param("profileId") long profileId, @Param("value") String newValue);

    @Modifying
    @Transactional
    @Query(value = "update users " +
            "set dominate_win = :dominate_win,  battle_won = :battle_won, lose_streak = :lose_streak, win_streak = :win_streak, god_like_win = :god_like_win, highest_trophy = :highest_trophy, highest_streak = :highest_streak " +
            "where id = :profileId", nativeQuery = true)
    void updateEndGame(@Param("profileId") long profileId,
                       @Param("dominate_win") int dominate_win,
                       @Param("battle_won") int battle_won,
                       @Param("lose_streak") int lose_streak,
                       @Param("win_streak") int win_streak,
                       @Param("god_like_win") int god_like_win,
                       @Param("highest_trophy") int highest_trophy,
                       @Param("highest_streak") int highest_streak);
    @Modifying
    @Transactional
    @Query(value = "update users " +
            "set highest_trophy = :highest_trophy " +
            "where id = :profileId", nativeQuery = true)
    void updateHighestTrophy(@Param("profileId") long profileId,
                       @Param("highest_trophy") int highest_trophy);
    @Modifying
    @Transactional
    @Query(value = "update users " +
            "set select_hero = :select_hero " +
            "where id = :profileId", nativeQuery = true)
    void updateSelectHero(@Param("profileId") long profileId,
                             @Param("select_hero") int select_hero);

    @Modifying
    @Transactional
    @Query(value = "update users " +
            "set google_id = :google_id, google_name = :google_name, google_avatar = :google_avatar, gmail = :gmail, avatar_path = :avatar_path " +
            "where id = :profileId", nativeQuery = true)
    void updateGoogleLink(@Param("profileId") long profileId,
                          @Param("google_id") String google_id,
                          @Param("google_name") String google_name,
                          @Param("google_avatar") String google_avatar,
                          @Param("gmail") String gmail,
                          @Param("avatar_path") String avatar_path);

    @Modifying
    @Transactional
    @Query(value = "update users " +
            "set appleid = :appleid, apple_name = :apple_name, apple_avatar = :apple_avatar, apple_email = :apple_email, avatar_path = :avatar_path " +
            "where id = :profileId", nativeQuery = true)
    void updateAppleLink(@Param("profileId") long profileId,
                          @Param("appleid") String appleid,
                          @Param("apple_name") String apple_name,
                          @Param("apple_avatar") String apple_avatar,
                          @Param("apple_email") String apple_email,
                          @Param("avatar_path") String avatar_path);

    @Modifying
    @Transactional
    @Query(value = "update users " +
            "set tutorial = 1, money = 0, emerald = 0, amethyst = 0, royal_amethyst = 0, win_streak = 0, battle_won = 0, " +
            "select_hero = 1, dominate_win = 0, god_like_win = 0, highest_trophy = 0, highest_streak = 0 " +
            "where id = :profileId", nativeQuery = true)
    void updateDeleteAccount(@Param("profileId") long profileId);
}
