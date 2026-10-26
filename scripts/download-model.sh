#!/usr/bin/env bash
# Obtiene SPECTER2 base (ONNX) y su tokenizer en src/main/resources/models/specter2-base/.
# El repositorio de Hugging Face solo publica pesos PyTorch, así que se descarga la revisión
# fijada y se exporta a ONNX en un entorno virtual temporal (requiere python3 y ~2 GB libres).
# Revisión y versiones fijadas para que los embeddings sean reproducibles (constitución P12).
set -euo pipefail

MODEL="allenai/specter2_base"
REVISION="${MODEL_REVISION:-3447645e1def9117997203454fa4495937bfbd83}"
DEST="$(cd "$(dirname "$0")/.." && pwd)/src/main/resources/models/specter2-base"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

python3 -m venv "$WORK/venv"
"$WORK/venv/bin/pip" install -q --extra-index-url https://download.pytorch.org/whl/cpu \
  "optimum==2.1.0" "optimum-onnx[onnxruntime]==0.1.0" "torch==2.14.1" \
  "transformers==4.57.6" "onnx==1.23.2" "onnxruntime==1.31.0"

echo "Descargando ${MODEL}@${REVISION}..."
"$WORK/venv/bin/python" -c "
from huggingface_hub import snapshot_download
snapshot_download('${MODEL}', revision='${REVISION}', local_dir='${WORK}/src')"

echo "Exportando a ONNX..."
"$WORK/venv/bin/optimum-cli" export onnx --model "$WORK/src" --task feature-extraction "$WORK/out"

mkdir -p "$DEST"
cp "$WORK"/out/{model.onnx,tokenizer.json,tokenizer_config.json,config.json} "$DEST/"
echo "Modelo en ${DEST}"
