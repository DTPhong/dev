/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author baotn
 */
@Repository
public interface TriggerReadRepository extends ReadOnlyRepository<TriggerEnt, Integer> {

    @Query(value = "SELECT * FROM `trigger` where start_time <= :now and end_time >= :now and status = :status", nativeQuery = true)
    public List<TriggerEnt> getCurrentListTrigger(@Param(value = "now") Timestamp now, @Param(value = "status") int status);

    @Query(value = "SELECT * FROM `trigger` where status = :status", nativeQuery = true)
    public List<TriggerEnt> getCurrentListTrigger(@Param(value = "status") int status);

    @Query(value = "SELECT * FROM `trigger` where type = :type and ref_id = :refId limit 1", nativeQuery = true)
    public TriggerEnt getTrigger(@Param(value = "type") int type, @Param(value = "refId") int refId);

    @Query(value = "SELECT * FROM `trigger` where start_time <= :now and end_time >= :now and id = :id", nativeQuery = true)
    public TriggerEnt getTriggerById(@Param(value = "now") Timestamp now, @Param(value = "id") int id);

    @Query(value = "SELECT * FROM `trigger` where type = :type and start_time <= :now and end_time >= :now and status = :status", nativeQuery = true)
    public List<TriggerEnt> getCurrentListTrigger(@Param(value = "type") int type, @Param(value = "now") Timestamp now, @Param(value = "status") int status);

    @Query(value = "SELECT * FROM `trigger` where type = :type and start_time <= :now and end_time >= :now and status = :status order by created_time desc limit 1", nativeQuery = true)
    public TriggerEnt getCurrentSingleTrigger(@Param(value = "type") int type, @Param(value = "now") Timestamp now, @Param(value = "status") int status);
}
