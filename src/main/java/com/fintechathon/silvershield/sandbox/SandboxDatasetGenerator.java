package com.fintechathon.silvershield.sandbox;

import com.fintechathon.silvershield.config.SandboxProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("!prod & (dev | test | offline-demo)")
public class SandboxDatasetGenerator {

    private static final String NAMESPACE = "silver-shield/fintechathon-demo-v2/";

    private final SandboxProperties properties;

    public SandboxDatasetGenerator(SandboxProperties properties) {
        this.properties = properties;
    }

    public SandboxDataset generate() {
        validateConfiguration();
        LocalDate baseline = properties.getBaselineDate();
        Instant fixedTime = baseline.atStartOfDay().toInstant(ZoneOffset.UTC);
        SplittableRandom random = new SplittableRandom(properties.getRandomSeed());

        UUID aliceId = id("user/alice");
        UUID bobId = id("user/bob");
        UUID adminId = id("user/admin");
        UUID lockedId = id("user/locked");

        List<SandboxDataset.UserSeed> users = List.of(
                new SandboxDataset.UserSeed(aliceId, "demo.alice", properties.getCustomerPasswordHash(),
                        "林晓岚（虚构）", "CUSTOMER", "ACTIVE", fixedTime),
                new SandboxDataset.UserSeed(bobId, "demo.bob", properties.getCustomerPasswordHash(),
                        "陈思远（虚构）", "CUSTOMER", "ACTIVE", fixedTime),
                new SandboxDataset.UserSeed(adminId, "demo.admin", properties.getAdminPasswordHash(),
                        "沙箱管理员（虚构）", "DEMO_ADMIN", "ACTIVE", fixedTime),
                new SandboxDataset.UserSeed(lockedId, "demo.locked", properties.getCustomerPasswordHash(),
                        "锁定用户（虚构）", "CUSTOMER", "LOCKED", fixedTime));

        GeneratedAccount aliceChecking = generateChecking(
                "alice/checking", aliceId, "MOCK-ACC-ALICE-8899", "**** 8899", "日常账户",
                new BigDecimal("52000.00"), new BigDecimal("50000.00"), baseline, fixedTime, random.split(), true);
        GeneratedAccount aliceSavings = generateSavings(
                "alice/savings", aliceId, "MOCK-ACC-ALICE-6628", "**** 6628", "安心储蓄",
                new BigDecimal("180000.00"), baseline, fixedTime);
        GeneratedAccount bobChecking = generateChecking(
                "bob/checking", bobId, "MOCK-ACC-BOB-3106", "**** 3106", "工资账户",
                new BigDecimal("18000.00"), new BigDecimal("20000.00"), baseline, fixedTime, random.split(), false);

        List<SandboxDataset.AccountSeed> accounts = List.of(
                aliceChecking.account(), aliceSavings.account(), bobChecking.account());
        List<SandboxDataset.RiskProfileSeed> riskProfiles = List.of(
                new SandboxDataset.RiskProfileSeed(id("risk/alice"), aliceId, "R3", baseline.minusMonths(2),
                        baseline.plusMonths(10), "FIXED_SANDBOX", fixedTime),
                new SandboxDataset.RiskProfileSeed(id("risk/bob"), bobId, "R2", baseline.minusMonths(1),
                        baseline.plusMonths(11), "FIXED_SANDBOX", fixedTime));
        List<SandboxDataset.TransactionSeed> transactions = new ArrayList<>();
        transactions.addAll(aliceChecking.transactions());
        transactions.addAll(aliceSavings.transactions());
        transactions.addAll(bobChecking.transactions());
        transactions.sort(Comparator.comparing(SandboxDataset.TransactionSeed::id));

        List<SandboxDataset.CardSeed> cards = List.of(
                card("alice/debit", aliceId, aliceChecking.account().id(), "MOCK-CARD-ALICE-1001",
                        "**** **** **** 1001", "DEBIT", "ACTIVE", true, false, "5000.00", "20000.00", "2030-08", fixedTime),
                card("alice/credit", aliceId, aliceChecking.account().id(), "MOCK-CARD-ALICE-2038",
                        "**** **** **** 2038", "CREDIT", "FROZEN", false, false, "3000.00", "10000.00", "2029-12", fixedTime),
                card("bob/debit", bobId, bobChecking.account().id(), "MOCK-CARD-BOB-7782",
                        "**** **** **** 7782", "DEBIT", "ACTIVE", true, false, "3000.00", "12000.00", "2030-03", fixedTime));

        List<SandboxDataset.PayeeSeed> payees = List.of(
                payee("alice/payee/zhang", aliceId, "张伟（虚构）", "星海银行（模拟）", "MOCK-PAYEE-A-001", "**** 3208",
                        bobChecking.account().id(), true, baseline.minusDays(12), fixedTime),
                payee("alice/payee/li", aliceId, "李然（虚构）", "云帆银行（模拟）", "MOCK-PAYEE-A-002", "**** 7719",
                        null, true, baseline.minusDays(40), fixedTime),
                payee("alice/payee/zhou", aliceId, "周晴（虚构）", "远山银行（模拟）", "MOCK-PAYEE-A-003", "**** 5066",
                        null, false, null, fixedTime),
                payee("alice/payee/property", aliceId, "青禾物业（虚构）", "星海银行（模拟）", "MOCK-PAYEE-A-004", "**** 1142",
                        null, true, baseline.minusDays(28), fixedTime),
                payee("alice/payee/family", aliceId, "家用备用金（虚构）", "云帆银行（模拟）", "MOCK-PAYEE-A-005", "**** 9907",
                        null, true, baseline.minusDays(5), fixedTime),
                payee("bob/payee/wang", bobId, "王宁（虚构）", "远山银行（模拟）", "MOCK-PAYEE-B-001", "**** 0831",
                        null, true, baseline.minusDays(22), fixedTime),
                payee("bob/payee/new", bobId, "新收款人（虚构）", "星海银行（模拟）", "MOCK-PAYEE-B-002", "**** 4405",
                        null, false, null, fixedTime));

        List<SandboxDataset.SubscriptionSeed> subscriptions = List.of(
                subscription("alice/sub/video", aliceId, aliceChecking.account().id(), "星云视频（虚构）", "视频会员", "28.00", "MONTHLY", baseline.plusDays(8), "ACTIVE", fixedTime),
                subscription("alice/sub/music", aliceId, aliceChecking.account().id(), "拾音音乐（虚构）", "音乐会员", "15.00", "MONTHLY", baseline.plusDays(11), "ACTIVE", fixedTime),
                subscription("alice/sub/cloud", aliceId, aliceChecking.account().id(), "云盒存储（虚构）", "云盘套餐", "18.00", "MONTHLY", baseline.plusDays(3), "ACTIVE", fixedTime),
                subscription("alice/sub/fitness", aliceId, aliceChecking.account().id(), "轻动健身（虚构）", "健身月卡", "99.00", "MONTHLY", baseline.plusDays(15), "PAUSED", fixedTime),
                subscription("alice/sub/news", aliceId, aliceChecking.account().id(), "见闻周刊（虚构）", "数字刊订阅", "168.00", "YEARLY", baseline.plusMonths(3), "ACTIVE", fixedTime),
                new SandboxDataset.SubscriptionSeed(id("alice/sub/cancelled"), aliceId, aliceChecking.account().id(),
                        "旧影像馆（虚构）", "已取消会员", money("20.00"), "CNY", "MONTHLY", null, "CANCELLED",
                        "FIXED_SANDBOX", "已取消，不会再次扣款", fixedTime),
                subscription("bob/sub/video", bobId, bobChecking.account().id(), "星云视频（虚构）", "视频会员", "28.00", "MONTHLY", baseline.plusDays(7), "ACTIVE", fixedTime),
                subscription("bob/sub/reader", bobId, bobChecking.account().id(), "墨舟阅读（虚构）", "阅读会员", "19.00", "MONTHLY", baseline.plusDays(19), "ACTIVE", fixedTime));

        List<SandboxDataset.WealthProductSeed> products = wealthProducts(baseline, fixedTime);

        return new SandboxDataset(properties.getDatasetVersion(), properties.getRandomSeed(), baseline, fixedTime,
                users, riskProfiles, accounts, cards, List.copyOf(transactions), payees, subscriptions, products);
    }

    private GeneratedAccount generateChecking(
            String key,
            UUID userId,
            String token,
            String masked,
            String alias,
            BigDecimal opening,
            BigDecimal dailyLimit,
            LocalDate baseline,
            Instant fixedTime,
            SplittableRandom random,
            boolean primaryDemo) {
        UUID accountId = id(key);
        BigDecimal balance = opening;
        List<SandboxDataset.TransactionSeed> rows = new ArrayList<>();
        int sequence = 0;
        String[][] merchants = {
            {"DINING", "榕树餐厅（虚构）"}, {"GROCERY", "日日鲜超市（虚构）"},
            {"TRANSPORT", "城市出行（虚构）"}, {"SHOPPING", "星港百货（虚构）"},
            {"MEDICAL", "安康药房（虚构）"}, {"ENTERTAINMENT", "微光影院（虚构）"}
        };

        for (LocalDate day = baseline.minusDays(179); !day.isAfter(baseline); day = day.plusDays(1)) {
            if (day.getDayOfMonth() == 1) {
                BigDecimal salary = primaryDemo ? money("18000.00") : money("9500.00");
                balance = balance.add(salary);
                rows.add(transaction(key, sequence++, accountId, day, 3, "IN", salary, "SALARY",
                        primaryDemo ? "远航科技工资（虚构）" : "青岚设计工资（虚构）", "模拟工资入账", null, balance));
            }
            if (day.getDayOfMonth() == 3) {
                BigDecimal rent = primaryDemo ? money("4800.00") : money("3200.00");
                balance = balance.subtract(rent);
                rows.add(transaction(key, sequence++, accountId, day, 4, "OUT", rent, "HOUSING",
                        "青禾物业（虚构）", "模拟房租", "**** 1142", balance));
            }
            if (primaryDemo && day.getDayOfMonth() == 8) {
                BigDecimal recurring = money("61.00");
                balance = balance.subtract(recurring);
                rows.add(transaction(key, sequence++, accountId, day, 5, "OUT", recurring, "SUBSCRIPTION",
                        "聚合订阅扣款（虚构）", "视频、音乐与云盘模拟扣款", null, balance));
            }
            if (random.nextDouble() < (primaryDemo ? 0.72 : 0.58)) {
                String[] merchant = merchants[random.nextInt(merchants.length)];
                BigDecimal amount = BigDecimal.valueOf(15 + random.nextInt(primaryDemo ? 486 : 286))
                        .add(BigDecimal.valueOf(random.nextInt(100), 2)).setScale(4, RoundingMode.UNNECESSARY);
                balance = balance.subtract(amount);
                rows.add(transaction(key, sequence++, accountId, day, 12, "OUT", amount, merchant[0], merchant[1],
                        "固定种子生成的模拟消费", null, balance));
            }
        }
        BigDecimal normalized = balance.setScale(4, RoundingMode.UNNECESSARY);
        SandboxDataset.AccountSeed account = new SandboxDataset.AccountSeed(
                accountId, userId, token, masked, alias, "CHECKING", "CNY", normalized, normalized,
                "ACTIVE", dailyLimit.setScale(4), fixedTime);
        return new GeneratedAccount(account, List.copyOf(rows));
    }

    private GeneratedAccount generateSavings(
            String key, UUID userId, String token, String masked, String alias,
            BigDecimal opening, LocalDate baseline, Instant fixedTime) {
        UUID accountId = id(key);
        BigDecimal balance = opening;
        List<SandboxDataset.TransactionSeed> rows = new ArrayList<>();
        int sequence = 0;
        for (int month = 5; month >= 0; month--) {
            LocalDate day = baseline.minusMonths(month).withDayOfMonth(20);
            BigDecimal interest = money("236.80");
            balance = balance.add(interest);
            rows.add(transaction(key, sequence++, accountId, day, 2, "IN", interest, "INTEREST",
                    "安心储蓄（虚构）", "模拟利息", null, balance));
        }
        BigDecimal normalized = balance.setScale(4, RoundingMode.UNNECESSARY);
        SandboxDataset.AccountSeed account = new SandboxDataset.AccountSeed(
                accountId, userId, token, masked, alias, "SAVINGS", "CNY", normalized, normalized,
                "ACTIVE", money("100000.00"), fixedTime);
        return new GeneratedAccount(account, List.copyOf(rows));
    }

    private static SandboxDataset.TransactionSeed transaction(
            String accountKey, int sequence, UUID accountId, LocalDate date, int hour, String direction,
            BigDecimal amount, String category, String merchant, String description, String counterparty,
            BigDecimal balanceAfter) {
        return new SandboxDataset.TransactionSeed(
                id(accountKey + "/tx/" + sequence), accountId, date.atTime(hour, 0).toInstant(ZoneOffset.UTC),
                direction, amount.setScale(4), "CNY", category, merchant, description, counterparty,
                "POSTED", balanceAfter.setScale(4));
    }

    private static SandboxDataset.CardSeed card(
            String key, UUID userId, UUID accountId, String token, String masked, String type, String status,
            boolean online, boolean overseas, String singleLimit, String dailyLimit, String expiry, Instant createdAt) {
        return new SandboxDataset.CardSeed(id(key), userId, accountId, token, masked, type, status, online,
                overseas, money(singleLimit), money(dailyLimit), expiry, createdAt);
    }

    private static SandboxDataset.PayeeSeed payee(
            String key, UUID userId, String name, String bank, String token, String masked,
            UUID internalAccountId, boolean trusted, LocalDate lastUsed, Instant createdAt) {
        return new SandboxDataset.PayeeSeed(id(key), userId, name, bank, token, masked, internalAccountId,
                "CNY", "ACTIVE", trusted,
                lastUsed == null ? null : lastUsed.atTime(9, 0).toInstant(ZoneOffset.UTC), createdAt);
    }

    private static SandboxDataset.SubscriptionSeed subscription(
            String key, UUID userId, UUID accountId, String merchant, String service, String amount,
            String cycle, LocalDate nextCharge, String status, Instant createdAt) {
        return new SandboxDataset.SubscriptionSeed(id(key), userId, accountId, merchant, service, money(amount),
                "CNY", cycle, nextCharge, status, "FIXED_SANDBOX", "取消后下一计费周期不再扣款", createdAt);
    }

    private static List<SandboxDataset.WealthProductSeed> wealthProducts(LocalDate baseline, Instant createdAt) {
        return List.of(
                product("WP-R1-001", "银盾稳享存款（模拟）", "DEPOSIT", "R1", "1000", "100", "模拟年化展示 1.80%，不构成承诺", 90, "ON_SALE", baseline, createdAt),
                product("WP-R2-001", "银盾短债优选（模拟）", "BOND", "R2", "1000", "100", "模拟业绩比较基准 2.20%-2.80%", 180, "ON_SALE", baseline, createdAt),
                product("WP-R2-002", "银盾安盈一年（模拟）", "BOND", "R2", "5000", "1000", "模拟业绩比较基准 2.50%-3.10%", 365, "ON_SALE", baseline, createdAt),
                product("WP-R3-001", "银盾均衡组合（模拟）", "FUND", "R3", "1000", "100", "净值型模拟产品，收益随市场波动", 365, "ON_SALE", baseline, createdAt),
                product("WP-R3-002", "银盾绿色主题（模拟）", "FUND", "R3", "100", "100", "权益与债券混合模拟产品", 0, "ON_SALE", baseline, createdAt),
                product("WP-R4-001", "银盾成长精选（模拟）", "FUND", "R4", "1000", "100", "高波动模拟权益产品", 0, "ON_SALE", baseline, createdAt),
                product("WP-R5-001", "银盾结构观察（模拟）", "STRUCTURED", "R5", "50000", "10000", "复杂结构模拟产品，不保证本金", 730, "SUSPENDED", baseline, createdAt),
                product("WP-END-001", "银盾往期产品（模拟）", "BOND", "R2", "1000", "100", "已结束销售的模拟产品", 180, "ENDED", baseline.minusYears(1), createdAt));
    }

    private static SandboxDataset.WealthProductSeed product(
            String code, String name, String type, String risk, String min, String increment,
            String yieldText, int termDays, String status, LocalDate baseline, Instant createdAt) {
        LocalDate from = status.equals("ENDED") ? baseline.minusMonths(6) : baseline.minusDays(30);
        LocalDate to = status.equals("ENDED") ? baseline.minusDays(1) : baseline.plusYears(1);
        return new SandboxDataset.WealthProductSeed(id("product/" + code), code, name, type, risk, "CNY",
                money(min), money(increment), yieldText, termDays, status, from, to, createdAt);
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Sandbox dataset generation is disabled");
        }
        if (!StringUtils.hasText(properties.getDatasetVersion()) || properties.getBaselineDate() == null) {
            throw new IllegalStateException("Sandbox dataset version and baseline date are required");
        }
        if (!isBcrypt(properties.getCustomerPasswordHash()) || !isBcrypt(properties.getAdminPasswordHash())) {
            throw new IllegalStateException("Sandbox passwords must be precomputed BCrypt hashes");
        }
    }

    private static boolean isBcrypt(String value) {
        return StringUtils.hasText(value) && value.matches("^\\$2[ayb]\\$\\d{2}\\$.{53}$");
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(4, RoundingMode.UNNECESSARY);
    }

    private static UUID id(String key) {
        return UUID.nameUUIDFromBytes((NAMESPACE + key).getBytes(StandardCharsets.UTF_8));
    }

    private record GeneratedAccount(
            SandboxDataset.AccountSeed account, List<SandboxDataset.TransactionSeed> transactions) {}
}
