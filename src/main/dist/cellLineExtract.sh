# exports cell lines

# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== CELL_LINES ... ==="
$APPHOME/run.sh -cell_lines
echo "=== CELL LINES OK ==="
echo ""