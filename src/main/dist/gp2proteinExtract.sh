# analyzes rat, mouse and human sslps and generates public ftp files SSLPS_RAT, SSLPS_MOUSE and SSLPS_HUMAN
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== GP2PROTEIN ... ==="
$APPHOME/run.sh -gp2protein
echo "=== GP2PROTEIN OK ==="
echo ""