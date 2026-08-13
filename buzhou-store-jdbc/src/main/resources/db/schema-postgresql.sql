CREATE TABLE IF NOT EXISTS buzhou_message (
    id                VARCHAR(64)  PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    turn_seq          INT          NOT NULL,
    seq_in_turn       INT          NOT NULL,
    role              VARCHAR(16)  NOT NULL,
    content           TEXT,
    tool_calls        TEXT,
    tool_call_id      VARCHAR(64),
    reasoning_content TEXT,
    reasoning_signature VARCHAR(512),
    metadata          TEXT,
    created_at        TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_msg_session_order ON buzhou_message (session_id, turn_seq, seq_in_turn);

CREATE TABLE IF NOT EXISTS buzhou_summary (
    id                BIGSERIAL PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    version           BIGINT       NOT NULL,
    sections          TEXT         NOT NULL,
    token_estimate    INT          NOT NULL,
    created_at        TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_summary_session_version ON buzhou_summary (session_id, version);

CREATE TABLE IF NOT EXISTS buzhou_session_state (
    session_id        VARCHAR(128) NOT NULL,
    state_key         VARCHAR(256) NOT NULL,
    state_value       TEXT,
    producer          VARCHAR(128) NOT NULL,
    created_turn      INT          NOT NULL,
    ttl_turns         INT,
    updated_at        TIMESTAMP    NOT NULL,
    PRIMARY KEY (session_id, state_key)
);

CREATE TABLE IF NOT EXISTS buzhou_session_lease (
    session_id        VARCHAR(128) PRIMARY KEY,
    owner_id          VARCHAR(128) NOT NULL,
    fencing_token     BIGINT       NOT NULL,
    acquired_at       TIMESTAMP    NOT NULL,
    expires_at        TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS buzhou_span (
    span_id           VARCHAR(64)  PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    turn_seq          INT,
    parent_id         VARCHAR(64),
    kind              VARCHAR(32)  NOT NULL,
    name              VARCHAR(256) NOT NULL,
    started_at        TIMESTAMP    NOT NULL,
    ended_at          TIMESTAMP,
    status            VARCHAR(16)  NOT NULL,
    attributes        TEXT
);
CREATE INDEX IF NOT EXISTS idx_span_session ON buzhou_span (session_id, turn_seq);

CREATE TABLE IF NOT EXISTS buzhou_event (
    event_id          VARCHAR(64)  PRIMARY KEY,
    span_id           VARCHAR(64)  NOT NULL,
    session_id        VARCHAR(128) NOT NULL,
    kind              VARCHAR(32)  NOT NULL,
    payload           TEXT,
    created_at        TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_event_session ON buzhou_event (session_id, created_at);

CREATE TABLE IF NOT EXISTS buzhou_injection_snapshot (
    id                BIGSERIAL PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    turn_seq          INT          NOT NULL,
    messages          TEXT         NOT NULL,
    budget_detail     TEXT,
    created_at        TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_snapshot_session_turn ON buzhou_injection_snapshot (session_id, turn_seq);

CREATE TABLE IF NOT EXISTS buzhou_run_registry (
    session_id           VARCHAR(128) PRIMARY KEY,
    app_id               VARCHAR(128) NOT NULL,
    agent_name           VARCHAR(128) NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    current_turn         INT          NOT NULL,
    last_completed_turn  INT          NOT NULL,
    owner_id             VARCHAR(128),
    updated_at           TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS buzhou_tool_call_log (
    session_id   VARCHAR(128) NOT NULL,
    tool_call_id VARCHAR(64)  NOT NULL,
    tool_name    VARCHAR(128) NOT NULL,
    args_hash    VARCHAR(128) NOT NULL,
    outcome      VARCHAR(24)  NOT NULL,
    result       TEXT,
    occurred_at  TIMESTAMP    NOT NULL,
    PRIMARY KEY (session_id, tool_call_id)
);
