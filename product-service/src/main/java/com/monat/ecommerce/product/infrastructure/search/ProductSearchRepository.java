package com.monat.ecommerce.product.infrastructure.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for product index sync (save/delete).
 * Actual search queries go through ProductMongoRepository (MongoDB).
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {
}
