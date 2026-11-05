package com.example.productcatalogue.config;

import com.example.productcatalogue.model.Category;
import com.example.productcatalogue.model.Product;
import com.example.productcatalogue.repository.CategoryRepository;
import com.example.productcatalogue.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    public DataSeeder(CategoryRepository categoryRepo, ProductRepository productRepo) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) return;
        if (categoryRepo.count() > 0) {
            restoreIrekureCategories();
            return;
        }

        seedOne("Gumamo", "Serivisi yo kwibutsa/guma ku murongo",
                "Gumamo", "Serivisi yo kwibutsa/guma ku murongo", 1, "1.00");

        Category irekure = categoryRepo.save(
                new Category(null, "MTN Irekure", "MTN Irekure bundles (24hrs / weekly / monthly)", null));

        productRepo.save(product("MTN Irekure(24hrs)", "Irekure bundle valid for 24 hours", 1, "100.00", irekure));
        productRepo.save(product("MTN Irekure Icyumweru", "Irekure bundle valid for 1 week", 7, "500.00", irekure));
        productRepo.save(product("MTN Irekure Ukwezi", "Irekure bundle valid for 1 month", 30, "1500.00", irekure));

        seedOne("Gwamon'", "Iyi serivisi iracyakorwaho",
                "Gwamon'", "Iyi serivisi iracyakorwaho", 1, "1.00");

        seedOne("Ayo usigaranye", "Reba amafaranga asigaye kuri konti yawe",
                "Ayo usigaranye", "Reba amafaranga asigaye kuri konti yawe", 1, "1.00");
    }

    private void restoreIrekureCategories() {
        Category grouped = categoryRepo.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase("MTN Irekure"))
                .findFirst().orElse(null);

        Category daily = grouped == null
                ? findOrCreate("MTN Irekure(24hrs)", "MTN Irekure bundles valid for 24 hours")
                : rename(grouped, "MTN Irekure(24hrs)", "MTN Irekure bundles valid for 24 hours");
        Category weekly = findOrCreate("MTN Irekure Icyumweru", "MTN Irekure bundles valid for 1 week");
        Category monthly = findOrCreate("MTN Irekure Ukwezi", "MTN Irekure bundles valid for 1 month");

        for (Product p : productRepo.findAll()) {
            String name = p.getName() == null ? "" : p.getName().toLowerCase();
            if (!name.contains("irekure")
                    && (p.getCategory() == null || !"mtn irekure".equalsIgnoreCase(p.getCategory().getName()))) {
                continue;
            }
            if (name.contains("icyumweru") || (p.getValidityDays() != null && p.getValidityDays() == 7)) {
                p.setCategory(weekly);
            } else if (name.contains("ukwezi") || (p.getValidityDays() != null && p.getValidityDays() >= 30)) {
                p.setCategory(monthly);
            } else {
                p.setCategory(daily);
            }
            productRepo.save(p);
        }
    }

    private Category findOrCreate(String name, String description) {
        return categoryRepo.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> categoryRepo.save(new Category(null, name, description, null)));
    }

    private Category rename(Category category, String name, String description) {
        category.setName(name);
        category.setDescription(description);
        return categoryRepo.save(category);
    }

    private void seedOne(String catName, String catDesc, String prodName, String prodDesc,
                         int validityDays, String price) {
        Category category = categoryRepo.save(new Category(null, catName, catDesc, null));
        productRepo.save(product(prodName, prodDesc, validityDays, price, category));
    }

    private Product product(String name, String description, int validityDays, String price, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setValidityDays(validityDays);
        p.setPrice(new BigDecimal(price));
        p.setCategory(category);
        p.setActive(true);
        return p;
    }
}
