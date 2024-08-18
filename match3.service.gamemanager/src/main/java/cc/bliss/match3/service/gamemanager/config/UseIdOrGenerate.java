package cc.bliss.match3.service.gamemanager.config;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;

import java.io.Serializable;

public class UseIdOrGenerate extends IdentityGenerator {
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) {
        if (obj == null) throw new HibernateException(new NullPointerException()) ;

        if ((((Profile) obj).getId()) == 0) {
            Serializable id = super.generate(session, obj) ;
            return id;
        } else {
            return ((Profile) obj).getId();

        }
    }
}
