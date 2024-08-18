package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "version")
@Data
@DynamicUpdate
public class Version {

    @Id
    private String version;

    private String gameServerNameSpace = StringUtils.EMPTY;

    private int os = 0;

    private int forceUpdate = 0;

    private String forceUpdateUrl = StringUtils.EMPTY;

    private String forceUpdateVersion = StringUtils.EMPTY;
}
