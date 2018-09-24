# generates FTP files for chinchilla
# abort the script if any of stages below will fail
set -e

APPHOME=/home/rgddata/pipelines/ftpFileExtracts
CHINDIR=data/chinchilla

echo "=== CHINCHILLA ... ==="

#chinchilla genes and orthologs (also RefSeq data in bed format)
$APPHOME/run.sh -chinchilla -species=chinchilla

#annotations in extended format for chinchilla
$APPHOME/run.sh -annotations -species=chinchilla -annotDir=$CHINDIR

#rename annotation files to be more user-friendly for naive users
cd $APPHOME
rm -vf $CHINDIR/chinchilla_genes_*
if [ -f $CHINDIR/chinchilla_terms_nbo ]; then
  mv $CHINDIR/chinchilla_terms_nbo $CHINDIR/chinchilla_genes_neuro_behavioral_ontology_annotations
else
  touch $CHINDIR/chinchilla_genes_neuro_behavioral_ontology_annotations
fi
mv $CHINDIR/chinchilla_terms_rdo $CHINDIR/chinchilla_genes_disease_annotations
mv $CHINDIR/chinchilla_terms_pw $CHINDIR/chinchilla_genes_pathway_annotations
mv $CHINDIR/chinchilla_terms_bp $CHINDIR/chinchilla_genes_GO_biological_process_annotations
mv $CHINDIR/chinchilla_terms_cc $CHINDIR/chinchilla_genes_GO_cellular_component_annotations
mv $CHINDIR/chinchilla_terms_mf $CHINDIR/chinchilla_genes_GO_molecular_function_annotations

echo "=== CHINCHILLA OK ==="
echo ""
