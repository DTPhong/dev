/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ClanMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Phong
 */
@Repository
public interface ClanMemberReadRepository extends ReadOnlyRepository<ClanMember, Integer> {

    public Optional<ClanMember> findByUserID(long userID);

    public Optional<ClanMember> findByUserIDAndClanID(long userID, int clanID);

    public List<ClanMember> findByClanID(int clanID);

    public List<ClanMember> findByClanIDAndState(int clanID, int state);

    public List<ClanMember> findByClanIDAndStateIn(int clanID, List<Integer> listState);

    public int countByClanIDAndStateIn(int clanID, List<Integer> listState);

    public void deleteByClanID(int clanID);
}
