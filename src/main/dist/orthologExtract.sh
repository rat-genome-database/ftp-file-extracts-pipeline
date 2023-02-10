# extract RGD orthologs
#
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== ORTHOLOGS ... ==="

# generate original orthologs: rat-mouse-human
$APPHOME/run.sh -orthologs
#generate orthologs for all species
$APPHOME/run.sh -orthologs2

echo "=== ORTHOLOGS OK ==="
echo ""

