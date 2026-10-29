package com.fintechathon.silvershield.sandbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fintechathon.silvershield.config.SandboxProperties;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SandboxDatasetInitializerTest {
    private final SandboxProperties properties = new SandboxProperties();
    private final SandboxResetService reset = mock(SandboxResetService.class);
    private final SandboxDatasetInitializer initializer = new SandboxDatasetInitializer(properties, reset);

    @Test
    void existingDatasetWithBusinessChangesMustSurviveRestart() {
        properties.setEnabled(true);
        var metadata = matchingMetadata();
        when(reset.metadata()).thenReturn(Optional.of(metadata));
        when(reset.isCurrentAndIntact()).thenReturn(false);
        initializer.run(null);
        verify(reset, never()).reset();
        verify(reset, never()).isCurrentAndIntact();
    }

    @Test
    void firstStartupInitializesMissingDataset() {
        properties.setEnabled(true);
        when(reset.metadata()).thenReturn(Optional.empty());
        var metadata = matchingMetadata();
        when(reset.reset()).thenReturn(metadata);
        initializer.run(null);
        verify(reset).reset();
    }

    @Test
    void changedDatasetDefinitionRequiresExplicitResetInsteadOfDiscardingData() {
        properties.setEnabled(true);
        var metadata = matchingMetadata();
        when(metadata.datasetVersion()).thenReturn("old-version");
        when(reset.metadata()).thenReturn(Optional.of(metadata));
        assertThatThrownBy(() -> initializer.run(null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("explicit reset");
        verify(reset, never()).reset();
    }

    @Test
    void disabledSandboxDoesNotTouchData() {
        properties.setEnabled(false);
        initializer.run(null);
        verifyNoInteractions(reset);
    }

    private SandboxDatasetMetadata matchingMetadata() {
        var metadata = mock(SandboxDatasetMetadata.class);
        when(metadata.datasetVersion()).thenReturn(properties.getDatasetVersion());
        when(metadata.randomSeed()).thenReturn(properties.getRandomSeed());
        when(metadata.baselineDate()).thenReturn(properties.getBaselineDate());
        return metadata;
    }
}
