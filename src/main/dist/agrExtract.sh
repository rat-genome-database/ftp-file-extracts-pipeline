# extract files for submission to AGR (Alliance of Genome Resources)
#
# abort the script if any of stages below will fail
set -e
APPNAME="ftp-file-extracts-pipeline"
APPHOME=/home/rgddata/pipelines/$APPNAME
OUTDIR=$APPHOME/data/agr
# note: python -m json.tool is formatting the json files in human readable pretty format

echo "=== AGR files ... ==="
# download all files in parallel
WGET="wget --no-check-certificate -nv"
AGR_URL="https://pipelines.rgd.mcw.edu/rgdws/agr"
$WGET ${AGR_URL}/affectedGenomicModels/10116 -O $OUTDIR/affectedGenomicModels_rat.json &
$WGET ${AGR_URL}/alleles/10116 -O $OUTDIR/alleles_rat.json &
$WGET ${AGR_URL}/variants/10116 -O $OUTDIR/variants_rat.json &
$WGET ${AGR_URL}/expression/9606 -O $OUTDIR/expression_human.json &
$WGET ${AGR_URL}/expression/10116 -O $OUTDIR/expression_rat.json &
$WGET ${AGR_URL}/phenotypes/9606 -O $OUTDIR/phenotypes_human.json &
$WGET ${AGR_URL}/phenotypes/10116 -O $OUTDIR/phenotypes_rat.json &
$WGET ${AGR_URL}/9606 -O $OUTDIR/genes_human.json &
$WGET ${AGR_URL}/10116 -O $OUTDIR/genes_rat.json &

# make the downloaded json files human readable
wait
python -m json.tool $OUTDIR/affectedGenomicModels_rat.json > $OUTDIR/affectedGenomicModels.10116.json &
python -m json.tool $OUTDIR/alleles_rat.json > $OUTDIR/alleles.10116.json &
python -m json.tool $OUTDIR/variants_rat.json > $OUTDIR/variants.10116.json &
python -m json.tool $OUTDIR/expression_human.json > $OUTDIR/expression.9606.json &
python -m json.tool $OUTDIR/expression_rat.json > $OUTDIR/expression.10116.json &
python -m json.tool $OUTDIR/phenotypes_human.json > $OUTDIR/phenotypes.9606.json &
python -m json.tool $OUTDIR/phenotypes_rat.json > $OUTDIR/phenotypes.10116.json &
python -m json.tool $OUTDIR/genes_human.json > $OUTDIR/genes.9606.json &
python -m json.tool $OUTDIR/genes_rat.json > $OUTDIR/genes.10116.json &

#remove tmp files
wait
rm $OUTDIR/affectedGenomicModels_rat.json
rm $OUTDIR/alleles_rat.json
rm $OUTDIR/variants_rat.json
rm $OUTDIR/expression_human.json
rm $OUTDIR/expression_rat.json
rm $OUTDIR/phenotypes_human.json
rm $OUTDIR/phenotypes_rat.json
rm $OUTDIR/genes_human.json
rm $OUTDIR/genes_rat.json

$APPHOME/run.sh -agr_htp

$APPHOME/run.sh -agr_ref

echo "=== AGR files OK"
echo ""
