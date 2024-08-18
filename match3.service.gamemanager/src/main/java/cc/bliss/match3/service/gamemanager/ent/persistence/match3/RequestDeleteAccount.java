package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Data
@DynamicUpdate
public class RequestDeleteAccount {

    @Id
    long userId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date requestTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deleteTime;
}
