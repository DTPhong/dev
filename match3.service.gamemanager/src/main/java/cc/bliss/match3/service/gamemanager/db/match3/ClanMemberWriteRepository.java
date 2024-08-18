/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ClanMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @author Phong
 */
@Repository
public interface ClanMemberWriteRepository extends WriteRepository<ClanMember, Long> {

    void deleteByClanID(int clanID);
    @Modifying
    @Transactional
    @Query(value = "update clan_member set clainid = :clainid, state = :state, request_msg = :request_msg, userid = :userid where id = :id", nativeQuery = true)
    void updateData(@Param("id") int id,
                    @Param("clainid") int clainid,
                    @Param("state") int state,
                    @Param("request_msg") String request_msg,
                    @Param("userid") long userid);
}
