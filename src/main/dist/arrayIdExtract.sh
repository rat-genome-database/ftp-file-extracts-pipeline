# export array ids for genes
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== ARRAY IDS ... ==="
$APPHOME/run.sh -array_ids
echo "=== ARRAY IDS OK ==="
echo ""
