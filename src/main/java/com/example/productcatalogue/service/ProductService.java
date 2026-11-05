package com.example.productcatalogue.service;

import com.example.productcatalogue.dto.ProductRequest;
import com.example.productcatalogue.exception.ResourceNotFoundException;
import com.example.productcatalogue.model.Category;
import com.example.productcatalogue.model.Product;
import com.example.productcatalogue.repository.CategoryRepository;
import com.example.productcatalogue.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    public List<Product> getAll() {
        return productRepo.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Product getById(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public List<Product> getByCategory(Long categoryId) {
        return productRepo.findByCategoryIdOrderByNameAsc(categoryId);
    }

    public List<Product> search(String name) {
        return productRepo.findByNameContainingIgnoreCaseOrderByNameAsc(name);
    }

    public List<Product> getActiveProducts() {
        return productRepo.findByActiveTrueOrderByNameAsc();
    }

    public Product create(ProductRequest request) {
        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        Product product = new Product();
        apply(product, request, category);
        return productRepo.save(product);
    }

    public Product update(Long id, ProductRequest request) {
        Product product = getById(id);
        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        apply(product, request, category);
        return productRepo.save(product);
    }

    public void delete(Long id) {
        productRepo.delete(getById(id));
    }

    private void apply(Product product, ProductRequest request, Category category) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setValidityDays(request.getValidityDays());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        product.setActive(request.getActive() == null || request.getActive());
    }
}
