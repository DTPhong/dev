/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ProfileStatistic;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Phong
 */
@Repository
public interface ProfileStatisticWriteRepository extends WriteRepository<ProfileStatistic, Long> {

    @Modifying
    @Transactional
    @Query(value = "update profile_statistic set data = :data where user_id = :profileId", nativeQuery = true)
    void updateData(@Param("profileId") long profileId, @Param("data") String data);

    @Modifying
    @Transactional
    @Query(value = "update profile_statistic set is_joined_7d_event = :isJoined where user_id = :profileId", nativeQuery = true)
    void updateIsJoined7dEvent(@Param("profileId") long profileId, @Param("isJoined") int isJoined);
}
