/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.FriendEnt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Phong
 */
@Repository
public interface FriendReadRepository extends ReadOnlyRepository<FriendEnt, Integer> {

    @Query(value = "select * from friends where user_id = :userID and friend_status = 0", nativeQuery = true)
    public List<FriendEnt> findListFriend(@Param("userID") long userID);

    @Query(value = "select * from friends where friend_id = :userID and friend_status = 1", nativeQuery = true)
    public List<FriendEnt> findListFriendRequest(@Param("userID") long userID);

    @Query(value = "select * from friends where user_id = :userID and friend_id = :friendID and friend_status = :status", nativeQuery = true)
    public FriendEnt findByUserIDAndFriendID(@Param("userID") long userID, @Param("friendID") long friendID, @Param("status") int status);

    @Query(value = "select count(*) from friends where user_id = :userID and friend_status = 0", nativeQuery = true)
    public int countListFriend(@Param("userID") long userID);

    @Query(value = "select count(*) from friends where friend_id = :userID and friend_status = 1", nativeQuery = true)
    public int countListFriendRequest(@Param("userID") long userID);
}
