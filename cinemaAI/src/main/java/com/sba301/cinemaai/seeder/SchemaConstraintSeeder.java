package com.sba301.cinemaai.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate ddl-auto=update không sửa CHECK constraint đã tồn tại — khi thêm giá trị
 * enum mới (vd PaymentProvider.CASH) phải tự đồng bộ constraint, nếu không insert sẽ fail.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SchemaConstraintSeeder implements Seeder {

    private final JdbcTemplate jdbc;

    @Override
    public void seed() {
        try {
            jdbc.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_provider_check");
            jdbc.execute("""
                    ALTER TABLE payments ADD CONSTRAINT payments_provider_check
                    CHECK (provider IN ('MOCK', 'VNPAY', 'MOMO', 'CASH'))
                    """);
        } catch (Exception e) {
            log.warn("Không đồng bộ được payments_provider_check: {}", e.getMessage());
        }
    }
}
