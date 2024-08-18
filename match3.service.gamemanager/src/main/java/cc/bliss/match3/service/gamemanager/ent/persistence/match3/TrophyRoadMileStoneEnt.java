/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.ent.common.RewardEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Phong
 */

@Entity
@Table(name = "trophy_road")
@Data
@DynamicUpdate
public class TrophyRoadMileStoneEnt {

    @Id
    private int id;
    private int milestone;

    @Column(columnDefinition = "varchar(255) default '[]'")
    private String rewards;

    /**
     * Please read cc.bliss.match3.service.gamemanager.ent.enums.ETrophyRoadMileStoneType
     */
    private int type;

    @JsonIgnore
    public List<RewardEnt> getRewards() {
        List<RewardEnt> rewardEnts = new ArrayList<>();
        JsonArray jsonArray = JSONUtil.DeSerialize(rewards, JsonArray.class);
        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            RewardEnt rewardEnt = new RewardEnt();
            rewardEnt.setERewardType(ERewardType.findByValue(jsonObject.get("rewardType").getAsInt()));
            rewardEnt.setDelta(jsonObject.get("delta").getAsInt());
            if (jsonObject.has("ref")) {
                rewardEnt.setRef(jsonObject.get("ref").getAsInt());
            }
            rewardEnts.add(rewardEnt);
        }
        return rewardEnts;
    }
}
