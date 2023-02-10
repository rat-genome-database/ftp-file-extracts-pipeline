# export array ids for genes
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== ARRAY IDS ... ==="
$APPHOME/run.sh -array_ids
echo "=== ARRAY IDS OK ==="
echo ""
