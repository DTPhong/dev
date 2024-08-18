/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.MailEnt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author Phong
 */
@Repository
public interface MailWriteRepository extends WriteRepository<MailEnt, Integer> {

    @Modifying
    @Transactional
    @Query(value = "update mail set `status` = :value where id = :mailID", nativeQuery = true)
    void updateStatusById(@Param("mailID") int mailID, @Param("value") int status);

    @Modifying
    @Transactional
    @Query(value = "update mail set `status` = :value where id in (:mailID)", nativeQuery = true)
    void updateStatusByIdIn(@Param("mailID") List<Integer> mailID, @Param("value") int status);

    @Query(value = "DELETE FROM mail where user_id = :userId", nativeQuery = true)
    public void deleteMailByUserID(@Param(value = "userId") long userId);
}
