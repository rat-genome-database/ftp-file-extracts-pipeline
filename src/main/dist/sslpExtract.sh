# analyzes rat, mouse and human sslps and generates public ftp files SSLPS_RAT, SSLPS_MOUSE and SSLPS_HUMAN
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== SSLPS ... ==="
$APPHOME/run.sh -sslps
echo "=== SSLPS OK ==="
echo ""