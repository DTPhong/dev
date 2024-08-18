/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ConfigEnt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Phong
 */
@Repository
public interface ConfigWriteRepository extends WriteRepository<ConfigEnt, String>,JpaRepository<ConfigEnt, String> {

    @Modifying
    @Transactional
    @Query(value = "update configs set `value` = :value where id = :id", nativeQuery = true)
    void updateValueById(@Param("id") String id,@Param("value")  String value);
}
