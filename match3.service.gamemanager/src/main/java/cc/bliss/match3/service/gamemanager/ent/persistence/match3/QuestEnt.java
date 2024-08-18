/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.ent.enums.EQuestType;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

/**
 * @author Phong
 */
@Entity
@Table(name = "quest_pool")
@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamicUpdate
public class QuestEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String customData;
    private int tier;

    public int getHeroId() {
        JsonObject jsonObject = JSONUtil.DeSerialize(customData, JsonObject.class);
        return jsonObject != null && jsonObject.has("heroID")
                ? jsonObject.get("heroID").getAsInt()
                : 0;
    }

    public boolean isUpgradeHero() {
        JsonObject jsonObject = JSONUtil.DeSerialize(customData, JsonObject.class);
        int questType = jsonObject != null && jsonObject.has("questType")
                ? jsonObject.get("questType").getAsInt()
                : 0;
        return EQuestType.findByValue(questType).equals(EQuestType.UPGRADE_HERO);
    }
}
