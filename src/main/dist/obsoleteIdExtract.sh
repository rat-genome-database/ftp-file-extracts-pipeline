# export obsolete Ids of genes, alleles and strains
# also generates GENES_OBSOLETE_IDS.txt, ALLELES_OBSOLETE_IDS.txt, STRAINS_OBSOLETE_IDS.txt files
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== Obsolete Ids ... ==="
$APPHOME/run.sh -obsolete
echo "copying the STRAINS_OBSOLETE_IDS.txt file to /data/www/strainFiles"
scp -p $APPHOME/private_data/STRAINS_OBSOLETE_IDS.txt /data/www/strainFiles
echo "copying the ALLELES_OBSOLETE_IDS.txt file to /data/www/strainFiles"
scp -p $APPHOME/private_data/ALLELES_OBSOLETE_IDS.txt /data/www/strainFiles
echo "=== Obsolete Ids OK ==="
echo ""
