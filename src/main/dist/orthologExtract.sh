# extract RGD orthologs
#
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== ORTHOLOGS ... ==="
$APPHOME/run.sh -orthologs
echo "=== ORTHOLOGS OK ==="
echo ""

