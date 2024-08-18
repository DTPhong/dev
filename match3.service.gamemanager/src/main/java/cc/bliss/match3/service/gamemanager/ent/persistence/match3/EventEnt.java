/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Phong
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@Table(name = "event")
public class EventEnt implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(name = "title")
    public String title = "";

    @Column(name = "priority")
    public int priority;

    @Column(name = "status")
    public int status;

    @Column(name = "custom_data")
    public String customData = "";

    @Column(name = "thumbnail")
    public String thumbnail = "";

    @Column(name = "type")
    public int type;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime;
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedTime;
}
