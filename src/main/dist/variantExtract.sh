# generate files with nonsynonymous variants for all rat strains
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== VARIANTS ... ==="
$APPHOME/run.sh -variants -species=rat
echo "=== VARIANTS OK ==="
echo ""
