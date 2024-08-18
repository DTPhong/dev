package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "device_mapping")
public class DeviceMappingEnt {

    @EmbeddedId
    private DeviceMappingID deviceMappingID;
    private long userID;
}
