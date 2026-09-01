package com.fintechathon.silvershield.sandbox;

import com.fintechathon.silvershield.config.SandboxProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"dev", "test"})
public class SandboxResetService {

    private final SandboxProperties properties;
    private final SandboxDatasetGenerator generator;
    private final SandboxDatasetStore store;
    private final SandboxDigestService digestService;
    private final JdbcTemplate jdbcTemplate;

    public SandboxResetService(
            SandboxProperties properties,
            SandboxDatasetGenerator generator,
            SandboxDatasetStore store,
            SandboxDigestService digestService,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.generator = generator;
        this.store = store;
        this.digestService = digestService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public synchronized SandboxDatasetMetadata reset() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Sandbox reset is disabled");
        }
        SandboxDataset dataset = generator.generate();
        store.clear();
        store.insert(dataset);
        String digest = digestService.computePersistedDigest();
        store.insertMetadata(dataset, digest);
        return metadata().orElseThrow();
    }

    @Transactional(readOnly = true)
    public Optional<SandboxDatasetMetadata> metadata() {
        return jdbcTemplate.query("""
                        SELECT dataset_id, dataset_version, random_seed, baseline_date, dataset_digest, generated_at
                        FROM sandbox_dataset_metadata WHERE dataset_id = 'banking-sandbox'
                        """, this::mapMetadata)
                .stream()
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean isCurrentAndIntact() {
        return metadata()
                .filter(row -> row.datasetVersion().equals(properties.getDatasetVersion()))
                .filter(row -> row.randomSeed() == properties.getRandomSeed())
                .filter(row -> row.baselineDate().equals(properties.getBaselineDate()))
                .filter(row -> row.datasetDigest().equals(digestService.computePersistedDigest()))
                .isPresent();
    }

    private SandboxDatasetMetadata mapMetadata(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SandboxDatasetMetadata(
                resultSet.getString("dataset_id"),
                resultSet.getString("dataset_version"),
                resultSet.getLong("random_seed"),
                resultSet.getDate("baseline_date").toLocalDate(),
                resultSet.getString("dataset_digest"),
                resultSet.getTimestamp("generated_at").toInstant(),
                digestService.rowCounts());
    }
}
