package com.monat.ecommerce.order.infrastructure.reporting;

import com.monat.ecommerce.order.application.dto.DailySalesReportResponse;
import com.monat.ecommerce.order.application.dto.OrderStatusDistributionResponse;
import com.monat.ecommerce.order.domain.model.OrderStatus;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcOrderAnalyticsRepository implements OrderAnalyticsRepository {

    private static final RowMapper<OrderSummaryReadModel> ORDER_SUMMARY_MAPPER = new OrderSummaryRowMapper();
    private static final RowMapper<DailySalesReportResponse> DAILY_REPORT_MAPPER = (rs, rowNum) ->
            new DailySalesReportResponse(
                    rs.getObject("sales_date", LocalDate.class),
                    rs.getString("status"),
                    rs.getString("currency"),
                    rs.getLong("order_count"),
                    rs.getLong("unique_customers"),
                    rs.getBigDecimal("total_sales"),
                    rs.getBigDecimal("average_order_value"));
    private static final RowMapper<OrderStatusDistributionResponse> STATUS_DISTRIBUTION_MAPPER = (rs, rowNum) ->
            new OrderStatusDistributionResponse(
                    rs.getString("status"),
                    rs.getLong("order_count"),
                    rs.getBigDecimal("total_sales"),
                    rs.getBigDecimal("share_percentage"));

    private final NamedParameterJdbcTemplate replicaJdbcTemplate;

    private final JdbcTemplate primaryJdbcTemplate;

    public JdbcOrderAnalyticsRepository(
            @Qualifier("replicaNamedParameterJdbcTemplate") NamedParameterJdbcTemplate replicaJdbcTemplate,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbcTemplate) {
        this.replicaJdbcTemplate = replicaJdbcTemplate;
        this.primaryJdbcTemplate = primaryJdbcTemplate;
    }

    @Override
    public OrderReadPage<OrderSummaryReadModel> findOrders(OrderStatus status, int page, int size) {
        String baseSql = """
                FROM orders_read_model
                WHERE (:status IS NULL OR status = :status)
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("status", status != null ? status.name() : null)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        List<OrderSummaryReadModel> content = replicaJdbcTemplate.query("""
                SELECT id, order_number, user_id, status, total_amount, currency,
                       payment_reference, cancellation_reason, created_at, updated_at
                """ + baseSql + """
                ORDER BY created_at DESC, id DESC
                LIMIT :limit OFFSET :offset
                """, parameters, ORDER_SUMMARY_MAPPER);
        Long totalElements = replicaJdbcTemplate.queryForObject("SELECT COUNT(*) " + baseSql, parameters, Long.class);
        return new OrderReadPage<>(content, totalElements != null ? totalElements : 0L);
    }

    @Override
    public OrderReadPage<OrderSummaryReadModel> findUserOrderHistory(UUID userId, int page, int size) {
        Objects.requireNonNull(userId, "userId must not be null");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        List<OrderSummaryReadModel> content = replicaJdbcTemplate.query("""
                SELECT id, order_number, user_id, status, total_amount, currency,
                       payment_reference, cancellation_reason, created_at, updated_at
                FROM orders_read_model
                WHERE user_id = :userId
                ORDER BY created_at DESC, id DESC
                LIMIT :limit OFFSET :offset
                """, parameters, ORDER_SUMMARY_MAPPER);
        Long totalElements = replicaJdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM orders_read_model
                WHERE user_id = :userId
                """, parameters, Long.class);
        return new OrderReadPage<>(content, totalElements != null ? totalElements : 0L);
    }

    @Override
    public List<DailySalesReportResponse> findDailySalesReport(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("startDate", resolvedStart)
                .addValue("endDate", resolvedEnd);
        return replicaJdbcTemplate.query("""
                SELECT sales_date, status, currency, order_count, unique_customers, total_sales, average_order_value
                FROM mv_daily_sales_report
                WHERE sales_date BETWEEN :startDate AND :endDate
                ORDER BY sales_date DESC, status ASC, currency ASC
                """, parameters, DAILY_REPORT_MAPPER);
    }

    @Override
    public List<OrderStatusDistributionResponse> findOrderStatusDistribution() {
        return replicaJdbcTemplate.getJdbcTemplate().query("""
                SELECT status, order_count, total_sales, share_percentage
                FROM mv_order_status_distribution
                ORDER BY order_count DESC, status ASC
                """, STATUS_DISTRIBUTION_MAPPER);
    }

    @Override
    public void refreshMaterializedViews() {
        primaryJdbcTemplate.execute("REFRESH MATERIALIZED VIEW mv_daily_sales_report");
        primaryJdbcTemplate.execute("REFRESH MATERIALIZED VIEW mv_order_status_distribution");
    }

    private static final class OrderSummaryRowMapper implements RowMapper<OrderSummaryReadModel> {

        @Override
        public OrderSummaryReadModel mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OrderSummaryReadModel(
                    rs.getObject("id", UUID.class),
                    rs.getString("order_number"),
                    rs.getObject("user_id", UUID.class),
                    rs.getString("status"),
                    rs.getBigDecimal("total_amount"),
                    rs.getString("currency"),
                    rs.getString("payment_reference"),
                    rs.getString("cancellation_reason"),
                    rs.getObject("created_at", LocalDateTime.class),
                    rs.getObject("updated_at", LocalDateTime.class));
        }
    }
}
