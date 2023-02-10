# exports protein-protein interactions

# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== INTERACTIONS ... ==="
$APPHOME/run.sh -interactions
echo "=== INTERACTIONS OK ==="
echo ""