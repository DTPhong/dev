/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ConfigEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Version;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Phong
 */
@Repository
public interface ConfigReadRepository extends ReadOnlyRepository<ConfigEnt, String> {

    ConfigEnt getById(String id);

    Optional<ConfigEnt> findById(String id);

}
