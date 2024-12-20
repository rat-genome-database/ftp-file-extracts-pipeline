package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mtutaj
 * @since Nov 29, 2010
 */
public class GeneExtractor extends BaseExtractor {

    final String[] ALIAS_TYPES = {"old_gene_name", "old_gene_symbol"};

    final String HEADER_RAT_PART1 = """
    # RGD-PIPELINE: ftp-file-extracts
    # MODULE: genes  build 2024-06-24
    # GENERATED-ON: #DATE#
    # PURPOSE: information about active #SPECIES# genes extracted from RGD database
    # SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#
    # CONTACT: rgd.data@mcw.edu
    # FORMAT: tab delimited text
    # NOTES: multiple values in a single column are separated by ';'
    #
    ### Apr  1, 2011 RATMAP_IDs and RHDB_IDs are discontinued.
    ### Apr 15, 2011 GENE_REFSEQ_STATUS column is provided.
    ### Jul  1, 2011 fixed generation of CURATED_REF_PUBMED_IDs and UNCURATED_PUBMED_IDs
    ### Nov 23, 2011 no format changes (UniGene Ids are extracted from db in different way)
    ### Dec 19, 2011 fixed documentation in header to be consistent with column names
    ### Jul  6, 2012 added generation of file GENES_RAT_5.0
    ### Oct 23, 2012 obsoleted column 23 'UNCURATED_REF_MEDLINE_ID' - changed to '(UNUSED)'
    ### Aug 19, 2013 gene descriptions made consistent with gene report pages from RGD website
    ### Oct  2, 2014 genes files refactored:
    ###   GENES_RAT_5.0.txt and GENES_RAT_6.0.txt retired -- added new columns to GENES_RAT.txt to accommodate positions for Rnor_5.0 and Rnor_6.0.
    ### May 25, 2017 GENE_REFSEQ_STATUS is now published in column 23 for all species
    ###   during transition period, for rat, mouse and human, GENE_REFSEQ_STATUS will continue to be also published in columns 39, 41 and 42 respectively
    ### Nov 1, 2018  renamed columns: SSLP_RGD_ID => MARKER_RGD_ID, SSLP_SYMBOL => MARKER_SYMBOL
    ### Jun 17 2019  data sorted by RGD ID; files exported into species specific directories
    ### Mar 11 2020  added Ensembl map positions
    ### Jan 18 2021  discontinued column 27 UNIGENE ID
    ### Feb 12 2021  added export of positions on assembly mRatBN7.2; discontinued export of positions on assembly RGSCv3.1 (columns 6,12,13,14)
    ### Jan 25 2022  rat Ensembl positions exported for mRatBN7.2 assembly
    ### Apr 18 2022  added export of canonical proteins in column 27
    ### Mar 10 2023  no more 'protein_coding' gene types: 'protein-coding' used instead
    ### Mar 11 2024  added export of positions on assembly GRCr8
    ### Jun 24 2024  added export of positions on Peter Doris assemblies: SHR, SHRSP and WKY
    #
    #COLUMN INFORMATION:
    # (First 38 columns are in common between all species)
    #
    #1   GENE_RGD_ID	      the RGD_ID of the gene
    #2   SYMBOL             official gene symbol
    #3   NAME    	          gene name
    #4   GENE_DESC          gene description (if available)
    #5   CHROMOSOME_CELERA  chromosome for Celera assembly
    #6   CHROMOSOME_#REF1# chromosome for reference assembly #REF1#
    #7   CHROMOSOME_#REF2# chromosome for reference assembly #REF2#
    #8   FISH_BAND          fish band information
    #9   START_POS_CELERA   start position for Celera assembly
    #10  STOP_POS_CELERA    stop position for Celera assembly
    #11  STRAND_CELERA      strand information for Celera assembly
    #12  START_POS_#REF1#   start position for reference assembly #REF1#
    #13  STOP_POS_#REF1#    stop position for reference assembly #REF1#
    #14  STRAND_#REF1#      strand information for reference assembly #REF1#
    #15  START_POS_#REF2#   start position for reference assembly #REF2#
    #16  STOP_POS_#REF2#    stop position for reference assembly #REF2#
    #17  STRAND_#REF2#      strand information for reference assembly #REF2#
    #18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene
    #19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene
    #20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation
    #21  NCBI_GENE_ID           NCBI Gene ID
    #22  UNIPROT_ID             UniProtKB id(s)
    #23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)
    #24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)
    #25  TIGR_ID                TIGR ID(s)
    #26  GENBANK_PROTEIN        GenBank Protein ID(s)
    #27  CANONICAL_PROTEIN      UniProt canonical protein(s)
    #28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene
    #29  MARKER_SYMBOL          marker symbol
    #30  OLD_SYMBOL             old symbol alias(es)
    #31  OLD_NAME               old name alias(es)
    #32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene
    #33  QTL_SYMBOL             QTL symbol
    #34  NOMENCLATURE_STATUS    nomenclature status
    #35  SPLICE_RGD_ID          RGD_IDs for gene splices
    #36  SPLICE_SYMBOL          symbol for gene
    #37  GENE_TYPE              gene type
    #38  ENSEMBL_ID             Ensembl Gene ID
    #39  (UNUSED)               blank
    #40  CHROMOSOME_#REF3#      chromosome for #REF3# reference assembly
    #41  START_POS_#REF3#       start position for #REF3# reference assembly
    #42  STOP_POS_#REF3#        stop position for #REF3# reference assembly
    #43  STRAND_#REF3#          strand information for #REF3# reference assembly
    #44  CHROMOSOME_#REF4#      chromosome for #REF4# reference assembly
    #45  START_POS_#REF4#       start position for #REF4# reference assembly
    #46  STOP_POS_#REF4#        stop position for #REF4# reference assembly
    #47  STRAND_#REF4#          strand information for #REF4# reference assembly
    #48  CHROMOSOME_ENSEMBL     chromosome for mRatBN7.2 Ensembl assembly
    #49  START_POS_ENSEMBL      start position for mRatBN7.2 Ensembl assembly
    #50  STOP_POS_ENSEMBL       stop position for mRatBN7.2 Ensembl assembly
    #51  STRAND_ENSEMBL         strand information for mRatBN7.2 Ensembl assembly
    #52  CHROMOSOME_#REF5#      chromosome for GRCr8 NCBI assembly
    #53  START_POS_#REF5#       start position for GRCr8 NCBI assembly
    #54  STOP_POS_#REF5#        stop position for GRCr8 NCBI assembly
    #55  STRAND_#REF5#          strand information for GRCr8 NCBI assembly
    #56  CHROMOSOME_#REF6#      chromosome for UTH_Rnor_SHR_Utx assembly
    #57  START_POS_#REF6#       start position for UTH_Rnor_SHR_Utx assembly
    #58  STOP_POS_#REF6#        stop position for UTH_Rnor_SHR_Utx assembly
    #59  STRAND_#REF6#          strand information for UTH_Rnor_SHR_Utx assembly
    #60  CHROMOSOME_#REF7#      chromosome for UTH_Rnor_SHRSP_BbbUtx_1.0 assembly
    #61  START_POS_#REF7#       start position for UTH_Rnor_SHRSP_BbbUtx_1.0 assembly
    #62  STOP_POS_#REF7#        stop position for UTH_Rnor_SHRSP_BbbUtx_1.0 assembly
    #63  STRAND_#REF7#          strand information for UTH_Rnor_SHRSP_BbbUtx_1.0 assembly
    #64  CHROMOSOME_#REF8#      chromosome for UTH_Rnor_WKY_Bbb_1.0 assembly
    #65  START_POS_#REF8#       start position for UTH_Rnor_WKY_Bbb_1.0 assembly
    #66  STOP_POS_#REF8#        stop position for UTH_Rnor_WKY_Bbb_1.0 assembly
    #67  STRAND_#REF8#          strand information for UTH_Rnor_WKY_Bbb_1.0 assembly
    #
    """;

    final String HEADER_RAT_PART2 =
     "GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_CELERA\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_CELERA\tSTOP_POS_CELERA\tSTRAND_CELERA\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\tTIGR_ID\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\tSPLICE_RGD_ID\tSPLICE_SYMBOL\tGENE_TYPE\tENSEMBL_ID\t(UNUSED)\t"
    +"CHROMOSOME_#REF3#\tSTART_POS_#REF3#\tSTOP_POS_#REF3#\tSTRAND_#REF3#\t"
    +"CHROMOSOME_#REF4#\tSTART_POS_#REF4#\tSTOP_POS_#REF4#\tSTRAND_#REF4#\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL\t"
    +"CHROMOSOME_#REF5#\tSTART_POS_#REF5#\tSTOP_POS_#REF5#\tSTRAND_#REF5#\t"
    +"CHROMOSOME_#REF6#\tSTART_POS_#REF6#\tSTOP_POS_#REF6#\tSTRAND_#REF6#\t"
    +"CHROMOSOME_#REF7#\tSTART_POS_#REF7#\tSTOP_POS_#REF7#\tSTRAND_#REF7#\t"
    +"CHROMOSOME_#REF8#\tSTART_POS_#REF8#\tSTOP_POS_#REF8#\tSTRAND_#REF8#";

    final String HEADER_RAT = HEADER_RAT_PART1 + HEADER_RAT_PART2;

    final String HEADER_HUMAN =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Apr 15, 2011 GENE_REFSEQ_STATUS column is provided.\n"
    +"### Jul  1, 2011 fixed generation of CURATED_REF_PUBMED_IDs and UNCURATED_PUBMED_IDs\n"
    +"### Nov 23, 2011 no format changes (UniGene Ids are extracted from db in different way).\n"
    +"### Dec 19, 2011 fixed documentation in header to be consistent with column names.\n"
    +"### Oct 23, 2012 obsoleted column 23 'UNCURATED_REF_MEDLINE_ID' - changed to '(UNUSED)'.\n"
    +"### Aug 19, 2013 gene descriptions made consistent with gene report pages from RGD website.\n"
    +"### Oct  2, 2014 genes files refactored:\n"
    +"###   GENES_HUMAN_B38.txt retired -- added new columns to GENES_HUMAN.txt to accommodate positions for GRCh38.\n"
    +"### Feb 15, 2017 HPRD_IDs are discontinued for human genes.\n"
    +"### May 25, 2017 GENE_REFSEQ_STATUS is now published in column 23 for all species\n"
    +"###   during transition period, for rat, mouse and human, GENE_REFSEQ_STATUS will continue to be also published in columns 39, 41 and 42 respectively\n"
    +"### Nov 1, 2018  renamed columns: SSLP_RGD_ID => MARKER_RGD_ID, SSLP_SYMBOL => MARKER_SYMBOL\n"
    +"### Jun 17 2019  data sorted by RGD ID; files exported into species specific directories\n"
    +"### Mar 11 2020  added Ensembl map positions\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   CHROMOSOME_CELERA  chromosome for Celera assembly\n"
    +"#6   CHROMOSOME_#REF1# chromosome for reference assembly build #REF1#\n"
    +"#7   CHROMOSOME_#REF2# chromosome for reference assembly build #REF2#\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   START_POS_CELERA   start position for Celera assembly\n"
    +"#10  STOP_POS_CELERA    stop position for Celera assembly\n"
    +"#11  STRAND_CELERA      strand information for Celera assembly\n"
    +"#12  START_POS_#REF1#   start position for reference assembly build #REF1#\n"
    +"#13  STOP_POS_#REF1#    stop position for reference assembly build #REF1#\n"
    +"#14  STRAND_#REF1#      strand information for reference assembly build #REF1#\n"
    +"#15  START_POS_#REF2#   start position for reference assembly build #REF2#\n"
    +"#16  STOP_POS_#REF2#    stop position for reference assembly build #REF2#\n"
    +"#17  STRAND_#REF2#      strand information for reference assembly build #REF2#\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)\n"
    +"#36  (UNUSED)\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  HGNC_ID            Human Genome Nomenclature Committee ID\n"
    +"#40  (UNUSED)\n"
    +"#41  OMIM_ID            Online Mendelian Inheritance in Man ID\n"
    +"#42  (UNUSED)\n"
    +"#43  CHROMOSOME_#REF3#      chromosome for GRCh#REF3# reference assembly\n"
    +"#44  START_POS_#REF3#       start position for GRCh#REF3# reference assembly\n"
    +"#45  STOP_POS_#REF3#        stop position for GRCh#REF3# reference assembly\n"
    +"#46  STRAND_#REF3#          strand information for GRCh#REF3# reference assembly\n"
    +"#47  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#48  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#49  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#50  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_CELERA\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_CELERA\tSTOP_POS_CELERA\tSTRAND_CELERA\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\tHGNC_ID\t(UNUSED)\tOMIM_ID\t(UNUSED)\t"
    +"CHROMOSOME_#REF3#\tSTART_POS_#REF3#\tSTOP_POS_#REF3#\tSTRAND_#REF3#\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";

    final String HEADER_MOUSE =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Apr 15, 2011 GENE_REFSEQ_STATUS column is provided.\n"
    +"### Jul  1, 2011 fixed generation of CURATED_REF_PUBMED_IDs and UNCURATED_PUBMED_IDs\n"
    +"### Nov 23, 2011 no format changes (UniGene Ids are extracted from db in different way).\n"
    +"### Dec 19, 2011 fixed documentation in header to be consistent with column names.\n"
    +"### Oct 23, 2012 obsoleted column 23 'UNCURATED_REF_MEDLINE_ID' - changed to '(UNUSED)'.\n"
    +"### Aug 19, 2013 gene descriptions made consistent with gene report pages from RGD website.\n"
    +"### Oct  2, 2014 genes files refactored:\n"
    +"###   GENES_MOUSE_B36.txt retired -- added new columns to GENES_MOUSE.txt to accommodate positions for assembly build 36.\n"
    +"### May 25, 2017 GENE_REFSEQ_STATUS is now published in column 23 for all species\n"
    +"###   during transition period, for rat, mouse and human, GENE_REFSEQ_STATUS will continue to be also published in columns 39, 41 and 42 respectively\n"
    +"### Nov 1, 2018  renamed columns: SSLP_RGD_ID => MARKER_RGD_ID, SSLP_SYMBOL => MARKER_SYMBOL\n"
    +"### Jun 17 2019  data sorted by RGD ID; files exported into species specific directories\n"
    +"### Mar 11 2020  added Ensembl map positions\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Jan 19 2021  export positions on assembly GRCm39 instead of assembly MGSCv36\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   CHROMOSOME_CELERA  chromosome for Celera assembly\n"
    +"#6   CHROMOSOME_#REF1# chromosome for #REF1# assembly\n"
    +"#7   CHROMOSOME_#REF2# chromosome for #REF2# assembly\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   START_POS_CELERA   start position for Celera assembly\n"
    +"#10  STOP_POS_CELERA    stop position for Celera assembly\n"
    +"#11  STRAND_CELERA      strand information for Celera assembly\n"
    +"#12  START_POS_#REF1#   start position for #REF1# assembly\n"
    +"#13  STOP_POS_#REF1#    stop position for #REF1# assembly\n"
    +"#14  STRAND_#REF1#      strand information for #REF1# assembly\n"
    +"#15  START_POS_#REF2#   start position for #REF2# assembly\n"
    +"#16  STOP_POS_#REF2#    stop position for #REF2# assembly\n"
    +"#17  STRAND_#REF2#      strand information for #REF2# assembly\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)\n"
    +"#36  (UNUSED)\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  MGD_ID             MGD ID\n"
    +"#40  CM_POS             mouse cM map absolute position\n"
    +"#41  (UNUSED)\n"
    +"#42  CHROMOSOME_#REF3#      chromosome for #REF3# assembly\n"
    +"#43  START_POS_#REF3#       start position for #REF3# assembly\n"
    +"#44  STOP_POS_#REF3#        stop position for #REF3# assembly\n"
    +"#45  STRAND_#REF3#          strand information for #REF3# assembly\n"
    +"#46  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#47  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#48  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#49  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_CELERA\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_CELERA\tSTOP_POS_CELERA\tSTRAND_CELERA\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\tMGD_ID\tCM_POS\t(UNUSED)\t"
    +"CHROMOSOME_#REF3#\tSTART_POS_#REF3#\tSTOP_POS_#REF3#\tSTRAND_#REF3#\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";

    final String HEADER_CHINCHILLA =
    "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Mar 11 2020  added Ensembl map positions\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   (UNUSED)           blank\n"
    +"#6   CHROMOSOME_#REF1# chromosome for reference assembly build #REF1#\n"
    +"#7   (UNUSED)           blank\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   (UNUSED)           blank\n"
    +"#10  (UNUSED)           blank\n"
    +"#11  (UNUSED)           blank\n"
    +"#12  START_POS_#REF1#   start position for reference assembly build #REF1#\n"
    +"#13  STOP_POS_#REF1#    stop position for reference assembly build #REF1#\n"
    +"#14  STRAND_#REF1#      strand information for reference assembly build #REF1#\n"
    +"#15  (UNUSED)           blank\n"
    +"#16  (UNUSED)           blank\n"
    +"#17  (UNUSED)           blank\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)               blank\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)               blank\n"
    +"#36  (UNUSED)               blank\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#40  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#41  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#42  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\t(UNUSED)\tCHROMOSOME_#REF1#\t(UNUSED)\t"
    +"FISH_BAND\t(UNUSED)\t(UNUSED)\t(UNUSED)\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"(UNUSED)\t(UNUSED)\t(UNUSED)\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";

    final String HEADER_BONOBO =
    "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2023-05-19\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Mar 11 2020  added Ensembl map positions\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Jan 19 2021  added Mhudiblu_PPA_v0 assembly positions\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"### May 19 2023  added NHGRI_mPanPan1-v1.1-0.1.freeze_pri assembly positions\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   CHROMOSOME_mPanPan1 chromosome for reference assembly NHGRI_mPanPan1-v1.1-0.1.freeze_pri\n"
    +"#6   CHROMOSOME_#REF1#  chromosome for assembly #REF1#\n"
    +"#7   CHROMOSOME_#REF2#  chromosome for assembly #REF2#\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   START_POS_mPanPan1 start position for NHGRI_mPanPan1-v1.1-0.1.freeze_pri assembly\n"
    +"#10  STOP_POS_mPanPan1  stop position for NHGRI_mPanPan1-v1.1-0.1.freeze_pri assembly\n"
    +"#11  STRAND_mPanPan1    strand information for NHGRI_mPanPan1-v1.1-0.1.freeze_pri assembly\n"
    +"#12  START_POS_#REF1#   start position for #REF1# assembly\n"
    +"#13  STOP_POS_#REF1#    stop position for #REF1# assembly\n"
    +"#14  STRAND_#REF1#      strand information for #REF1# assembly\n"
    +"#15  START_POS_#REF2#   start position for #REF2# assembly\n"
    +"#16  STOP_POS_#REF2#    stop position for #REF2# assembly\n"
    +"#17  STRAND_#REF2#      strand information for #REF2# assembly\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)\n"
    +"#36  (UNUSED)\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#40  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#41  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#42  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_mPanPan1\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_mPanPan1\tSTOP_POS_mPanPan1\tSTRAND_mPanPan1\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";

    final String HEADER_SQUIRREL =
    "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Mar 11 2020  added Ensembl map positions\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   (UNUSED)           blank\n"
    +"#6   CHROMOSOME_#REF1# chromosome for reference assembly build #REF1#\n"
    +"#7   (UNUSED)           blank\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   (UNUSED)           blank\n"
    +"#10  (UNUSED)           blank\n"
    +"#11  (UNUSED)           blank\n"
    +"#12  START_POS_#REF1#   start position for reference assembly build #REF1#\n"
    +"#13  STOP_POS_#REF1#    stop position for reference assembly build #REF1#\n"
    +"#14  STRAND_#REF1#      strand information for reference assembly build #REF1#\n"
    +"#15  (UNUSED)           blank\n"
    +"#16  (UNUSED)           blank\n"
    +"#17  (UNUSED)           blank\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)               blank\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)               blank\n"
    +"#36  (UNUSED)               blank\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#40  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#41  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#42  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\t(UNUSED)\tCHROMOSOME_#REF1#\t(UNUSED)\t"
    +"FISH_BAND\t(UNUSED)\t(UNUSED)\t(UNUSED)\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"(UNUSED)\t(UNUSED)\t(UNUSED)\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";

    final String HEADER_VERVET =
    "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Mar 11 2020  added Ensembl map positions\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   (UNUSED)           blank\n"
    +"#6   CHROMOSOME_#REF1# chromosome for reference assembly build #REF1#\n"
    +"#7   (UNUSED)           blank\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   (UNUSED)           blank\n"
    +"#10  (UNUSED)           blank\n"
    +"#11  (UNUSED)           blank\n"
    +"#12  START_POS_#REF1#   start position for reference assembly build #REF1#\n"
    +"#13  STOP_POS_#REF1#    stop position for reference assembly build #REF1#\n"
    +"#14  STRAND_#REF1#      strand information for reference assembly build #REF1#\n"
    +"#15  (UNUSED)           blank\n"
    +"#16  (UNUSED)           blank\n"
    +"#17  (UNUSED)           blank\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)               blank\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)               blank\n"
    +"#36  (UNUSED)               blank\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#40  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#41  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#42  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\t(UNUSED)\tCHROMOSOME_#REF1#\t(UNUSED)\t"
    +"FISH_BAND\t(UNUSED)\t(UNUSED)\t(UNUSED)\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"(UNUSED)\t(UNUSED)\t(UNUSED)\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";


    final String HEADER_MOLERAT=
    "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Mar 11 2020  added Ensembl map positions\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   (UNUSED)           blank\n"
    +"#6   CHROMOSOME_#REF1# chromosome for reference assembly build #REF1#\n"
    +"#7   (UNUSED)           blank\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   (UNUSED)           blank\n"
    +"#10  (UNUSED)           blank\n"
    +"#11  (UNUSED)           blank\n"
    +"#12  START_POS_#REF1#   start position for reference assembly build #REF1#\n"
    +"#13  STOP_POS_#REF1#    stop position for reference assembly build #REF1#\n"
    +"#14  STRAND_#REF1#      strand information for reference assembly build #REF1#\n"
    +"#15  (UNUSED)           blank\n"
    +"#16  (UNUSED)           blank\n"
    +"#17  (UNUSED)           blank\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)               blank\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)               blank\n"
    +"#36  (UNUSED)               blank\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#40  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#41  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#42  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\t(UNUSED)\tCHROMOSOME_#REF1#\t(UNUSED)\t"
    +"FISH_BAND\t(UNUSED)\t(UNUSED)\t(UNUSED)\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"(UNUSED)\t(UNUSED)\t(UNUSED)\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";

    final String HEADER_PIG =
    "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Mar 11 2020  added Ensembl map positions and VGNC IDs\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   (UNUSED)           blank\n"
    +"#6   CHROMOSOME_#REF1# chromosome for reference assembly build #REF1#\n"
    +"#7   CHROMOSOME_#REF2# chromosome for reference assembly build #REF2#\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   (UNUSED)           blank\n"
    +"#10  (UNUSED)           blank\n"
    +"#11  (UNUSED)           blank\n"
    +"#12  START_POS_#REF1#   start position for reference assembly build #REF1#\n"
    +"#13  STOP_POS_#REF1#    stop position for reference assembly build #REF1#\n"
    +"#14  STRAND_#REF1#      strand information for reference assembly build #REF1#\n"
    +"#15  START_POS_#REF2#   start position for reference assembly build #REF2#\n"
    +"#16  STOP_POS_#REF2#    stop position for reference assembly build #REF2#\n"
    +"#17  STRAND_#REF2#      strand information for reference assembly build #REF2#\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)               blank\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)               blank\n"
    +"#36  (UNUSED)               blank\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  VGNC_ID                VGNC ID\n"
    +"#40  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#41  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#42  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#43  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\t(UNUSED)\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\t(UNUSED)\t(UNUSED)\t(UNUSED)\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\tVGNC_ID\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL";

    final String HEADER_DOG =
    "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes  build 2022-04-18\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# SPECIES: #TAXONOMY_NAME# (#SPECIES_LONGNAME#) NCBI:txid#TAXONID#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### Mar 11 2020  added Ensembl map positions and VGNC IDs\n"
    +"### Jan 18 2021  discontinued columns: UNIGENE_ID, TIGR_ID, SPLICE_RGD_ID, SPLICE_SYMBOL\n"
    +"### Mar 16 2021  added positions for assemblies: Dog10K_Boxer_Tasha, ROS_Cfam_1.0, UMICH_Zoey_3.1, UNSW_CanFamBas_1.0, UU_Cfam_GSD_1.0\n"
    +"### Apr 18 2022  added export of canonical proteins in column 27\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 38 columns are in common between all species)\n"
    +"#\n"
    +"#1   GENE_RGD_ID	      the RGD_ID of the gene\n"
    +"#2   SYMBOL             official gene symbol\n"
    +"#3   NAME    	          gene name\n"
    +"#4   GENE_DESC          gene description (if available)\n"
    +"#5   CHROMOSOME_#REF3# chromosome for #REF3# assembly\n"
    +"#6   CHROMOSOME_#REF1# chromosome for #REF1# assembly\n"
    +"#7   CHROMOSOME_#REF2# chromosome for #REF2# assembly\n"
    +"#8   FISH_BAND          fish band information\n"
    +"#9   START_POS_#REF3#   start position for #REF3# assembly\n"
    +"#10  STOP_POS_#REF3#    stop position for #REF3# assembly\n"
    +"#11  STRAND_#REF3#      strand information for #REF3# assembly\n"
    +"#12  START_POS_#REF1#   start position for #REF1# assembly\n"
    +"#13  STOP_POS_#REF1#    stop position for #REF1# assembly\n"
    +"#14  STRAND_#REF1#      strand information for #REF1# assembly\n"
    +"#15  START_POS_#REF2#   start position for #REF2# assembly\n"
    +"#16  STOP_POS_#REF2#    stop position for #REF2# assembly\n"
    +"#17  STRAND_#REF2#      strand information for #REF2# assembly\n"
    +"#18  CURATED_REF_RGD_ID     RGD_ID of paper(s) used to curate gene\n"
    +"#19  CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) used to curate gene\n"
    +"#20  UNCURATED_PUBMED_ID    PUBMED ids of papers associated with the gene at NCBI but not used for curation\n"
    +"#21  NCBI_GENE_ID           NCBI Gene ID\n"
    +"#22  UNIPROT_ID             UniProtKB id(s)\n"
    +"#23  GENE_REFSEQ_STATUS     gene RefSeq Status (from NCBI)\n"
    +"#24  GENBANK_NUCLEOTIDE     GenBank Nucleotide ID(s)\n"
    +"#25  (UNUSED)               blank\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  CANONICAL_PROTEIN      UniProt canonical protein(s)\n"
    +"#28  MARKER_RGD_ID          RGD_ID(s) of markers associated with given gene\n"
    +"#29  MARKER_SYMBOL          marker symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  (UNUSED)               blank\n"
    +"#36  (UNUSED)               blank\n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
    +"#39  VGNC_ID                VGNC ID\n"
    +"#40  CHROMOSOME_ENSEMBL     chromosome for primary Ensembl assembly\n"
    +"#41  START_POS_ENSEMBL      start position for primary Ensembl assembly\n"
    +"#42  STOP_POS_ENSEMBL       stop position for primary Ensembl assembly\n"
    +"#43  STRAND_ENSEMBL         strand information for primary Ensembl assembly\n"
    +"#44  CHROMOSOME_#REF4# chromosome for #REF4# assembly\n"
    +"#45  START_POS_#REF4#   start position for #REF4# assembly\n"
    +"#46  STOP_POS_#REF4#    stop position for #REF4# assembly\n"
    +"#47  STRAND_#REF4#      strand information for #REF4# assembly\n"
    +"#48  CHROMOSOME_#REF5# chromosome for #REF5# assembly\n"
    +"#49  START_POS_#REF5#   start position for #REF5# assembly\n"
    +"#50  STOP_POS_#REF5#    stop position for #REF5# assembly\n"
    +"#51  STRAND_#REF5#      strand information #REF5# for assembly\n"
    +"#52  CHROMOSOME_#REF6# chromosome for #REF6# assembly\n"
    +"#53  START_POS_#REF6#   start position for #REF6# assembly\n"
    +"#54  STOP_POS_#REF6#    stop position for #REF6# assembly\n"
    +"#55  STRAND_#REF6#      strand information for #REF6# assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_#REF3#\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_#REF3#\tSTOP_POS_#REF3#\tSTRAND_#REF3#\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\t(UNUSED)\t"
    +"GENBANK_PROTEIN\tCANONICAL_PROTEIN\tMARKER_RGD_ID\tMARKER_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\t(UNUSED)\t(UNUSED)\tGENE_TYPE\tENSEMBL_ID\tVGNC_ID\t"
    +"CHROMOSOME_ENSEMBL\tSTART_POS_ENSEMBL\tSTOP_POS_ENSEMBL\tSTRAND_ENSEMBL\t"
    +"CHROMOSOME_#REF4#\tSTART_POS_#REF4#\tSTOP_POS_#REF4#\tSTRAND_#REF4#\t"
    +"CHROMOSOME_#REF5#\tSTART_POS_#REF5#\tSTOP_POS_#REF5#\tSTRAND_#REF5#\t"
    +"CHROMOSOME_#REF6#\tSTART_POS_#REF6#\tSTOP_POS_#REF6#\tSTRAND_#REF6#";

    Logger log = LogManager.getLogger("gene");
    private java.util.Map<String,List<String>> mapKeys;
    private String fileNamePrefix;
    private java.util.Set<String> canonicalProteins;
    private CounterPool counters;

    public void run(SpeciesRecord si) throws Exception {

        final int speciesType = si.getSpeciesType();

        counters = new CounterPool();

        // generate regular file
        String outputFileName = generate(si);
        FtpFileExtractsManager.qcFileContent(outputFileName, "genes", speciesType);
    }

    String generate(final SpeciesRecord si) throws Exception {

        String speciesName = si.getSpeciesShortName();
        String outputFileName = getSpeciesSpecificExtractDir(si)+'/'+getFileNamePrefix()+speciesName.toUpperCase()+".txt";

        final FtpFileExtractsDAO dao = getDao();
        final int speciesType = si.getSpeciesType();
        final PrintWriter writer = new PrintWriter(outputFileName);

        canonicalProteins = dao.getCanonicalProteins(speciesType);

        final int ASSEMBLY_COUNT = 8;
        String[] assembly = new String[ASSEMBLY_COUNT];
        final int[] mapKey = new int[ASSEMBLY_COUNT];

        List<String> mapInfos = getMapKeys().get(speciesName.toLowerCase());
        for( int i=0; i<ASSEMBLY_COUNT; i++ ) {

            if( mapInfos.size() > i ) {
                String mapInfo = mapInfos.get(i);
                assembly[i] = mapInfo.substring(1+mapInfo.indexOf(' '));
                mapKey[i] = Integer.parseInt(mapInfo.substring(0, mapInfo.indexOf(' ')));
            } else {
                assembly[i] = null; mapKey[i] = 0;
            }
        }

        // prepare header common lines
        String headerLines = switch (speciesType) {
            case SpeciesType.HUMAN -> HEADER_HUMAN;
            case SpeciesType.MOUSE -> HEADER_MOUSE;
            case SpeciesType.RAT -> HEADER_RAT;
            case SpeciesType.CHINCHILLA -> HEADER_CHINCHILLA;
            case SpeciesType.BONOBO -> HEADER_BONOBO;
            case SpeciesType.DOG -> HEADER_DOG;
            case SpeciesType.SQUIRREL -> HEADER_SQUIRREL;
            case SpeciesType.VERVET -> HEADER_VERVET;
            case SpeciesType.NAKED_MOLE_RAT -> HEADER_MOLERAT;
            case SpeciesType.PIG -> HEADER_PIG;
            default -> null;
        };

        for( int i=0; i<ASSEMBLY_COUNT; i++ ) {
            if( assembly[i] != null ) {
                headerLines = headerLines.replace("#REF"+(i+1)+"#", assembly[i]);
            }
        }

        String taxonomyName = SpeciesType.getTaxonomicName(speciesType);
        String speciesLongName = SpeciesType.getGenebankCommonName(speciesType);
        String taxonId = Integer.toString(SpeciesType.getTaxonomicId(speciesType));
        headerLines = headerLines
            .replace("#SPECIES#", si.getSpeciesName())
            .replace("#DATE#", SpeciesRecord.getTodayDate())
            .replace("#TAXONOMY_NAME#", taxonomyName)
            .replace("#SPECIES_LONGNAME#", speciesLongName)
            .replace("#TAXONID#", taxonId);

        writer.println(headerLines);


        // load all curated pubmed ids
        final PubmedIdsManager pubmedIdsManager = new PubmedIdsManager();
        pubmedIdsManager.loadCuratedPubmedIds(dao, RgdId.OBJECT_KEY_GENES, si.getSpeciesType());

        List<GeneExtractRecord> geneRecords = loadGeneRecords(speciesType);

        final Map<Integer, String> lineMap = new ConcurrentHashMap<>(geneRecords.size());

        geneRecords.parallelStream().forEach( rec -> {

            try {
                for (MapData md : dao.getMapData(rec.getRgdId())) {
                    // skip maps with empty chromosomes
                    if (md.getChromosome() == null || md.getChromosome().trim().length() == 0)
                        continue;

                    if (md.getMapKey() == mapKey[0]) {
                        rec.assembly1Map.add(md);
                    } else if (md.getMapKey() == mapKey[1]) {
                        rec.assembly2Map.add(md);
                    } else if (md.getMapKey() == mapKey[2]) {
                        rec.assembly3Map.add(md);
                    } else if (md.getMapKey() == mapKey[3]) {
                        rec.assembly4Map.add(md);
                    } else if (md.getMapKey() == mapKey[4]) {
                        rec.assembly5Map.add(md);
                    } else if (md.getMapKey() == mapKey[5]) {
                        rec.assembly6Map.add(md);
                    } else if (md.getMapKey() == mapKey[6]) {
                        rec.assembly7Map.add(md);
                    } else if (md.getMapKey() == mapKey[7]) {
                        rec.assembly8Map.add(md);
                    } else if (md.getMapKey() == si.getCeleraAssemblyMapKey()) {
                        rec.celeraMap.add(md);
                    } else if (md.getMapKey() == si.getEnsemblAssemblyMapKey()) {
                        rec.ensemblMap.add(md);
                    } else if (md.getMapKey() == si.getCytoMapKey()) {
                        rec.cytoMap.add(md);
                    } else if (md.getMapKey() == si.getCmMapKey()) {
                        rec.setAbsPos(md.getAbsPosition());
                    }

                    // hack for dog
                    if( speciesType==SpeciesType.DOG ) {
                        if (md.getMapKey() == si.getCeleraAssemblyMapKey()) {
                            rec.celeraMap.add(md);
                        }
                    }
                }

                rec.addCuratedRefRgdIds(dao.getCuratedRefs(rec.getRgdId()));
                for (String curatedPubmedId : pubmedIdsManager.getCuratedPubmedIds(rec.getRgdId())) {
                    rec.addCuratedPubmedIds(curatedPubmedId);
                }

                for (XdbId xdbId : getXdbIdList(rec.getRgdId())) {
                    switch (xdbId.getXdbKey()) {
                        case XdbId.XDB_KEY_PUBMED:
                            if (!pubmedIdsManager.isCuratedPubmedId(xdbId.getRgdId(), xdbId.getAccId()))
                                rec.addUncuratedPubmedIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_NCBI_GENE:
                            rec.addNcbiGeneIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_UNIPROT:
                            boolean isUniProtSource = Utils.defaultString(xdbId.getSrcPipeline()).startsWith("UniProt");
                            rec.addUniprotIds(xdbId.getAccId(), canonicalProteins.contains(xdbId.getAccId()) && isUniProtSource);
                            break;
                        case XdbId.XDB_KEY_GENEBANKNU:
                            rec.addGeneBankNucleoIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_TIGR:
                            if( speciesType==SpeciesType.RAT ) { // TIGR IDs are available only for rat
                                rec.addTigerIds(xdbId.getAccId());
                            }
                            break;
                        case XdbId.XDB_KEY_GENEBANKPROT:
                            rec.addGeneBankProteinIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_HGNC:
                            rec.addHgncIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_VGNC:
                            rec.addVgncIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_OMIM:
                            rec.addOmimIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_MGD:
                            rec.addMgdIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_ENSEMBL_GENES:
                            rec.addEnsemblGeneIds(xdbId.getAccId());
                            break;
                    }
                }

                // get rgd id and gene symbol for splices: rat only
                if( speciesType==SpeciesType.RAT ) {
                    for (Gene splice : dao.getSplices(rec.getGeneKey())) {
                        rec.addSpliceRgdIds(Integer.toString(splice.getRgdId()));
                        rec.addSpliceSymbols(splice.getSymbol());
                    }
                }

                // get rgd id and marker names for markers
                for (SSLP marker : dao.getMarkers(rec.getGeneKey())) {
                    rec.addMarkerRgdIds(Integer.toString(marker.getRgdId()));
                    rec.addMarkerNames(marker.getName());
                }

                // get aliases
                for (Alias alias : dao.getAliases(rec.getRgdId(), ALIAS_TYPES)) {

                    if( FtpFileExtractsManager.isStringAscii7(alias.getValue()) ) {

                        if (alias.getTypeName().equals("old_gene_name"))
                            rec.addOldGeneNames(alias.getValue());
                        if (alias.getTypeName().equals("old_gene_symbol"))
                            rec.addOldGeneSymbols(alias.getValue());
                    } else {
                        counters.increment("alias-not-ascii7");
                    }
                }

                // get rgd id and qtl names for associated qtls
                for (QTL qtl : dao.getQtlsForGene(rec.getGeneKey())) {
                    rec.addQtlRgdIds(Integer.toString(qtl.getRgdId()));
                    rec.addQtlNames(qtl.getSymbol());
                }

                // get the most recent nomenclature event
                for (NomenclatureEvent event : dao.getNomenEvents(rec.getRgdId())) {
                    rec.setNomenEvents(event.getNomenStatusType());
                    break; // we take only one most recent nomen event even if there are more
                }

                // write out all the parameters to the file
                String line = generateLineContents(rec, speciesType);
                lineMap.put(rec.getRgdId(), line);

            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

        writeDataLines(writer, lineMap);

        // close the output file
        writer.close();

        log.info("   "+outputFileName+",  data lines written: "+lineMap.size()+"\n"
            +"      canonical proteins written: "+counters.get("canonical_proteins")
            +"      canonical proteins in RGD: "+canonicalProteins.size());

        int nonAscii7Aliases = counters.get("alias-not-ascii7");
        if( nonAscii7Aliases>0 ) {
            log.info("      non-ASCII-7 aliases skipped: " + nonAscii7Aliases);
        }

        return outputFileName;
    }

    String generateLineContents(GeneExtractRecord rec, int speciesType) throws Exception {

        String canonicalProtein = rec.getCanonicalUniprotIds();
        if( !Utils.isStringEmpty(canonicalProtein) ) {
            counters.increment("canonical_proteins");
        }

        // SECTION 1: common columns for all species
        StringBuilder buf = new StringBuilder();
        buf.append(rec.getRgdId())
            .append('\t')
            .append(checkNull(rec.getGeneSymbol()))
            .append('\t')
            .append(checkNull(rec.getGeneFullName()))
            .append('\t')
            .append(checkNull(rec.getGeneDesc()))
            .append('\t')

            .append(getString(rec.celeraMap, "getChromosome"))
            .append('\t')
            .append(getString(rec.assembly1Map, "getChromosome"))
            .append('\t')
            .append(getString(rec.assembly2Map, "getChromosome"))
            .append('\t')
            .append(getString(rec.cytoMap, "getFishBand"))
            .append('\t')

            .append(getString(rec.celeraMap, "getStartPos"))
            .append('\t')
            .append(getString(rec.celeraMap, "getStopPos"))
            .append('\t')
            .append(getString(rec.celeraMap, "getStrand"))
            .append('\t')

            .append(getString(rec.assembly1Map, "getStartPos"))
            .append('\t')
            .append(getString(rec.assembly1Map, "getStopPos"))
            .append('\t')
            .append(getString(rec.assembly1Map, "getStrand"))
            .append('\t')

            .append(getString(rec.assembly2Map, "getStartPos"))
            .append('\t')
            .append(getString(rec.assembly2Map, "getStopPos"))
            .append('\t')
            .append(getString(rec.assembly2Map, "getStrand"))
            .append('\t')

            .append(checkNull(rec.getCuratedRefRgdIds()))
            .append('\t')
            .append(checkNull(rec.getCuratedPubmedIds()))
            .append('\t')
            .append(checkNull(rec.getUncuratedPubmedIds()))
            .append('\t')
            .append(checkNull(rec.getNcbiGeneIds()))
            .append('\t')
            .append(checkNull(rec.getUniprotIds()))
            .append('\t')

            .append(checkNull(rec.getRefSeqStatus()))
            .append('\t')
            .append(checkNull(rec.getGeneBankNucleoIds()))
            .append('\t')
            .append(checkNull(rec.getTigerIds()))
            .append('\t')
            .append(checkNull(rec.getGeneBankProteinIds()))
            .append('\t')
            .append(checkNull(canonicalProtein))
            .append('\t')

            .append(checkNull(rec.getMarkerRgdIds()))
            .append('\t')
            .append(checkNull(rec.getMarkerNames()))
            .append('\t')
            .append(checkNull(rec.getOldGeneSymbols()))
            .append('\t')
            .append(checkNull(rec.getOldGeneNames()))
            .append('\t')
            .append(checkNull(rec.getQtlRgdIds()))
            .append('\t')
            .append(checkNull(rec.getQtlNames()))
            .append('\t')
            .append(checkNull(rec.getNomenEvents()))
            .append('\t')
            .append(checkNull(rec.getSpliceRgdIds()))
            .append('\t')
            .append(checkNull(rec.getSpliceSymbols()))
            .append('\t')
            .append(checkNull(rec.getGeneType()))
            .append('\t')
            .append(checkNull(rec.getEnsemblGeneIds()));

        // SECTION 2: species specific section: between common section and Ensembl positions
        switch( speciesType ) {
            case SpeciesType.RAT:
                buf.append('\t')
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getChromosome"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStartPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStopPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStrand"))
                    .append('\t')
                    .append(getString(rec.assembly4Map, "getChromosome"))
                    .append('\t')
                    .append(getString(rec.assembly4Map, "getStartPos"))
                    .append('\t')
                    .append(getString(rec.assembly4Map, "getStopPos"))
                    .append('\t')
                    .append(getString(rec.assembly4Map, "getStrand"));
                break;

            case SpeciesType.HUMAN:
                buf.append('\t')
                    .append(checkNull(rec.getHgncIds()))
                    .append('\t')
                    .append('\t')
                    .append(checkNull(rec.getOmimIds()))
                    .append('\t')
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getChromosome"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStartPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStopPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStrand"));
                break;

            case SpeciesType.MOUSE:
                buf.append('\t')
                    .append(checkNull(rec.getMgdIds()))
                    .append('\t')
                    .append(rec.getAbsPos()!=null ? Double.toString(rec.getAbsPos()) : "")
                    .append('\t')
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getChromosome"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStartPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStopPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStrand"));
                break;

            case SpeciesType.DOG:
            case SpeciesType.PIG:
                buf.append('\t')
                   .append(checkNull(rec.getVgncIds()));
                break;
        }

        // SECTION 3: Ensembl positions
        buf.append('\t')
            .append(getString(rec.ensemblMap, "getChromosome"))
            .append('\t')
            .append(getString(rec.ensemblMap, "getStartPos"))
            .append('\t')
            .append(getString(rec.ensemblMap, "getStopPos"))
            .append('\t')
            .append(getString(rec.ensemblMap, "getStrand"));

        // SECTION 4: after Ensembl positions
        if( speciesType==SpeciesType.DOG ) {
            buf.append('\t')
            .append(getString(rec.assembly4Map, "getChromosome"))
            .append('\t')
            .append(getString(rec.assembly4Map, "getStartPos"))
            .append('\t')
            .append(getString(rec.assembly4Map, "getStopPos"))
            .append('\t')
            .append(getString(rec.assembly4Map, "getStrand"))
            .append('\t')
            .append(getString(rec.assembly5Map, "getChromosome"))
            .append('\t')
            .append(getString(rec.assembly5Map, "getStartPos"))
            .append('\t')
            .append(getString(rec.assembly5Map, "getStopPos"))
            .append('\t')
            .append(getString(rec.assembly5Map, "getStrand"))
            .append('\t')
            .append(getString(rec.assembly6Map, "getChromosome"))
            .append('\t')
            .append(getString(rec.assembly6Map, "getStartPos"))
            .append('\t')
            .append(getString(rec.assembly6Map, "getStopPos"))
            .append('\t')
            .append(getString(rec.assembly6Map, "getStrand"));
        }
        else if( speciesType==SpeciesType.RAT ) {
            buf.append('\t')
               .append(getString(rec.assembly5Map, "getChromosome"))
               .append('\t')
               .append(getString(rec.assembly5Map, "getStartPos"))
               .append('\t')
               .append(getString(rec.assembly5Map, "getStopPos"))
               .append('\t')
               .append(getString(rec.assembly5Map, "getStrand"));

            buf.append('\t')
                .append(getString(rec.assembly6Map, "getChromosome"))
                .append('\t')
                .append(getString(rec.assembly6Map, "getStartPos"))
                .append('\t')
                .append(getString(rec.assembly6Map, "getStopPos"))
                .append('\t')
                .append(getString(rec.assembly6Map, "getStrand"));

            buf.append('\t')
                .append(getString(rec.assembly7Map, "getChromosome"))
                .append('\t')
                .append(getString(rec.assembly7Map, "getStartPos"))
                .append('\t')
                .append(getString(rec.assembly7Map, "getStopPos"))
                .append('\t')
                .append(getString(rec.assembly7Map, "getStrand"));

            buf.append('\t')
                .append(getString(rec.assembly8Map, "getChromosome"))
                .append('\t')
                .append(getString(rec.assembly8Map, "getStartPos"))
                .append('\t')
                .append(getString(rec.assembly8Map, "getStopPos"))
                .append('\t')
                .append(getString(rec.assembly8Map, "getStrand"));
        }

        buf.append("\n");

        return buf.toString();
    }

    String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

    String checkNull(int val) {
        return val<=0 ? "" : Integer.toString(val);
    }

    List<XdbId> getXdbIdList(int rgdId) throws Exception {
        return getDao().getXdbIds(rgdId);
    }

    String getString(List<MapData> mds, String method) throws Exception {
        if( mds==null || mds.isEmpty() )
            return "";
        return Utils.concatenate(";", mds, method);
    }


    public void setMapKeys(java.util.Map<String,List<String>> mapKeys) {
        this.mapKeys = mapKeys;
    }

    public java.util.Map<String,List<String>> getMapKeys() {
        return mapKeys;
    }

    public void setFileNamePrefix(String fileNamePrefix) {
        this.fileNamePrefix = fileNamePrefix;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }
}