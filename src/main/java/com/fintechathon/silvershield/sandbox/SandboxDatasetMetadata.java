package com.fintechathon.silvershield.sandbox;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record SandboxDatasetMetadata(
        String datasetId,
        String datasetVersion,
        long randomSeed,
        LocalDate baselineDate,
        String datasetDigest,
        Instant generatedAt,
        Map<String, Long> rowCounts) {}
