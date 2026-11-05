package com.example.productcatalogue.repository;

import com.example.productcatalogue.model.UssdDialLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UssdDialLogRepository extends JpaRepository<UssdDialLog, Long> {

    List<UssdDialLog> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    List<UssdDialLog> findAllByOrderByCreatedAtDesc();

    @Query("select d.phoneNumber, count(d), min(d.createdAt), max(d.createdAt) "
            + "from UssdDialLog d group by d.phoneNumber order by max(d.createdAt) desc")
    List<Object[]> summarizeCallers();
}
