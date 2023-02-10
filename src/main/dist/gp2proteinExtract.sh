set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== GP2PROTEIN ... ==="
$APPHOME/run.sh -gp2protein
echo "=== GP2PROTEIN OK ==="
echo ""