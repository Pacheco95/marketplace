CREATE TABLE users
(
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    keycloak_subject    VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    profile_picture_url TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_keycloak_subject UNIQUE (keycloak_subject),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_keycloak_subject ON users (keycloak_subject);
CREATE INDEX idx_users_email ON users (email);
