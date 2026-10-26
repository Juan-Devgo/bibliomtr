-- Historial de mediciones JMH del Requerimiento 2 (spec 002, REQ-27).
-- Una fila por medición con su estado y los parámetros fijos de JMH (REQ-18),
-- y una fila por algoritmo con su resultado (ms ± scoreError) y sus parámetros propios (REQ-22).

CREATE TABLE benchmark_run (
    id                     bigserial PRIMARY KEY,
    started_at             timestamptz NOT NULL,
    finished_at            timestamptz NULL,
    status                 varchar(12) NOT NULL CHECK (status IN ('EN_CURSO', 'COMPLETADA', 'FALLIDA')),
    message                text NULL,
    forks                  int NOT NULL,
    warmup_iterations      int NOT NULL,
    warmup_seconds         int NOT NULL,
    measurement_iterations int NOT NULL,
    measurement_seconds    int NOT NULL,
    mode                   varchar(8) NOT NULL,
    time_unit              varchar(4) NOT NULL,
    bucket_size            int NOT NULL,
    trace_enabled          boolean NOT NULL,
    -- n: número de términos de la lista medida.
    term_count             int NOT NULL
);

CREATE TABLE benchmark_algorithm_result (
    id             bigserial PRIMARY KEY,
    run_id         bigint NOT NULL REFERENCES benchmark_run (id) ON DELETE CASCADE,
    algorithm      varchar(8) NOT NULL,
    score_ms       double precision NOT NULL,
    score_error_ms double precision NOT NULL,
    -- Parámetros propios: k (Counting), d y b (Radix), m (Bucket); NULL si no aplican.
    max_frequency  int NULL,
    max_digits     int NULL,
    radix_base     int NULL,
    bucket_count   int NULL,
    complexity     varchar(64) NOT NULL
);
