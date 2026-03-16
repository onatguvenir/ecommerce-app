package com.monat.ecommerce.product.infrastructure.search;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Elasticsearch NativeQuery Builder.
 * <p>
 * Builder Pattern: Karmaşık Elasticsearch sorgularını adım adım oluşturur.
 * <p>
 * Neden NativeQuery / CriteriaQuery?
 * - Spring Data'nın @Query anotasyonu statik JSON string'ler kullanır.
 * - Dinamik filtreler (opsiyonel keyword, category, price range) için
 * programatik query builder çok daha temiz ve type-safe'dir.
 * - CriteriaQuery, Elasticsearch DSL'ini Java API'si üzerinden ifade eder.
 * <p>
 * Sorgu Stratejisi:
 * - keyword varsa: name^3, description^2, category, brand alanlarında
 * multi-match
 * - category varsa: exact match (Keyword field)
 * - brand varsa: exact match (Keyword field)
 * - price range varsa: range filter
 * Tüm kriterler AND mantığıyla birleştirilir.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ProductSearchQueryBuilder {

    /**
     * Verilen parametrelerden dinamik Elasticsearch sorgusu oluşturur.
     * Null parametreler sorguya dahil edilmez (optional filter pattern).
     *
     * @param keyword  full-text arama terimi (name, description, category, brand)
     * @param category kategori filtresi (exact match)
     * @param brand    marka filtresi (exact match)
     * @param minPrice minimum fiyat (inclusive)
     * @param maxPrice maksimum fiyat (inclusive)
     * @param pageable sayfalama ve sıralama bilgisi
     * @return Elasticsearch'e gönderilecek Query nesnesi
     */
    public Query buildSearchQuery(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        // Criteria: AND mantığıyla birleştirilecek filtreler zinciri
        Criteria criteria = new Criteria();
        boolean hasCriteria = false;

        // Full-text arama: birden fazla alana boost ile arama
        if (keyword != null && !keyword.isBlank()) {
            // name alanına 3x boost, description'a 2x boost
            Criteria keywordCriteria = new Criteria("name").boost(3f).matches(keyword)
                    .or(new Criteria("description").boost(2f).matches(keyword))
                    .or(new Criteria("category").matches(keyword))
                    .or(new Criteria("brand").matches(keyword));
            criteria = hasCriteria ? criteria.and(keywordCriteria) : keywordCriteria;
            hasCriteria = true;
        }

        // Kategori filtresi: Keyword field — exact match
        if (category != null && !category.isBlank()) {
            Criteria categoryCriteria = new Criteria("category").is(category);
            criteria = hasCriteria ? criteria.and(categoryCriteria) : categoryCriteria;
            hasCriteria = true;
        }

        // Marka filtresi: Keyword field — exact match
        if (brand != null && !brand.isBlank()) {
            Criteria brandCriteria = new Criteria("brand").is(brand);
            criteria = hasCriteria ? criteria.and(brandCriteria) : brandCriteria;
            hasCriteria = true;
        }

        // Fiyat aralığı filtresi: range query
        if (minPrice != null && maxPrice != null) {
            Criteria priceCriteria = new Criteria("price").greaterThanEqual(minPrice).lessThanEqual(maxPrice);
            criteria = hasCriteria ? criteria.and(priceCriteria) : priceCriteria;
            hasCriteria = true;
        } else if (minPrice != null) {
            Criteria priceCriteria = new Criteria("price").greaterThanEqual(minPrice);
            criteria = hasCriteria ? criteria.and(priceCriteria) : priceCriteria;
            hasCriteria = true;
        } else if (maxPrice != null) {
            Criteria priceCriteria = new Criteria("price").lessThanEqual(maxPrice);
            criteria = hasCriteria ? criteria.and(priceCriteria) : priceCriteria;
            hasCriteria = true;
        }

        // Hiçbir filtre yoksa match_all sorgusu (tüm aktif ürünler)
        if (!hasCriteria) {
            criteria = new Criteria("status").is("ACTIVE");
        }

        return new CriteriaQuery(criteria, pageable);
    }
}
