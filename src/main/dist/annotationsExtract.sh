# extract annotations for species by ontology
#
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== ANNOTATIONS ... ==="
ANNOTDIR=$APPHOME/data/annotated_rgd_objects_by_ontology/with_terms
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAILLIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAILLIST=mtutaj@mcw.edu,jrsmith@mcw.edu,slaulederkind@mcw.edu
fi

$APPHOME/run.sh -annotations

echo "=== ANNOTATIONS OK ==="
echo ""
