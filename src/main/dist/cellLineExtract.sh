# exports cell lines

# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== CELL_LINES ... ==="
$APPHOME/run.sh -cell_lines
echo "=== CELL LINES OK ==="
echo ""