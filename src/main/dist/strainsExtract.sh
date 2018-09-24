# analyze all strains and generate public ftp files STRAINS and STRAINS.xml
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== STRAINS ... ==="
$APPHOME/run.sh -strains -species=rat
echo "=== STRAINS OK ==="
echo ""