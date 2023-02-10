# analyzes rat, mouse and human markers and generates public ftp files SSLPS_RAT, SSLPS_MOUSE and SSLPS_HUMAN
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== MARKERS ... ==="
$APPHOME/run.sh -markers
echo "=== MARKERS OK ==="
echo ""