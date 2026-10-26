-- Artículos del corpus del Requerimiento 1 (spec 001, REQ-8, REQ-9, REQ-9a, REQ-10).
-- Una fila por PDF ingerido, identificado por el SHA-256 de su contenido para no duplicarlo al reiniciar
-- (AC-11). Los embeddings se guardan en columnas vector(768) de pgvector solo como almacenamiento: la
-- similitud se calcula en Java (AGENTS.md, constitución P3).

CREATE TABLE article (
    id                   bigserial PRIMARY KEY,
    file_name            text NOT NULL,
    file_sha256          varchar(64) NOT NULL UNIQUE,
    title                text NOT NULL,
    authors              text[] NOT NULL,
    -- NULL si no se detectó el abstract; el artículo queda como no seleccionable (REQ-10).
    abstract_text        text NULL,
    abstract_detected    boolean NOT NULL,
    -- SPECTER2 base sobre "título [SEP] abstract": vector [CLS] con norma L2 igual a 1 (REQ-9).
    specter2_embedding   vector(768) NULL,
    -- true si el texto superó 512 tokens y se truncó (REQ-9a); token_count es el total antes de truncar.
    specter2_truncated   boolean NOT NULL,
    specter2_token_count int NULL,
    -- nomic-embed-text vía Ollama sobre "clustering: <abstract>" (REQ-9).
    nomic_embedding      vector(768) NULL,
    ingested_at          timestamptz NOT NULL,
    CONSTRAINT article_abstract_detected_check
        CHECK (abstract_detected = (abstract_text IS NOT NULL)),
    CONSTRAINT article_embeddings_check
        CHECK (NOT abstract_detected OR (specter2_embedding IS NOT NULL AND nomic_embedding IS NOT NULL))
);
