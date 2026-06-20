CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    txn_id          VARCHAR(36)   NOT NULL,
    user_id         VARCHAR(100)  NOT NULL,
    amount          NUMERIC(15,2) NOT NULL,
    sender_vpa      VARCHAR(200),
    receiver_vpa    VARCHAR(200),
    channel         VARCHAR(20),
    decision        VARCHAR(10)   NOT NULL,
    triggered_rule  VARCHAR(100),
    reason          TEXT,
    rules_evaluated INT           NOT NULL DEFAULT 0,
    txn_count       INT,
    total_amount    NUMERIC(15,2),
    decided_at      TIMESTAMP     NOT NULL,
    overridden      BOOLEAN       DEFAULT FALSE,
    override_reason TEXT,
    created_at      TIMESTAMP     DEFAULT NOW()
);

CREATE INDEX idx_audit_txn_id     ON audit_log(txn_id);
CREATE INDEX idx_audit_user_id    ON audit_log(user_id);
CREATE INDEX idx_audit_decided_at ON audit_log(decided_at);
CREATE INDEX idx_audit_decision   ON audit_log(decision);
CREATE INDEX idx_audit_rule       ON audit_log(triggered_rule);
