# extract uniprot annotations for species in tab separated format
#
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

#-single_thread option is passed in to avoid running out of memory
echo "=== UNIPROT ANNOTATIONS ... ==="
$APPHOME/run.sh -uniprot_annotations -single_thread
echo "=== UNIPROT ANNOTATIONS OK ==="
echo ""
