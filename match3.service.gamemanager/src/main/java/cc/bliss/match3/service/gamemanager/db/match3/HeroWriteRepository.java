/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.DeviceMappingEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author Phong
 */
@Repository
public interface HeroWriteRepository extends WriteRepository<HeroEnt, Integer> {

    HeroEnt saveAndFlush(HeroEnt var1);
}
