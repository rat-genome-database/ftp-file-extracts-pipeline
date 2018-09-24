# generates FTP files for RAT assembly comparison
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts

$APPHOME/run.sh -assembly_comparison -species=rat

