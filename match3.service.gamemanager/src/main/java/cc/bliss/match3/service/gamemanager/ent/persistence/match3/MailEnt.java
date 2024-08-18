/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Phong
 */
@Entity
@Table(name = "mail")
@Data
@DynamicUpdate
public class MailEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private long userId;
    private String title;
    private String content;

    @Temporal(TemporalType.TIMESTAMP)
    private Date receivedTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date expiredTime;

    private int status;
    private String rewards;
}
