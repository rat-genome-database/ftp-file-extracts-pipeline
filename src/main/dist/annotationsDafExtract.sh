# disease annotation file for AGR
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== DAF ANNOTATIONS ... ==="
$APPHOME/run.sh -daf_annotations
echo "=== DAF ANNOTATIONS OK ==="
echo ""
