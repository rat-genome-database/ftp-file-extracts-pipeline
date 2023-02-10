# generates FTP files for RAT assembly comparison
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

$APPHOME/run.sh -assembly_comparison -species=rat

