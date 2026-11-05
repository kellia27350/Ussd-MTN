package com.example.productcatalogue.repository;

import com.example.productcatalogue.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<Product> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId);


    List<Product> findByCategoryIdAndParentIsNullOrderByIdAsc(Long categoryId);


    @Query("select p from Product p where p.parent.id = :parentId order by p.id asc")
    List<Product> findByParentIdOrderByIdAsc(@Param("parentId") Long parentId);

    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Product> findByActiveTrue();

    List<Product> findByActiveTrueOrderByNameAsc();
}