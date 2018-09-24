# custom user request: for Dr Radoslavov
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== RADOSLAVOV ... ==="
$APPHOME/run.sh -radoslavov
echo "=== RADOSLAVOV OK ==="
echo ""