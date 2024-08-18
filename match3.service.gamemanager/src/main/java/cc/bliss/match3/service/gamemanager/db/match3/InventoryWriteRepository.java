/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.InventoryEnt;
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
public interface InventoryWriteRepository extends WriteRepository<InventoryEnt, Long> {

    @Modifying
    @Transactional
    @Query(value = "update inventory set hero_arr = :hero_arr, shard_arr = :shard_arr where id = :profileId",
            nativeQuery = true)
    void updateHeroAndShard(@Param("profileId") long profileId, @Param("hero_arr") String hero_arr, @Param("shard_arr") String shard_arr);

    @Modifying
    @Transactional
    @Query(value = "update inventory set hero_arr = :hero_arr where id = :profileId",
            nativeQuery = true)
    void updateHero(@Param("profileId") long profileId, @Param("hero_arr") String hero_arr);
}
