# analyze all strains and generate public ftp files STRAINS and STRAINS.xml
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== STRAINS ... ==="
$APPHOME/run.sh -strains -species=rat
echo "=== STRAINS OK ==="
echo ""