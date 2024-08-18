/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Phong
 */
@Entity
@Table(name = "friends", indexes = {
        @Index(name = "fr_user_index", columnList = "user_id"),
        @Index(name = "fr_friend_index", columnList = "friend_id")
})
@Data
public class FriendEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "user_id")
    private long userID;
    @Column(name = "friend_id")
    private long friendID;
    private int friendStatus;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime;
}
