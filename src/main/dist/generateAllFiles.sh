. /etc/profile
APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

# abort the script if any of stages below will fail
set -e

$APPHOME/agrExtract.sh
$APPHOME/arrayIdExtract.sh
$APPHOME/radoslavov.sh
$APPHOME/markerAlleleExtract.sh
$APPHOME/markerExtract.sh
$APPHOME/qtlExtract.sh
$APPHOME/geneExtract.sh
$APPHOME/orthologExtract.sh
$APPHOME/strainsExtract.sh
$APPHOME/interactionsExtract.sh
$APPHOME/cellLineExtract.sh
$APPHOME/gp2proteinExtract.sh
$APPHOME/annotationsExtract.sh
$APPHOME/annotationsDafExtract.sh
$APPHOME/annotationsGafExtract.sh
$APPHOME/annotationsUniProtExtract.sh
$APPHOME/obsoleteIdExtract.sh

echo "copy all files to staging area (just in case the remaining parts of script fail)"
rsync -rt $APPHOME/data/ /home/rgddata/data_release

$APPHOME/chinchilla.sh

SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
mailx -s "[$SERVER] null value columns" mtutaj@mcw.edu < $APPHOME/logs/nullColumns.log

### March 2021: submission endpoint 'svn+ssh://rgddata@ext.geneontology.org/share/go/svn/trunk/gp2protein'
###      stopped working; apparently GO moved away from svn permanently and it is using git instead
#
#echo "postprocessing gp2protein files ..."
#ANNOTDIR=$APPHOME/data/annotated_rgd_objects_by_ontology
#scp -p /home/rgddata/data_release/gp2* /home/rgddata/pipelines/goc_annotation/goc_svn/trunk/gp2protein
#cd /home/rgddata/pipelines/goc_annotation/goc_svn/trunk/gp2protein
#if [ "$SERVER" == "REED" ]; then
#  svn commit -m "Weekly submission of gp2protein.rgd"
#fi

if [ "$SERVER" == "REED" ]; then
    $APPHOME/variantExtract.sh
fi

$APPHOME/mirnaTargetExtract.sh
$APPHOME/sequencesExtract.sh

echo "copy all files to staging area ..."
rsync -rt $APPHOME/data/ /home/rgddata/data_release

#original command: it was deleting too much, must be tested thoroughly
#rsync -rv --delete $APPHOME/data/ /home/rgddata/data_release

echo "=== DONE! ==="
