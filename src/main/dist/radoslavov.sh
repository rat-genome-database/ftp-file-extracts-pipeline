# custom user request: for Dr Radoslavov
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== RADOSLAVOV ... ==="
$APPHOME/run.sh -radoslavov
echo "=== RADOSLAVOV OK ==="
echo ""