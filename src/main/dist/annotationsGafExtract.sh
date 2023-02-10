# extract annotations for species by ontology in gaf 2.1 format
#
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== GAF ANNOTATIONS FOR AGR ... ==="
$APPHOME/run.sh -gaf_agr_annotations -species=human

echo "=== GAF ANNOTATIONS ... ==="
$APPHOME/run.sh -gaf_annotations
echo "=== GAF ANNOTATIONS OK ==="
echo ""