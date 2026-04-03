package com.monat.ecommerce.order.infrastructure.reporting;

import java.util.List;

public record OrderReadPage<T>(List<T> content, long totalElements) {
}
