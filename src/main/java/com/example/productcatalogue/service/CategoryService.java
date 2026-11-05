package com.example.productcatalogue.service;

import com.example.productcatalogue.dto.CategoryRequest;
import com.example.productcatalogue.exception.ResourceNotFoundException;
import com.example.productcatalogue.model.Category;
import com.example.productcatalogue.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    public List<Category> getAll() {
        return repo.findAllByOrderByNameAsc();
    }

    public Category getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    public Category create(CategoryRequest request) {
        if (repo.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Category '" + request.getName() + "' already exists");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return repo.save(category);
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = getById(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return repo.save(category);
    }

    public void delete(Long id) {
        repo.delete(getById(id));
    }
}
