package com.fintechathon.silvershield.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintechathon.silvershield.config.SandboxProperties;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SandboxDatasetGeneratorTest {

    private static final String CUSTOMER_HASH = "$2a$10$M5FwqEuCyAAsOSZHCN69.e8KDhjye3wC60un8rDDRUBwGBcADwtp2";
    private static final String ADMIN_HASH = "$2a$10$l/OLvObWZWXXefvMbWIra.RrjD6ak0ct6RXoBfC/xtyKXAmKcVN6q";

    @Test
    void sameSeedBaselineAndVersionMustGenerateIdenticalDataset() {
        SandboxDataset first = generator(2026L).generate();
        SandboxDataset second = generator(2026L).generate();

        assertThat(second).isEqualTo(first);
        assertThat(first.datasetVersion()).isEqualTo("fintechathon-demo-v1");
        assertThat(first.baselineDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(first.users()).hasSize(4);
        assertThat(first.accounts()).hasSize(3);
        assertThat(first.cards()).hasSize(3);
        assertThat(first.payees()).hasSize(7);
        assertThat(first.subscriptions()).hasSize(8);
        assertThat(first.wealthProducts()).hasSize(8);
        assertThat(first.transactions()).hasSizeGreaterThan(200);
    }

    @Test
    void differentSeedMustChangeGeneratedTransactionsAndBalances() {
        SandboxDataset first = generator(2026L).generate();
        SandboxDataset second = generator(2027L).generate();

        assertThat(second.transactions()).isNotEqualTo(first.transactions());
        assertThat(second.accounts()).isNotEqualTo(first.accounts());
    }

    @Test
    void datasetMustOnlyContainSyntheticMaskedBankingIdentifiers() {
        SandboxDataset dataset = generator(2026L).generate();

        assertThat(dataset.accounts()).allSatisfy(account -> {
            assertThat(account.accountToken()).startsWith("MOCK-");
            assertThat(account.accountMasked()).startsWith("****");
        });
        assertThat(dataset.cards()).allSatisfy(card -> {
            assertThat(card.cardToken()).startsWith("MOCK-");
            assertThat(card.cardMasked()).startsWith("****");
        });
        assertThat(dataset.users()).allSatisfy(user -> assertThat(user.displayName()).contains("虚构"));
    }

    private static SandboxDatasetGenerator generator(long seed) {
        SandboxProperties properties = new SandboxProperties();
        properties.setEnabled(true);
        properties.setDatasetVersion("fintechathon-demo-v1");
        properties.setRandomSeed(seed);
        properties.setBaselineDate(LocalDate.of(2026, 8, 31));
        properties.setCustomerPasswordHash(CUSTOMER_HASH);
        properties.setAdminPasswordHash(ADMIN_HASH);
        return new SandboxDatasetGenerator(properties);
    }
}
