# generate files with confirmed and predicted mirna targets for rat, mouse and human genes
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== MIRNA ... ==="
$APPHOME/run.sh -mirna_targets
echo "=== MIRNA OK ==="
echo ""
