# analyzes rat allele markers and generates public ftp files MARKER_ALLELES_RAT
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== MARKER ALLELES ... ==="
$APPHOME/run.sh -marker_alleles
echo "=== MARKER ALLELES OK ==="
echo ""