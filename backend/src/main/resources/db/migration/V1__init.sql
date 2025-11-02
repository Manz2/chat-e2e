-- V1__init.sql
CREATE TABLE app_user (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          handle TEXT UNIQUE NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

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

CREATE TABLE message (
                         id BIGSERIAL PRIMARY KEY,
                         conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
                         sender_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         ciphertext TEXT NOT NULL,
                         header JSONB
);
CREATE INDEX idx_msg_conv_created ON message(conversation_id, created_at);

