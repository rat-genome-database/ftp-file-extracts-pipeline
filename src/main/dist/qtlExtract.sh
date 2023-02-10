# 1. analyzes all rat, mouse and human qtls and recalculates their sizes when necessary
# by using estimated qtl size (see AppCongigure.xml file)
# 2. all qtls are extracted and files QTLS_RAT, QTLS_MOUSE and QTLS_HUMAN are generated
# these files are then copied to RGD ftp staging area (/user/rgddata/data_release)
# 
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== QTLS ==="
$APPHOME/run.sh -qtls
echo "=== QTLS OK ==="
echo ""