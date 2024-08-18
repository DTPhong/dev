/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.MailEnt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author Phong
 */
@Repository
public interface MailReadRepository extends ReadOnlyRepository<MailEnt, Integer> {

    // rewards có bestfriend là mail card, ko xử lý trả về client trong game
    @Query(value = "SELECT * FROM mail where expired_time > :now and received_time <= :now and user_id = :userId and status != :statusDelete and (rewards not like '%bestfriend%' or rewards is null or rewards = '') order by received_time asc", nativeQuery = true)
    public List<MailEnt> getCurrentListMail(@Param(value = "userId") long userId,
                                            @Param(value = "statusDelete") int statusDelete,
                                            @Param(value = "now") Timestamp now);

    @Query(value = "SELECT count(1) FROM mail where expired_time > :now and received_time <= :now and user_id = :userId and status = :status and (rewards not like '%bestfriend%' or rewards is null or rewards = '')", nativeQuery = true)
    public Long getMailCountByStatus(@Param(value = "userId") long userId,
                                     @Param(value = "status") int statusDelete,
                                     @Param(value = "now") Timestamp now);

    @Query(value = "DELETE FROM mail where user_id = :userId", nativeQuery = true)
    public void deleteMailByUserID(@Param(value = "userId") long userId);
}
