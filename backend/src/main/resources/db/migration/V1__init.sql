-- V1__init.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Users
CREATE TABLE app_user (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          handle TEXT UNIQUE NOT NULL,
                          display_name TEXT,
                          password_hash TEXT NOT NULL,
                          two_fa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- Devices
CREATE TABLE user_device (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                             device_name TEXT,
                             platform TEXT,                                   -- "ios","android","web","desktop"
                             public_identity_key TEXT NOT NULL,               -- IK_pub (Base64/PEM)
                             public_identity_key_id INTEGER,
                             public_identity_key_sig BYTEA,
                             key_curve TEXT DEFAULT 'x25519',
                             pqkem_public_key BYTEA,                          -- PQXDH (z. B. Kyber Public Key)
                             cert_payload JSONB,
                             cert_issued_at TIMESTAMPTZ,
                             cert_expires_at TIMESTAMPTZ,
                             cert_alg TEXT DEFAULT 'Ed25519',
                             cert_serial TEXT,
                             revoked_at TIMESTAMPTZ,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             last_seen_at TIMESTAMPTZ,
                             CONSTRAINT chk_key_curve CHECK (key_curve IN ('x25519','x448')),
                             CONSTRAINT chk_cert_times CHECK (
                                 cert_issued_at IS NULL
                                     OR cert_expires_at IS NULL
                                     OR cert_expires_at > cert_issued_at
                                 )
);
CREATE INDEX idx_user_device_user ON user_device(user_id);


-- Key type enum
CREATE TYPE user_key_type AS ENUM ('signed_prekey', 'one_time_prekey', 'pqkem_prekey');

-- Public PreKeys per device
CREATE TABLE user_key (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          device_id UUID NOT NULL REFERENCES user_device(id) ON DELETE CASCADE,
                          type user_key_type NOT NULL,
                          key_id INTEGER NOT NULL DEFAULT 0,
                          public_key TEXT NOT NULL,
                          signature BYTEA,
                          is_used BOOLEAN NOT NULL DEFAULT FALSE,
                          valid_until TIMESTAMPTZ,
                          claimed_at TIMESTAMPTZ,
                          kem_scheme TEXT,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_user_key_device_type ON user_key(device_id, type);
CREATE UNIQUE INDEX uq_user_key_dev_type_id ON user_key(device_id, type, key_id);
CREATE UNIQUE INDEX uq_single_spk_per_device ON user_key(device_id) WHERE type = 'signed_prekey';
CREATE INDEX idx_user_key_opk_available ON user_key(device_id, created_at)
    WHERE type = 'one_time_prekey' AND is_used = FALSE;

-- Conversations
CREATE TABLE conversation (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              is_group BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversation_member (
                                     conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
                                     user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                     role TEXT DEFAULT 'member',
                                     PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX idx_conv_member_user ON conversation_member(user_id);

-- Messages (logical)
CREATE TABLE message_core (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
                              sender_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              content_type TEXT NOT NULL DEFAULT 'text',
                              header JSONB
);
CREATE INDEX idx_msg_core_conv_created ON message_core(conversation_id, created_at);
CREATE INDEX idx_msg_core_sender ON message_core(sender_id);

-- Per-device ciphertext
CREATE TABLE message_delivery (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  message_id UUID NOT NULL REFERENCES message_core(id) ON DELETE CASCADE,
                                  recipient_device_id UUID NOT NULL REFERENCES user_device(id) ON DELETE CASCADE,
                                  ciphertext BYTEA NOT NULL,
                                  ratchet_header JSONB,
                                  delivered_at TIMESTAMPTZ,
                                  read_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_delivery_per_device ON message_delivery(message_id, recipient_device_id);
CREATE INDEX idx_delivery_device ON message_delivery(recipient_device_id);
CREATE INDEX idx_delivery_message ON message_delivery(message_id);
