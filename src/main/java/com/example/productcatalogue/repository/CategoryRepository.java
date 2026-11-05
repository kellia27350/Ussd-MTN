package com.example.productcatalogue.repository;

import com.example.productcatalogue.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);

    List<Category> findAllByOrderByNameAsc();

    List<Category> findDistinctByProductsActiveTrueOrderByNameAsc();
}
