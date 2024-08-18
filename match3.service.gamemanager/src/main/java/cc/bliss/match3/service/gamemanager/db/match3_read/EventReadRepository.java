/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Phong
 */
@Repository
public interface EventReadRepository extends ReadOnlyRepository<EventEnt, Integer> {

    List<EventEnt> findByType(int type);
}
