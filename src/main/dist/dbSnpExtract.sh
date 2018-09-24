# analyzes rat, mouse and human genes and DB_SNP data and generates public ftp files DB_SNP_RAT, DB_SNP_MOUSE and DB_SNP_HUMAN
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts
$APPHOME/run.sh -db_snps
