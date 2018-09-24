# extract uniprot annotations for species in tab separated format
#
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== UNIPROT ANNOTATIONS ... ==="
$APPHOME/run.sh -uniprot_annotations
echo "=== UNIPROT ANNOTATIONS OK ==="
echo ""
