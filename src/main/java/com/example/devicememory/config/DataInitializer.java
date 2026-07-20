package com.example.devicememory.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds ~30,000 device rows on startup if the table is empty,
 * using JDBC batch inserts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final int TOTAL_ROWS = 30_000;
    private static final int BATCH_SIZE = 1_000;

    private static final String[][] MAKES_AND_MODELS = {
            {"Apple", "iPhone 15", "iPhone 15 Pro", "iPhone 14", "iPhone SE"},
            {"Samsung", "Galaxy S24", "Galaxy S24 Ultra", "Galaxy A54", "Galaxy Z Flip 5"},
            {"Google", "Pixel 9", "Pixel 9 Pro", "Pixel 8a", "Pixel Fold"},
            {"Motorola", "Edge 50", "Razr 40", "Moto G84", "Edge 40 Neo"},
            {"OnePlus", "12", "12R", "Nord 3", "Open"},
            {"Nothing", "Phone (2)", "Phone (2a)", "Phone (1)", "CMF Phone 1"}
    };

    private static final String[] COMPATIBILITY_VALUES = {"YES", "NO", "SUPPORTED", "NOT_SUPPORTED"};

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Long existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM device", Long.class);
        if (existing != null && existing > 0) {
            log.info("Device table already contains {} rows, skipping data generation", existing);
            return;
        }

        log.info("Device table is empty, generating {} rows", TOTAL_ROWS);
        Random random = new Random();
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int inserted = 0;

        for (int i = 0; i < TOTAL_ROWS; i++) {
            String[] makeAndModels = MAKES_AND_MODELS[random.nextInt(MAKES_AND_MODELS.length)];
            String make = makeAndModels[0];
            String model = makeAndModels[1 + random.nextInt(makeAndModels.length - 1)];
            String esim = COMPATIBILITY_VALUES[random.nextInt(COMPATIBILITY_VALUES.length)];
            String fiveg = COMPATIBILITY_VALUES[random.nextInt(COMPATIBILITY_VALUES.length)];
            batch.add(new Object[]{make, model, esim, fiveg});

            if (batch.size() == BATCH_SIZE) {
                inserted += insertBatch(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            inserted += insertBatch(batch);
        }

        log.info("Inserted {} device rows", inserted);
    }

    private int insertBatch(final List<Object[]> batch) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO device (make, model, esim_compatibility, fiveg_compatibility) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Object[] row = batch.get(i);
                        ps.setString(1, (String) row[0]);
                        ps.setString(2, (String) row[1]);
                        ps.setString(3, (String) row[2]);
                        ps.setString(4, (String) row[3]);
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                });
        return batch.size();
    }
}
