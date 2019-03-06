# extract files for submission to AGR (Alliance of Genome Resources)
#
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts
OUTDIR=$APPHOME/data/agr
TMPJSON=$OUTDIR/tmp.json
# note: python -m json.tool is formatting the json files in human readable pretty format

echo "=== AGR alleles for rat ... ==="
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/alleles/10116 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/alleles.10116.json
echo "=== AGR alleles for rat OK"
echo ""

echo "=== AGR variants for rat ... ==="
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/variants/10116 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/variants.10116.json
echo "=== AGR variants for rat OK"
echo ""

echo "=== AGR expression for rat and human ... ==="
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/expression/9606 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/expression.9606.json
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/expression/10116 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/expression.10116.json
echo "=== AGR expression for rat and human OK"
echo ""

echo "=== AGR phenotype files ... ==="
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/phenotypes/9606 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/phenotypes.9606.json
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/phenotypes/10116 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/phenotypes.10116.json
echo "=== AGR phenotype files OK ==="
echo ""

echo "=== AGR BGI files ... ==="
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/9606 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/bgi.9606.json
wget -nv http://pipelines.rgd.mcw.edu/rgdws/agr/10116 -O $TMPJSON
python -m json.tool $TMPJSON > $OUTDIR/bgi.10116.json
echo "=== AGR BGI files OK ==="

rm $OUTDIR/tmp.json

echo ""
