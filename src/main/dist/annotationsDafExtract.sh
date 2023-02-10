# disease annotation file for AGR
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== DAF ANNOTATIONS ... ==="
$APPHOME/run.sh -daf_annotations
echo "=== DAF ANNOTATIONS OK ==="
echo ""
