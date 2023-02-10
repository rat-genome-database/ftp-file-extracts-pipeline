# extract transcript sequences in fasta format, for all species
#
# abort the script if any of stages below will fail
set -e

APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME

echo "=== TRANSCRIPT SEQUENCES ... ==="
$APPHOME/run.sh -sequences -species=all
echo "=== TRANSCRIPT SEQUENCES OK ==="
echo ""
