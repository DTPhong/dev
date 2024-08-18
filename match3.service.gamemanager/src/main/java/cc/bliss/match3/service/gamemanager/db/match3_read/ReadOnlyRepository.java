package cc.bliss.match3.service.gamemanager.db.match3_read;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ReadOnlyRepository<T, ID> extends Repository<T, ID>, JpaSpecificationExecutor<T> {
    Optional<T> findById(ID id);
    List<T> findAll();
    List<T> findAllById(Iterable<ID> var1);
    List<T> findAll(@Nullable Specification<T> var1);
    List<T> findAll(Sort var1);
    T getById(ID var1);
    Page<T> findAll(Pageable var1);
    boolean existsById(ID var1);
}
