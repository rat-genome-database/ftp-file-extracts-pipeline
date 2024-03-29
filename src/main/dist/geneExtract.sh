# export genes for all species into files GENES_RAT, GENES_MOUSE, GENES_HUMAN, ...
# also generates GENES_OBSOLETE_IDS.txt file
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== GENES ... ==="
$APPHOME/run.sh -genes
echo "=== GENES OK ==="
echo ""
