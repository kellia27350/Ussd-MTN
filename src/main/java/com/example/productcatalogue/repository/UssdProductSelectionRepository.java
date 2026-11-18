package com.example.productcatalogue.repository;

import com.example.productcatalogue.model.UssdProductSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UssdProductSelectionRepository extends JpaRepository<UssdProductSelection, Long> {

    List<UssdProductSelection> findAllByOrderByCreatedAtDesc();

    List<UssdProductSelection> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    List<UssdProductSelection> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /** Most-viewed products (final detail screen only): productId, productName, views. */
    @Query("select s.productId, s.productName, count(s) from UssdProductSelection s "
            + "where s.selectionType = 'DETAIL_VIEWED' "
            + "group by s.productId, s.productName order by count(s) desc")
    List<Object[]> popularProducts();
}
