# export genes for all species into files GENES_RAT, GENES_MOUSE, GENES_HUMAN, ...
# also generates GENES_OBSOLETE_IDS.txt file
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== GENES ... ==="
$APPHOME/run.sh -genes
$APPHOME/run.sh -obsolete
echo "=== GENES OK ==="
echo ""
