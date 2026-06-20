CREATE TABLE fraud_rules (
    id              BIGSERIAL PRIMARY KEY,
    rule_name       VARCHAR(100)  NOT NULL UNIQUE,
    description     VARCHAR(500),
    rule_type       VARCHAR(50)   NOT NULL,
    threshold       NUMERIC(15,2) NOT NULL,
    window_seconds  INT           NOT NULL DEFAULT 0,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    priority        INT           NOT NULL DEFAULT 100,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

INSERT INTO fraud_rules (rule_name, description, rule_type, threshold, window_seconds, is_active, priority) VALUES
('VELOCITY_ABUSE',         '> 5 transactions in 60s',          'VELOCITY', 5,     60,  TRUE, 1),
('LARGE_AMOUNT_NEW_PAYEE', '> 50000 to first-time payee',      'AMOUNT',   50000, 0,   TRUE, 2),
('NEW_DEVICE_HIGH_AMOUNT', 'New device + amount > 10000',       'DEVICE',   10000, 0,   TRUE, 3),
('FAILED_ATTEMPTS',        '> 3 failed attempts in 5 minutes',  'VELOCITY', 3,     300, TRUE, 4);
