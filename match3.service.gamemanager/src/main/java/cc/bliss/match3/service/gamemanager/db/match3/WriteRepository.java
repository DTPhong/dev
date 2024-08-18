package cc.bliss.match3.service.gamemanager.db.match3;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;

@NoRepositoryBean
public interface WriteRepository<T, ID> extends Repository<T, ID> {

    void deleteAllInBatch(Iterable<T> var1);

    void deleteAllByIdInBatch(Iterable<ID> var1);

    void deleteAllInBatch();

    void deleteById(ID var1);

    void delete(T var1);

    void deleteAllById(Iterable<? extends ID> var1);

    void deleteAll(Iterable<? extends T> var1);

    void deleteAll();
}
