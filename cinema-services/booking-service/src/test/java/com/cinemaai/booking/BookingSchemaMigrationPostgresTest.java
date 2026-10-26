package com.cinemaai.booking;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="local.db.verify", matches="true")
class BookingSchemaMigrationPostgresTest {
    DriverManagerDataSource datasource(String schema) {
        String url=System.getProperty("local.db.booking.url");
        if(!"jdbc:postgresql://127.0.0.1:57103/booking_db".equals(url))
            throw new IllegalArgumentException("Task-owned disposable PostgreSQL required");
        return new DriverManagerDataSource(url+"?currentSchema="+schema,"booking_verify",System.getProperty("local.db.password"));
    }
    Flyway flyway(String schema, String target) {
        return Flyway.configure().dataSource(datasource(schema)).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target(target).load();
    }
    void validateSchema(String schema) {
        var factory=new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(datasource(schema)); factory.setPackagesToScan("com.cinemaai.booking.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));
        try { factory.afterPropertiesSet(); assertNotNull(factory.getObject()); }
        finally { factory.destroy(); }
    }
    void verifyColumns(String schema) {
        var jdbc=new JdbcTemplate(datasource(schema));
        for(var column:Map.of("customer_name_snapshot",255,"customer_phone_snapshot",50,"customer_email_snapshot",255).entrySet()) {
            var row=jdbc.queryForMap("select data_type,character_maximum_length,is_nullable from information_schema.columns where table_schema=? and table_name='bookings' and column_name=?",schema,column.getKey());
            assertEquals("character varying",row.get("data_type"));
            assertEquals(column.getValue(),row.get("character_maximum_length")); assertEquals("YES",row.get("is_nullable"));
        }
        assertEquals(1,jdbc.queryForObject("select count(*) from pg_indexes where schemaname=? and indexname='uq_booking_seats_active_showtime_seat'",Integer.class,schema));
        var end=jdbc.queryForMap("select data_type,is_nullable from information_schema.columns where table_schema=? and table_name='bookings' and column_name='showtime_end_snapshot'",schema);
        assertEquals("timestamp without time zone",end.get("data_type")); assertEquals("YES",end.get("is_nullable"));
        for(var column:Map.of("cinema_name",255,"cinema_address",500).entrySet()) {
            var row=jdbc.queryForMap("select character_maximum_length,is_nullable from information_schema.columns where table_schema=? and table_name='food_orders' and column_name=?",schema,column.getKey());
            assertEquals(column.getValue(),row.get("character_maximum_length")); assertEquals("YES",row.get("is_nullable"));
        }
        assertEquals("bigint",jdbc.queryForObject("select data_type from information_schema.columns where table_schema=? and table_name='food_orders' and column_name='cinema_id'",String.class,schema));
    }
    @Test void cleanInstallationMatchesAllEntityMappings() {
        String schema="booking_clean_it_"+Long.toUnsignedString(System.nanoTime());
        flyway(schema,"latest").migrate(); verifyColumns(schema); validateSchema(schema);
    }
    @Test void upgradingV5PreservesExistingBookingsAndAllowsSnapshots() {
        String schema="booking_upgrade_it_"+Long.toUnsignedString(System.nanoTime());
        flyway(schema,"5").migrate(); var jdbc=new JdbcTemplate(datasource(schema));
        jdbc.update("insert into bookings(booking_code,user_id,showtime_id,movie_id,movie_title_snapshot,cinema_name_snapshot,room_name_snapshot,showtime_start_snapshot,total_amount,status,created_at,updated_at) values ('UPGRADE-PRESERVED',7,10,20,'Movie','Cinema','Room',current_timestamp,123456.78,'PAID',current_timestamp,current_timestamp)");
        var before=jdbc.queryForMap("select booking_code,user_id,total_amount,status,created_at,updated_at from bookings where booking_code='UPGRADE-PRESERVED'");
        jdbc.update("insert into food_orders(food_order_code,user_id,total_amount,status,created_at,updated_at) values ('FOOD-PRESERVED',7,50000,'PAID',current_timestamp,current_timestamp)");
        var foodBefore=jdbc.queryForMap("select * from food_orders where food_order_code='FOOD-PRESERVED'");
        flyway(schema,"latest").migrate(); verifyColumns(schema); validateSchema(schema);
        assertEquals(before,jdbc.queryForMap("select booking_code,user_id,total_amount,status,created_at,updated_at from bookings where booking_code='UPGRADE-PRESERVED'"));
        var foodAfter=jdbc.queryForMap("select * from food_orders where food_order_code='FOOD-PRESERVED'");
        foodBefore.forEach((key,value)->assertEquals(value,foodAfter.get(key)));
        assertNull(foodAfter.get("cinema_id")); assertNull(foodAfter.get("cinema_name")); assertNull(foodAfter.get("cinema_address"));
        assertEquals(1,jdbc.queryForObject("select count(*) from bookings where customer_name_snapshot is null and customer_phone_snapshot is null and customer_email_snapshot is null",Integer.class));
        jdbc.update("update bookings set customer_name_snapshot='Test Customer',customer_phone_snapshot='0900',customer_email_snapshot='test@example.invalid' where booking_code='UPGRADE-PRESERVED'");
        assertEquals("Test Customer",jdbc.queryForObject("select customer_name_snapshot from bookings where booking_code='UPGRADE-PRESERVED'",String.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void upgradingV5WithExistingSnapshotColumnsPreservesData(boolean allColumnsExist) {
        String schema="booking_existing_it_"+Long.toUnsignedString(System.nanoTime());
        flyway(schema,"5").migrate(); var jdbc=new JdbcTemplate(datasource(schema));
        jdbc.execute("alter table bookings add column customer_name_snapshot varchar(255), add column showtime_end_snapshot timestamp");
        jdbc.execute("alter table food_orders add column cinema_id bigint");
        if(allColumnsExist) {
            jdbc.execute("alter table bookings add column customer_phone_snapshot varchar(50), add column customer_email_snapshot varchar(255)");
            jdbc.execute("alter table food_orders add column cinema_name varchar(255), add column cinema_address varchar(500)");
        }
        jdbc.update("insert into bookings(booking_code,user_id,showtime_id,movie_id,movie_title_snapshot,cinema_name_snapshot,room_name_snapshot,showtime_start_snapshot,showtime_end_snapshot,customer_name_snapshot,total_amount,status,created_at,updated_at) values ('EXISTING-SNAPSHOTS',7,10,20,'Movie','Cinema','Room',current_timestamp,current_timestamp + interval '2 hours','Existing Customer',123456.78,'PAID',current_timestamp,current_timestamp)");
        jdbc.update("insert into food_orders(food_order_code,user_id,cinema_id,total_amount,status,created_at,updated_at) values ('EXISTING-FOOD',7,42,50000,'PAID',current_timestamp,current_timestamp)");
        var before=jdbc.queryForMap("select booking_code,customer_name_snapshot,showtime_end_snapshot,total_amount from bookings where booking_code='EXISTING-SNAPSHOTS'");
        var foodBefore=jdbc.queryForMap("select food_order_code,cinema_id,total_amount from food_orders where food_order_code='EXISTING-FOOD'");

        assertDoesNotThrow(() -> flyway(schema,"latest").migrate());

        verifyColumns(schema); validateSchema(schema);
        assertEquals(before,jdbc.queryForMap("select booking_code,customer_name_snapshot,showtime_end_snapshot,total_amount from bookings where booking_code='EXISTING-SNAPSHOTS'"));
        assertEquals(foodBefore,jdbc.queryForMap("select food_order_code,cinema_id,total_amount from food_orders where food_order_code='EXISTING-FOOD'"));
    }
}
