# extract uniprot annotations for species in tab separated format
#
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

#-single_thread option is passed in to avoid running out of memory
echo "=== UNIPROT ANNOTATIONS ... ==="
$APPHOME/run.sh -uniprot_annotations -single_thread
echo "=== UNIPROT ANNOTATIONS OK ==="
echo ""
