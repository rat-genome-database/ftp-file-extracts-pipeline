# extract transcript sequences in fasta format, for all species
#
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

echo "=== TRANSCRIPT SEQUENCES ... ==="
$APPHOME/run.sh -sequences -species=all
echo "=== TRANSCRIPT SEQUENCES OK ==="
echo ""
