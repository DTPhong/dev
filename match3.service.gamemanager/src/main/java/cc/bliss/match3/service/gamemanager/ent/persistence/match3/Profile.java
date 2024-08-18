/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import bliss.lib.framework.util.JSONUtil;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Phong
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username")
        })
@Data
@DynamicUpdate
public class Profile {

    @Id
    @SequenceGenerator(name = "profile_seq", initialValue = 1000000, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "profile_seq")
    private long id;

    private String username;

    private String displayName;

    private String password;

    private String avatarPath;

    private String version;

    private long money;

    private String googleId;

    private String googleName;

    private String googleAvatar;

    private String gmail;

    private String appleName;

    private String appleID;

    private String appleEmail;

    private String appleAvatar;

    private String deviceID;

    private String packageID;

    private int selectHero;

    private int battleWon;

    private int dominateWin;

    private int godLikeWin;

    private int winStreak;

    private int loseStreak;

    private int highestTrophy;

    private int highestStreak;

    private long emerald;

    private long amethyst;

    private long royalAmethyst;

    private int frame;

    private float timeZone;

    private Date trophyRoadTicketExpired;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(columnDefinition = "DATETIME(6)")
    private Date lastLogin;

    @Lob
    private String listAvatar = "[]";

    private String listFrame = "[]";
    @Temporal(TemporalType.TIMESTAMP)
    @Column(columnDefinition = "DATETIME(6)")
    @CreationTimestamp
    private Date dateCreated;

    private int isNew;

    @Column(columnDefinition = "int default '0'")
    private int tutorial = 0;

    private int botType;

    @Transient
    private int totalHeroes;

    @Transient
    private int trophy;
    @Transient
    private int clanID = -1;
    @Transient
    private String avaId;
    @Transient
    private int selectHeroTrophy;

    public String getDisplayName(){
        return StringUtils.isEmpty(displayName) ? username : displayName;
    }

    public String getAvatarPath() {
        return StringUtils.isEmpty(avatarPath) ? "https://dlc.match3arena.com/profile_ava_01.png" : avatarPath;
    }

    public int getSelectHero() {
        return selectHero == 0 ? ModuleConfig.HERO_DEFAULT_ID : selectHero;
    }

    public void addAvatar(List<String> listAva) {
        JsonArray jsonArray = getListAvatarArr();
        for (String ava : listAva) {
            String avaId = UUID.randomUUID().toString();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("avaId", avaId);
            jsonObject.addProperty("avaPath", ava);
            jsonArray.add(jsonObject);
        }

        listAvatar = jsonArray.toString();
    }

    public void addFrame(List<Integer> frames) {
        JsonArray jsonArray = getListFrameArr();
        for (Integer frame : frames) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("frameId", frame);
            jsonArray.add(jsonObject);
        }

        listFrame = jsonArray.toString();
    }

    public JsonArray getListAvatarArr() {
        if (StringUtils.isNotEmpty(listAvatar)) {
            return JSONUtil.DeSerialize(listAvatar, JsonArray.class);
        }
        return new JsonArray();
    }

    public JsonArray getListFrameArr() {
        if (StringUtils.isNotEmpty(listFrame)) {
            return JSONUtil.DeSerialize(listFrame, JsonArray.class);
        }
        return new JsonArray();
    }

    public String getAvaPath(String avaId) {
        JsonArray jsonArray = getListAvatarArr();
        for (JsonElement item : jsonArray) {
            JsonObject ava = item.getAsJsonObject();
            if (ava.get("avaId").getAsString().equals(avaId)) {
                return ava.get("avaPath").toString();
            }
        }
        return "";
    }
}
