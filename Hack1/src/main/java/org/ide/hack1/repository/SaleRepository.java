package org.ide.hack1.repository;

    import org.ide.hack1.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;


@Repository
public interface SaleRepository extends JpaRepository<Sale, String> {

    List<Sale> findBySoldAtBetween(Instant from, Instant to);

    Page<Sale> findBySoldAtBetweenAndBranch(Instant from, Instant to, String branch, Pageable pageable);

    Page<Sale> findByBranch(String branch, Pageable pageable);

}
