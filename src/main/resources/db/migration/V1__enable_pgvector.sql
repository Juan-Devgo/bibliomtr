-- Habilita pgvector. Se usa solo como almacenamiento de embeddings:
-- sus operadores de distancia están prohibidos (AGENTS.md).
CREATE EXTENSION IF NOT EXISTS vector;
