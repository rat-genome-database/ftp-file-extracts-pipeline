# generate files with confirmed and predicted mirna targets for rat, mouse and human genes
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== MIRNA ... ==="
$APPHOME/run.sh -mirna_targets
echo "=== MIRNA OK ==="
echo ""
