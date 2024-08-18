/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Phong
 */
@Entity
@Table(name = "clan_member", indexes = {
        @Index(name = "idx_cm_userID", columnList = "userID"),
        @Index(name = "idx_cm_clanID", columnList = "clanID")
})
@Data
public class ClanMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private long userID;
    private int clanID;
    private int state;
    @Column(length = 1000)
    private String requestMsg;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedTime;
}
