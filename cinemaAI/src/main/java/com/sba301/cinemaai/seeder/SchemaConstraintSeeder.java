package com.sba301.cinemaai.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Đồng bộ các CHECK constraint và cấu trúc bảng phục vụ movie approval workflow.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SchemaConstraintSeeder implements Seeder {

    private final JdbcTemplate jdbc;

    @Override
    public void seed() {
        syncPaymentConstraints();
        syncMovieApprovalSchema();
    }

    private void syncPaymentConstraints() {
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

    private void syncMovieApprovalSchema() {
        try {
            // Thêm các cột cho movie nếu chưa có
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS approval_status VARCHAR(30) DEFAULT 'APPROVED'");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS publication_status VARCHAR(30) DEFAULT 'PUBLISHED'");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP(6) WITHOUT TIME ZONE");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS submitted_by BIGINT REFERENCES users(id)");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP(6) WITHOUT TIME ZONE");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS approved_by BIGINT REFERENCES users(id)");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP(6) WITHOUT TIME ZONE");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS rejected_by BIGINT REFERENCES users(id)");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS rejection_reason TEXT");
            jdbc.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS published_at TIMESTAMP(6) WITHOUT TIME ZONE");

            // Cập nhật dữ liệu phim cũ sang APPROVED và PUBLISHED để không làm gián đoạn suất chiếu
            jdbc.execute("UPDATE movies SET approval_status = 'APPROVED' WHERE approval_status IS NULL");
            jdbc.execute("UPDATE movies SET publication_status = 'PUBLISHED' WHERE publication_status IS NULL");
            jdbc.execute("UPDATE movies SET publication_status = 'ARCHIVED' WHERE status = 'INACTIVE'");

            // Cập nhật constraints
            jdbc.execute("ALTER TABLE movies DROP CONSTRAINT IF EXISTS movies_approval_status_check");
            jdbc.execute("""
                    ALTER TABLE movies ADD CONSTRAINT movies_approval_status_check
                    CHECK (approval_status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'))
                    """);

            jdbc.execute("ALTER TABLE movies DROP CONSTRAINT IF EXISTS movies_publication_status_check");
            jdbc.execute("""
                    ALTER TABLE movies ADD CONSTRAINT movies_publication_status_check
                    CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED', 'ARCHIVED'))
                    """);

            // Chỉ mục
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_movies_approval_status ON movies(approval_status)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_movies_publication_status ON movies(publication_status)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_movies_app_pub_status ON movies(approval_status, publication_status)");

            // Bảng lịch sử duyệt phim
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS movie_approval_histories (
                        id BIGSERIAL PRIMARY KEY,
                        movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                        action VARCHAR(30) NOT NULL,
                        from_status VARCHAR(30),
                        to_status VARCHAR(30),
                        comment TEXT,
                        actor_user_id BIGINT REFERENCES users(id),
                        created_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_mah_movie_id ON movie_approval_histories(movie_id)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_mah_actor_id ON movie_approval_histories(actor_user_id)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_mah_created_at ON movie_approval_histories(created_at)");

            log.info("Đã đồng bộ thành công cấu trúc bảng và dữ liệu cho Movie Approval & Publication Workflow.");
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ Movie Approval schema: {}", e.getMessage(), e);
        }
    }
}
