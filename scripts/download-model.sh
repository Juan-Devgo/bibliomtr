#!/usr/bin/env bash
# Descarga el modelo all-MiniLM-L6-v2 (ONNX) y su tokenizer en src/main/resources/models/.
# Revisión fijada para que los embeddings sean reproducibles (constitución P12).
set -euo pipefail

MODEL="sentence-transformers/all-MiniLM-L6-v2"
REVISION="${MODEL_REVISION:-1110a243fdf4706b3f48f1d95db1a4f5529b4d41}"
DEST="$(cd "$(dirname "$0")/.." && pwd)/src/main/resources/models/all-MiniLM-L6-v2"
BASE="https://huggingface.co/${MODEL}/resolve/${REVISION}"

mkdir -p "$DEST"
for file in onnx/model.onnx tokenizer.json tokenizer_config.json config.json; do
  echo "Descargando ${file}..."
  curl -fsSL "${BASE}/${file}" -o "${DEST}/$(basename "$file")"
done
echo "Modelo en ${DEST}"
