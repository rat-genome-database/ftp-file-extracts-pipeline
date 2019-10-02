#!/bin/sh
. /etc/profile
source ~/.bashrc

MYDIR=/rgd/pipeline_for_radoslavov
cd $MYDIR

echo "---generate gene_reference_info.txt"
run_sql gene_ref.sql gene_reference_info.txt
if [ ! -s gene_reference_info.txt ]; then
  rm gene_reference_info.txt
fi

echo "---generate allele_reference_info.txt"
run_sql allele_ref.sql allele_reference_info.txt
if [ ! -s allele_reference_info.txt ]; then
  rm allele_reference_info.txt
fi

echo "---generate strain_reference_info.txt"
run_sql strain_ref.sql strain_reference_info.txt
if [ ! -s strain_reference_info.txt ]; then
  rm strain_reference_info.txt
fi

echo "---generate qtl_reference_info.txt"
run_sql qtl_ref.sql qtl_reference_info.txt
if [ ! -s qtl_reference_info.txt ]; then
  rm qtl_reference_info.txt
fi

echo "---generate rgdid2pmid.txt"
run_sql rgdid2pmid.sql rgdid2pmid.txt
if [ ! -s rgdid2pmid.txt ]; then
  rm rgdid2pmid.txt
fi

echo ""

echo "---copy to horan"
scp $MYDIR/*txt horan:/rgd/ftp/pub/data_release/UserReqFiles/Radoslavov/
echo "---copy to osler"
scp $MYDIR/*txt osler:/rgd/ftp/pub/data_release/UserReqFiles/Radoslavov/

echo "---OK---"
