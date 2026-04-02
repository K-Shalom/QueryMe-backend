package com.year2.queryme.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database schema repair...");
        try {
            // Fix ID columns to be BIGINT instead of padded strings/binary
            // We use MODIFY to change the type while keeping the AUTO_INCREMENT property
            
            // 1. Users table
            jdbcTemplate.execute("ALTER TABLE users MODIFY id BIGINT AUTO_INCREMENT");
            log.info("Repaired users table schema.");

            // 2. Teachers table
            jdbcTemplate.execute("ALTER TABLE teachers MODIFY id BIGINT AUTO_INCREMENT");
            log.info("Repaired teachers table schema.");

            // 3. Courses table
            jdbcTemplate.execute("ALTER TABLE courses MODIFY id BIGINT AUTO_INCREMENT");
            log.info("Repaired courses table schema.");

            // 4. Students table
            jdbcTemplate.execute("ALTER TABLE students MODIFY id BIGINT AUTO_INCREMENT");
            log.info("Repaired students table schema.");

            // 5. Admins/Guests/ClassGroups (if they exist and have issues)
            try { jdbcTemplate.execute("ALTER TABLE admins MODIFY id BIGINT AUTO_INCREMENT"); } catch (Exception e) {}
            try { jdbcTemplate.execute("ALTER TABLE guests MODIFY id BIGINT AUTO_INCREMENT"); } catch (Exception e) {}
            try { jdbcTemplate.execute("ALTER TABLE class_groups MODIFY id BIGINT AUTO_INCREMENT"); } catch (Exception e) {}
            
            log.info("Database schema repair completed successfully.");
        } catch (Exception e) {
            log.error("Failed to repair database schema: {}", e.getMessage());
            // We don't throw the exception to avoid crashing startup if tables are already fixed
        }
    }
}
