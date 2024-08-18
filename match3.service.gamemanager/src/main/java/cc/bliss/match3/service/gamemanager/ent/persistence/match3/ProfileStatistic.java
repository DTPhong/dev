/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * @author Phong
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@DynamicUpdate
@Table(name = "profile_statistic")
public class ProfileStatistic {

    @Id
    @Column(name = "user_id")
    private long userID;

    @Column(name = "data")
    private String data;

    @Column(name = "is_joined_7d_event")
    private int isJoined7dEvent;
}
