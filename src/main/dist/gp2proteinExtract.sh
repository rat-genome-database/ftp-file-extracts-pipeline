set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== GP2PROTEIN ... ==="
$APPHOME/run.sh -gp2protein
echo "=== GP2PROTEIN OK ==="
echo ""