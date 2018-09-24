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

#email orphaned terms (if present)
if [ -s "$ANNOTDIR/rattus_terms_orphaned" ]; then
    cat "$ANNOTDIR/rattus_terms_orphaned" >> $ANNOTDIR/terms_orphaned
fi
if [ -s "$ANNOTDIR/mus_terms_orphaned" ]; then
    cat "$ANNOTDIR/mus_terms_orphaned" >> $ANNOTDIR/terms_orphaned
fi
if [ -s "$ANNOTDIR/homo_terms_orphaned" ]; then
    cat "$ANNOTDIR/homo_terms_orphaned" >> $ANNOTDIR/terms_orphaned
fi

if [ -s "$ANNOTDIR/terms_orphaned" ]; then
  mailx -s "[$SERVER] orphaned annotations" $EMAILLIST < $ANNOTDIR/terms_orphaned
else
  echo "file $ANNOTDIR/terms_orphaned is empty"
fi

echo "=== ANNOTATIONS OK ==="
echo ""
