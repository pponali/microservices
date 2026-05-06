package com.services.product.repository;

import com.services.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

@org.springframework.stereotype.Repository("productRepository")
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface ProductRepository extends MongoRepository<Product, String> {
}
