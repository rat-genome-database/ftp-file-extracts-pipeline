# analyzes rat allele markers and generates public ftp files MARKER_ALLELES_RAT
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== MARKER ALLELES ... ==="
$APPHOME/run.sh -marker_alleles
echo "=== MARKER ALLELES OK ==="
echo ""