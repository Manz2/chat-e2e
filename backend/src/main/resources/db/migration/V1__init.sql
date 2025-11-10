-- V1__init.sql
-- Postgres + pgcrypto für gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- Users
-- =========================
CREATE TABLE app_user (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          handle          TEXT UNIQUE NOT NULL,
                          display_name    TEXT,
                          password_hash   TEXT NOT NULL,
                          two_fa_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================
-- Devices
-- - Ed25519 (Identität/Signaturen)
-- - X25519 (Schlüsseltausch / sealed boxes)
-- =========================
CREATE TABLE user_device (
                             id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id                UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                             device_name            TEXT,
                             platform               TEXT, -- 'ios' | 'android' | 'web' | 'desktop'
                             public_identity_key    TEXT NOT NULL,   -- Ed25519 public (Base64/PEM/Text)
                             public_kx_key          TEXT NOT NULL,   -- X25519 public  (Base64/PEM/Text)
                             identity_binding_sig   BYTEA,           -- optional: Sig, die beide Public Keys bindet
                             created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
                             last_seen_at           TIMESTAMPTZ,
                             revoked_at             TIMESTAMPTZ,
                             CONSTRAINT chk_platform CHECK (
                                 platform IS NULL OR platform IN ('ios','android','web','desktop')
                                 )
);

CREATE INDEX idx_user_device_user ON user_device(user_id);

-- Für saubere FKs auf (device_id, user_id)
CREATE UNIQUE INDEX uq_user_device_id_user ON user_device(id, user_id);

-- =========================
-- Conversations & Members
-- =========================
CREATE TABLE conversation (
                              id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              is_group   BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversation_member (
                                     conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
                                     user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                     role            TEXT DEFAULT 'member',
                                     PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conv_member_user ON conversation_member(user_id);

-- Optional granular: welche Geräte gehören (technisch) zur Conversation
CREATE TABLE conversation_member_device (
                                            conversation_id UUID NOT NULL,
                                            user_id         UUID NOT NULL,
                                            device_id       UUID NOT NULL,
                                            PRIMARY KEY (conversation_id, device_id),
    -- Gerätezuordnung nur für existierende Mitglieder
                                            FOREIGN KEY (conversation_id, user_id)
                                                REFERENCES conversation_member(conversation_id, user_id) ON DELETE CASCADE,
    -- Gerät muss zum User gehören
                                            FOREIGN KEY (device_id, user_id)
                                                REFERENCES user_device(id, user_id) ON DELETE CASCADE
);

CREATE INDEX idx_cmd_conversation ON conversation_member_device(conversation_id);
CREATE INDEX idx_cmd_device ON conversation_member_device(device_id);

-- =========================
-- Conversation Epochs (nur Meta)
-- Clients kennen/halten den CK je Epoche;
-- Server speichert KEINEN Klartext-Schlüssel.
-- =========================
CREATE TABLE conversation_epoch (
                                    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
                                    epoch           INTEGER NOT NULL,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    PRIMARY KEY (conversation_id, epoch)
);

-- Optional nützlich: letztes bekanntes Epoch-Meta
CREATE INDEX idx_conv_epoch_conv ON conversation_epoch(conversation_id, epoch);

-- =========================
-- Messages (logisch)
-- header: z.B. { "type":"text", "epoch":3, "counter":1042, "content_type":"text/plain" }
-- =========================
CREATE TABLE message_core (
                              id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              conversation_id  UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
                              sender_id        UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                              created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                              content_type     TEXT NOT NULL DEFAULT 'text/plain',
                              header           JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_msg_core_conv_created ON message_core(conversation_id, created_at);
CREATE INDEX idx_msg_core_sender ON message_core(sender_id);

-- =========================
-- Per-Device Delivery (Ciphertexts)
-- msg_header: minimal { "epoch": int, "counter": int }
-- ciphertext: AEAD (z. B. XChaCha20-Poly1305)
-- =========================
CREATE TABLE message_delivery (
                                  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  message_id           UUID NOT NULL REFERENCES message_core(id) ON DELETE CASCADE,
                                  recipient_device_id  UUID NOT NULL REFERENCES user_device(id) ON DELETE CASCADE,
                                  ciphertext           BYTEA NOT NULL,
                                  msg_header           JSONB,  -- { "epoch": ..., "counter": ... }
                                  delivered_at         TIMESTAMPTZ,
                                  read_at              TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_delivery_per_device ON message_delivery(message_id, recipient_device_id);
CREATE INDEX idx_delivery_device ON message_delivery(recipient_device_id);
CREATE INDEX idx_delivery_message ON message_delivery(message_id);


-- Messages nach Konversation und Zeit
CREATE INDEX idx_msg_core_conv_time
    ON message_core (conversation_id, created_at DESC, id);

-- Deliveries: schnelle Suche pro Device
CREATE INDEX idx_delivery_device_msg
    ON message_delivery (recipient_device_id, message_id);

-- Nur ungelesene (für schnelle Badges)
CREATE INDEX idx_delivery_device_read_null
    ON message_delivery (recipient_device_id)
    WHERE read_at IS NULL;

-- Optional: undelivered zuerst
CREATE INDEX idx_delivery_device_delivered_null
    ON message_delivery (recipient_device_id)
    WHERE delivered_at IS NULL;

-- Member-Devices für Fanout
CREATE INDEX idx_cmd_conv ON conversation_member_device (conversation_id);