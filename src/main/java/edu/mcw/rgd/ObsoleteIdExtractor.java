package edu.mcw.rgd;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
public class ObsoleteIdExtractor extends BaseExtractor{

    Logger log = LogManager.getLogger("obsoleteIds");
    FtpFileExtractsDAO ftpDao = new FtpFileExtractsDAO();

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

    }

    public void run()throws Exception{
        generateObsoleteGeneIds();
        generateObsoleteAlleleIds();
        generateObsoleteStrainIds();
    }

    String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

    String checkNull(int val) {
        return val<=0 ? "" : Integer.toString(val);
    }

    String generateObsoleteStrainIds() throws Exception{

        String outputFileName = getExtractDir()+"/STRAINS_OBSOLETE_IDS.txt";
//        String outputFileName = "STRAINS_OBSOLETE_IDS.txt";
        log.info("generating file with obsolete ids for strains: "+outputFileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        List<ObsoleteStrainId> strainIds = ftpDao.getObsoleteIdsForStrains();

        final String HEADER_STRAIN = """
        # RGD-PIPELINE: ftp-file-extracts
        # MODULE: obsolete-ids-version-1.1
        # GENERATED-ON: #DATE#
        # PURPOSE: list of RGD IDs for Strains that have been retired or withdrawn from RGD database
        # CONTACT: rgd.data@mcw.edu
        # FORMAT: tab delimited text
        #
        ### As of Sep 5, 2023 added column DATE_DISCONTINUED.
        ### As of #DATE# generated obsolete RGD IDs for strains.
        #
        #COLUMN INFORMATION:
        #1   SPECIES              name of the species
        #2   OLD_STRAIN_RGD_ID    old strain RGD ID
        #3   OLD_STRAIN_SYMBOL    old strain symbol
        #4   OLD_STRAIN_STATUS    old strain status
        #5   NEW_STRAIN_RGD_ID    new strain RGD ID (if any)
        #6   NEW_STRAIN_SYMBOL    new strain symbol (if any)
        #7   NEW_STRAIN_STATUS    new strain status (if any)
        #8   DATE_DISCONTINUED    date the strain has been discontinued
        #
        SPECIES\tOLD_STRAIN_RGD_ID\tOLD_STRAIN_SYMBOL\tOLD_STRAIN_STATUS\tNEW_STRAIN_RGD_ID\tNEW_STRAIN_SYMBOL\tNEW_STRAIN_STATUS\tDATE_DISCONTINUED
        """;
        String header = HEADER_STRAIN.replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.write(header);

        for(ObsoleteStrainId id: strainIds){
            writer
                    .append(id.species)
                    .append('\t')
                    .append(checkNull(checkNull(id.oldStrainRgdId)))
                    .append('\t')
                    .append(checkNull(id.oldStrainSymbol))
                    .append('\t')
                    .append(checkNull(id.oldStrainStatus))
                    .append('\t')


                    .append(checkNull(id.newStrainRgdId))
                    .append('\t')
                    .append(checkNull(id.newStrainSymbol))
                    .append('\t')
                    .append(checkNull(id.newStrainStatus))
                    .append('\t')
                    .append(checkNull(id.dateObsoleted))
                    .append('\n');
        }
        writer.close();
        log.info("The number of generated obsolete strain ids are: "+strainIds.size());
        return outputFileName;
    }

    String generateObsoleteAlleleIds() throws Exception{

        String outputFileName = getExtractDir()+"/ALLELES_OBSOLETE_IDS.txt";
//        String outputFileName = "Alleles_OBSOLETE_IDS.txt";
        log.info("generating file with obsolete ids for alleles: "+outputFileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        List<ObsoleteId>alleleIds = ftpDao.getObsoleteIdsForAlleles();

        final String HEADER_ALLELES = """
        # RGD-PIPELINE: ftp-file-extracts
        # MODULE: obsolete-ids-version-1.1
        # GENERATED-ON: #DATE#
        # PURPOSE: list of RGD IDs for genes that have been retired or withdrawn from RGD database
        # CONTACT: rgd.data@mcw.edu
        # FORMAT: tab delimited text
        #
        ### As of Sep 5, 2023 added column DATE_DISCONTINUED.
        ### As of #DATE# generated obsolete RGD IDs for alleles.
        #
        #COLUMN INFORMATION:
        #1   SPECIES              name of the species
        #2   OLD_ALLELE_RGD_ID    old allele RGD ID
        #3   OLD_ALLELE_SYMBOL    old allele symbol
        #4   OLD_ALLELE_STATUS    old allele status
        #5   OLD_ALLELE_TYPE      old allele type
        #6   NEW_ALLELE_RGD_ID    new allele RGD ID (if any)
        #7   NEW_ALLELE_SYMBOL    new allele symbol (if any)
        #8   NEW_ALLELE_STATUS    old allele status (if any)
        #9   NEW_ALLELE_TYPE      new allele type (if any)
        #10  DATE_DISCONTINUED    date the gene has been discontinued
        #
        SPECIES\tOLD_ALLELE_RGD_ID\tOLD_ALLELE_SYMBOL\tOLD_ALLELE_STATUS\tOLD_ALLELE_TYPE\tNEW_ALLELE_RGD_ID\tNEW_ALLELE_SYMBOL\tNEW_ALLELE_STATUS\tNEW_ALLELE_TYPE\tDATE_DISCONTINUED
        """;

        String header = HEADER_ALLELES.replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.write(header);

        for( ObsoleteId id: alleleIds) {
            writer
                    .append(id.species)
                    .append('\t')

                    .append(checkNull(checkNull(id.oldGeneRgdId)))
                    .append('\t')
                    .append(checkNull(id.oldGeneSymbol))
                    .append('\t')
                    .append(checkNull(id.oldGeneStatus))
                    .append('\t')
                    .append(checkNull(id.oldGeneType))
                    .append('\t')

                    .append(checkNull(id.newGeneRgdId))
                    .append('\t')
                    .append(checkNull(id.newGeneSymbol))
                    .append('\t')
                    .append(checkNull(id.newGeneStatus))
                    .append('\t')
                    .append(checkNull(id.newGeneType))
                    .append('\t')
                    .append(checkNull(id.dateObsoleted))
                    .append('\n');
        }
        writer.close();
        log.info("The number of generated obsolete allele ids are: "+alleleIds.size());
        return outputFileName;
    }

    String generateObsoleteGeneIds() throws Exception{

        String outputFileName = getExtractDir()+"/GENES_OBSOLETE_IDS.txt";
//        String outputFileName = "GENES_OBSOLETE_IDS.txt";
        log.info("generating file with obsolete ids for genes: "+outputFileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        List<ObsoleteId>geneIds = ftpDao.getObsoleteIdsForGenes();

        final String HEADER_GENE = """
        # RGD-PIPELINE: ftp-file-extracts
        # MODULE: obsolete-ids-version-1.1
        # GENERATED-ON: #DATE#
        # PURPOSE: list of RGD IDs for genes that have been retired or withdrawn from RGD database
        # CONTACT: rgd.data@mcw.edu
        # FORMAT: tab delimited text
        #
        ### As of Sep 5, 2023 added column DATE_DISCONTINUED.
        ### As of #DATE# generated obsolete RGD IDs for genes.
        #
        #COLUMN INFORMATION:
        #1   SPECIES              name of the species
        #2   OLD_GENE_RGD_ID      old gene RGD ID
        #3   OLD_GENE_SYMBOL      old gene symbol
        #4   OLD_GENE_STATUS      old gene status
        #5   OLD_GENE_TYPE        old gene type
        #6   NEW_GENE_RGD_ID      new gene RGD ID (if any)
        #7   NEW_GENE_SYMBOL      new gene symbol (if any)
        #8   NEW_GENE_STATUS      old gene status (if any)
        #9   NEW_GENE_TYPE        new gene type (if any)
        #10  DATE_DISCONTINUED    date the gene has been discontinued
        #
        SPECIES\tOLD_GENE_RGD_ID\tOLD_GENE_SYMBOL\tOLD_GENE_STATUS\tOLD_GENE_TYPE\tNEW_GENE_RGD_ID\tNEW_GENE_SYMBOL\tNEW_GENE_STATUS\tNEW_GENE_TYPE\tDATE_DISCONTINUED
        """;


        String header = HEADER_GENE.replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.write(header);

        for( ObsoleteId id: geneIds) {
            writer
                    .append(id.species)
                    .append('\t')

                    .append(checkNull(checkNull(id.oldGeneRgdId)))
                    .append('\t')
                    .append(checkNull(id.oldGeneSymbol))
                    .append('\t')
                    .append(checkNull(id.oldGeneStatus))
                    .append('\t')
                    .append(checkNull(id.oldGeneType))
                    .append('\t')

                    .append(checkNull(id.newGeneRgdId))
                    .append('\t')
                    .append(checkNull(id.newGeneSymbol))
                    .append('\t')
                    .append(checkNull(id.newGeneStatus))
                    .append('\t')
                    .append(checkNull(id.newGeneType))
                    .append('\t')
                    .append(checkNull(id.dateObsoleted))
                    .append('\n');
        }
        writer.close();
        log.info("The number of generated obsolete gene ids are: "+geneIds.size());
        return outputFileName;
    }

}
