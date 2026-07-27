# extract annotations for species by ontology
#
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== ANNOTATIONS ... ==="
ANNOTDIR=$APPHOME/data/annotations/with_terms
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAILLIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAILLIST="mtutaj@mcw.edu jrsmith@mcw.edu"
fi

$APPHOME/run.sh -annotations

echo "=== ANNOTATIONS OK ==="
echo ""
