package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
@Embeddable
@Builder
public class DeviceMappingID implements Serializable {

    private String deviceID;
    private String socialID;
}
