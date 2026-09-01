package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payee")
public class Payee {

    public enum Status { ACTIVE, DISABLED }

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String name;
    @Column(name = "bank_name", nullable = false) private String bankName;
    @Column(name = "account_token", nullable = false) private String accountToken;
    @Column(name = "account_masked", nullable = false) private String accountMasked;
    @Column(name = "internal_account_id") private UUID internalAccountId;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(nullable = false) private boolean trusted;
    @Column(name = "last_used_at") private Instant lastUsedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected Payee() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getBankName() { return bankName; }
    public String getAccountMasked() { return accountMasked; }
    public String getCurrency() { return currency; }
    public Status getStatus() { return status; }
    public boolean isTrusted() { return trusted; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}
