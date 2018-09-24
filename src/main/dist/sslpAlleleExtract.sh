# analyzes rat allele sslps and generates public ftp files SSLPS_ALLELES_RAT
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== SSLP ALLELES ... ==="
$APPHOME/run.sh -sslp_alleles
echo "=== SSLP ALLELES OK ==="
echo ""