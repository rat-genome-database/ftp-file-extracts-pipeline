# exports protein-protein interactions

# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== INTERACTIONS ... ==="
$APPHOME/run.sh -interactions
echo "=== INTERACTIONS OK ==="
echo ""