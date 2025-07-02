CREATE TABLE api (
    id BIGINT PRIMARY KEY,
    name VARCHAR
);

CREATE TABLE credencial (
    id BIGINT PRIMARY KEY,
    api_id BIGINT NOT NULL,

    CONSTRAINT fk_api
        FOREIGN KEY (api_id)
        REFERENCES api(id)
        ON DELETE CASCADE
);

CREATE TABLE usina (
    id BIGINT PRIMARY KEY,
    credencial_id BIGINT NOT NULL,
    priority VARCHAR DEFAULT 'NORMAL',
    update_attempts INT DEFAULT 0,
    updated BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_credencial
        FOREIGN KEY (credencial_id)
        REFERENCES credencial(id)
        ON DELETE CASCADE
);