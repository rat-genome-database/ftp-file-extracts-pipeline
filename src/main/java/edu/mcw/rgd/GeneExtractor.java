package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.pipelines.PipelineManager;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordPreprocessor;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Nov 29, 2010
 * Time: 12:29:12 PM
 */
public class GeneExtractor extends BaseExtractor {


    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: genes-version-2.2.5\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### As of Apr 1, 2011 RATMAP_IDs and RHDB_IDs are discontinued.\n"
    +"### As of Apr 15, 2011 GENE_REFSEQ_STATUS column is provided.\n"
    +"### As of Jul 1, 2011 fixed generation of CURATED_REF_PUBMED_IDs and UNCURATED_PUBMED_IDs\n"
    +"### As of Nov 23, 2011 ver 2.1.2: no format changes (UniGene Ids are extracted from db in different way).\n"
    +"### As of Dec 19, 2011 ver 2.1.2.1: no format changes (fixed documentation in header to be consistent with column names).\n"
    +"### As of May 31, 2012 ver 2.1.2.2: no format changes (improved generation speed).\n"
    +"### As of July 6, 2012 ver 2.1.2.3: no format changes (added generation of file GENES_RAT_5.0).\n"
    +"### As of Oct 23, 2012 ver 2.1.2.4: obsoleted column 23 'UNCURATED_REF_MEDLINE_ID' - changed to '(UNUSED)'.\n"
    +"### As of Dec 17, 2012 ver 2.1.2.5: no format changes.\n"
    +"### As of Aug 19, 2013 ver 2.1.2.6: gene descriptions made consistent with gene report pages from RGD website.\n"
    +"### As of Oct 2, 2014 ver 2.2: genes files refactored:\n"
    +"###   GENES_HUMAN_B38.txt retired -- added new columns to GENES_HUMAN.txt to accommodate positions for GRCh38.\n"
    +"###   GENES_MOUSE_B36.txt retired -- added new columns to GENES_MOUSE.txt to accommodate positions for assembly build 36.\n"
    +"###   GENES_RAT_5.0.txt and GENES_RAT_6.0.txt retired -- added new columns to GENES_RAT.txt to accommodate positions for Rnor_5.0 and Rnor_6.0.\n"
    +"### As of Feb 15, 2017 ver 2.2.2: HPRD_IDs are discontinued for human genes.\n"
    +"### As of May 25, 2017 ver 2.2.5: GENE_REFSEQ_STATUS is now published in column 23 for all species\n"
    +"###   during transition period, for rat, mouse and human, GENE_REFSEQ_STATUS will continue to be laso published in columns 39, 41 and 42 respectively\n"
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
    +"#25  TIGR_ID                TIGR ID(s)\n"
    +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
    +"#27  UNIGENE_ID             UniGene ID(s)\n"
    +"#28  SSLP_RGD_ID            RGD_ID(s) of SSLPS associated with given gene\n"
    +"#29  SSLP_SYMBOL            SSLP symbol\n"
    +"#30  OLD_SYMBOL             old symbol alias(es)\n"
    +"#31  OLD_NAME               old name alias(es)\n"
    +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
    +"#33  QTL_SYMBOL             QTL symbol\n"
    +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
    +"#35  SPLICE_RGD_ID          RGD_IDs for gene splices\n"
    +"#36  SPLICE_SYMBOL          symbol for gene \n"
    +"#37  GENE_TYPE              gene type\n"
    +"#38  ENSEMBL_ID             Ensembl Gene ID\n";

    final String HEADER_COMMON_LINES_ONE_ASSEMBLY =
        "# RGD-PIPELINE: ftp-file-extracts\n"
        +"# MODULE: genes-version-2.2.5\n"
        +"# GENERATED-ON: #DATE#\n"
        +"# PURPOSE: information about active #SPECIES# genes extracted from RGD database\n"
        +"# CONTACT: rgd.data@mcw.edu\n"
        +"# FORMAT: tab delimited text\n"
        +"# NOTES: multiple values in a single column are separated by ';'\n"
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
        +"#25  TIGR_ID                TIGR ID(s)\n"
        +"#26  GENBANK_PROTEIN        GenBank Protein ID(s)\n"
        +"#27  UNIGENE_ID             UniGene ID(s)\n"
        +"#28  SSLP_RGD_ID            RGD_ID(s) of SSLPS associated with given gene\n"
        +"#29  SSLP_SYMBOL            SSLP symbol\n"
        +"#30  OLD_SYMBOL             old symbol alias(es)\n"
        +"#31  OLD_NAME               old name alias(es)\n"
        +"#32  QTL_RGD_ID             RGD_ID(s) of QTLs associated with given gene\n"
        +"#33  QTL_SYMBOL             QTL symbol\n"
        +"#34  NOMENCLATURE_STATUS    nomenclature status\n"
        +"#35  SPLICE_RGD_ID          RGD_IDs for gene splices\n"
        +"#36  SPLICE_SYMBOL          symbol for gene \n"
        +"#37  GENE_TYPE              gene type\n"
        +"#38  ENSEMBL_ID             Ensembl Gene ID\n"
        +"#\n"
        +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\t(UNUSED)\tCHROMOSOME_#REF1#\t(UNUSED)\t"
        +"FISH_BAND\t(UNUSED)\t(UNUSED)\t(UNUSED)\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
        +"(UNUSED)\t(UNUSED)\t(UNUSED)\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
        +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\tTIGR_ID\t"
        +"GENBANK_PROTEIN\tUNIGENE_ID\tSSLP_RGD_ID\tSSLP_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
        +"NOMENCLATURE_STATUS\tSPLICE_RGD_ID\tSPLICE_SYMBOL\tGENE_TYPE\tENSEMBL_ID";


    final String HEADER_LINE_RAT =
     "#39  GENE_REFSEQ_STATUS_LEGACY legacy column -- use column 23 instead -- it will be discontinued in the future\n"
    +"#40  CHROMOSOME_#REF3#         chromosome for Rnor_#REF3# reference assembly\n"
    +"#41  START_POS_#REF3#          start position for Rnor_#REF3# reference assembly\n"
    +"#42  STOP_POS_#REF3#           stop position for Rnor_#REF3# reference assembly\n"
    +"#43  STRAND_#REF3#             strand information for Rnor_#REF3# reference assembly\n"
    +"#44  CHROMOSOME_#REF4#         chromosome for Rnor_#REF4# reference assembly\n"
    +"#45  START_POS_#REF4#          start position for Rnor_#REF4# reference assembly\n"
    +"#46  STOP_POS_#REF4#           stop position for Rnor_#REF4# reference assembly\n"
    +"#47  STRAND_#REF4#             strand information for Rnor_#REF4# reference assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_CELERA\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_CELERA\tSTOP_POS_CELERA\tSTRAND_CELERA\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\tTIGR_ID\t"
    +"GENBANK_PROTEIN\tUNIGENE_ID\tSSLP_RGD_ID\tSSLP_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\tSPLICE_RGD_ID\tSPLICE_SYMBOL\tGENE_TYPE\tENSEMBL_ID\tGENE_REFSEQ_STATUS\t"
    +"CHROMOSOME_#REF3#\tSTART_POS_#REF3#\tSTOP_POS_#REF3#\tSTRAND_#REF3#\t"
    +"CHROMOSOME_#REF4#\tSTART_POS_#REF4#\tSTOP_POS_#REF4#\tSTRAND_#REF4#";

    final String HEADER_LINE_HUMAN =
     "#39  HGNC_ID            Human Genome Nomenclature Committee ID\n"
    +"#40  (UNUSED)\n"
    +"#41  OMIM_ID            Online Mendelian Inheritance in Man ID\n"
    +"#42  GENE_REFSEQ_STATUS_LEGACY legacy column -- use column 23 instead -- it will be discontinued in the future\n"
    +"#43  CHROMOSOME_#REF3#         chromosome for GRCh#REF3# reference assembly\n"
    +"#44  START_POS_#REF3#          start position for GRCh#REF3# reference assembly\n"
    +"#45  STOP_POS_#REF3#           stop position for GRCh#REF3# reference assembly\n"
    +"#46  STRAND_#REF3#             strand information for GRCh#REF3# reference assembly\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_CELERA\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_CELERA\tSTOP_POS_CELERA\tSTRAND_CELERA\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\tTIGR_ID\t"
    +"GENBANK_PROTEIN\tUNIGENE_ID\tSSLP_RGD_ID\tSSLP_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\tSPLICE_RGD_ID\tSPLICE_SYMBOL\tGENE_TYPE\tENSEMBL_ID\tHGNC_ID\t(UNUSED)\tOMIM_ID\tGENE_REFSEQ_STATUS\t"
    +"CHROMOSOME_#REF3#\tSTART_POS_#REF3#\tSTOP_POS_#REF3#\tSTRAND_#REF3#";

    final String HEADER_LINE_MOUSE =
     "#39  MGD_ID             MGD ID\n"
    +"#40  CM_POS             mouse cM map absolute position\n"
    +"#41  GENE_REFSEQ_STATUS_LEGACY legacy column -- use column 23 instead -- it will be discontinued in the future\n"
    +"#42  CHROMOSOME_#REF3#         chromosome for reference assembly build #REF3#\n"
    +"#43  START_POS_#REF3#          start position for reference assembly build #REF3#\n"
    +"#44  STOP_POS_#REF3#           stop position for reference assembly build #REF3#\n"
    +"#45  STRAND_#REF3#             strand information for reference assembly build #REF3#\n"
    +"#\n"
    +"GENE_RGD_ID\tSYMBOL\tNAME\tGENE_DESC\tCHROMOSOME_CELERA\tCHROMOSOME_#REF1#\tCHROMOSOME_#REF2#\t"
    +"FISH_BAND\tSTART_POS_CELERA\tSTOP_POS_CELERA\tSTRAND_CELERA\tSTART_POS_#REF1#\tSTOP_POS_#REF1#\tSTRAND_#REF1#\t"
    +"START_POS_#REF2#\tSTOP_POS_#REF2#\tSTRAND_#REF2#\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\tUNCURATED_PUBMED_ID\t"
    +"NCBI_GENE_ID\tUNIPROT_ID\tGENE_REFSEQ_STATUS\tGENBANK_NUCLEOTIDE\tTIGR_ID\t"
    +"GENBANK_PROTEIN\tUNIGENE_ID\tSSLP_RGD_ID\tSSLP_SYMBOL\tOLD_SYMBOL\tOLD_NAME\tQTL_RGD_ID\tQTL_SYMBOL\t"
    +"NOMENCLATURE_STATUS\tSPLICE_RGD_ID\tSPLICE_SYMBOL\tGENE_TYPE\tENSEMBL_ID\tMGD_ID\tCM_POS\tGENE_REFSEQ_STATUS\t"
    +"CHROMOSOME_#REF3#\tSTART_POS_#REF3#\tSTOP_POS_#REF3#\tSTRAND_#REF3#";

    Logger log = Logger.getLogger(getClass());
    private java.util.Map<String,List<String>> mapKeys;
    private String fileNamePrefix;

    public void run(SpeciesRecord si) throws Exception {

        final int speciesType = si.getSpeciesType();

        // generate regular file
        String outputFileName = generate(si);
        FtpFileExtractsManager.qcFileContent(outputFileName, "genes", speciesType);

        // generate obsolete ids
        outputFileName = generateObsoleteIds();
    }

    String generate(final SpeciesRecord si) throws Exception {

        String outputFileName = getFileNamePrefix()+si.getSpeciesName().toUpperCase()+".txt";
        outputFileName = getExtractDir()+'/'+outputFileName;
        log.info("started extraction to "+outputFileName);

        final FtpFileExtractsDAO dao = getDao();
        final int speciesType = si.getSpeciesType();
        final PrintWriter writer = new PrintWriter(outputFileName);


        String assembly1, assembly2, assembly3, assembly4;
        final int mapKey1, mapKey2, mapKey3, mapKey4;

        List<String> mapInfos = getMapKeys().get(si.getSpeciesName().toLowerCase());
        if( mapInfos.size()>=1 ) {
            String mapInfo = mapInfos.get(0);
            assembly1 = mapInfo.substring(1+mapInfo.indexOf(' '));
            mapKey1 = Integer.parseInt(mapInfo.substring(0, mapInfo.indexOf(' ')));
        } else {
            assembly1 = null; mapKey1 = 0;
        }
        if( mapInfos.size()>=2 ) {
            String mapInfo = mapInfos.get(1);
            assembly2 = mapInfo.substring(1+mapInfo.indexOf(' '));
            mapKey2 = Integer.parseInt(mapInfo.substring(0, mapInfo.indexOf(' ')));
        } else {
            assembly2 = null; mapKey2 = 0;
        }
        if( mapInfos.size()>=3 ) {
            String mapInfo = mapInfos.get(2);
            assembly3 = mapInfo.substring(1+mapInfo.indexOf(' '));
            mapKey3 = Integer.parseInt(mapInfo.substring(0, mapInfo.indexOf(' ')));
        } else {
            assembly3 = null; mapKey3 = 0;
        }
        if( mapInfos.size()>=4 ) {
            String mapInfo = mapInfos.get(3);
            assembly4 = mapInfo.substring(1+mapInfo.indexOf(' '));
            mapKey4 = Integer.parseInt(mapInfo.substring(0, mapInfo.indexOf(' ')));
        } else {
            assembly4 = null; mapKey4 = 0;
        }


        // prepare header common lines
        String headerLines = (assembly2==null ? HEADER_COMMON_LINES_ONE_ASSEMBLY : HEADER_COMMON_LINES)
                .replace("#SPECIES#", si.getSpeciesName())
                .replace("#DATE#", SpeciesRecord.getTodayDate());

        headerLines += speciesType== SpeciesType.RAT ? HEADER_LINE_RAT :
                speciesType==SpeciesType.HUMAN ? HEADER_LINE_HUMAN :
                speciesType==SpeciesType.MOUSE ? HEADER_LINE_MOUSE : "";

        if( assembly1!=null )
            headerLines = headerLines.replace("#REF1#", assembly1);
        if( assembly2!=null )
            headerLines = headerLines.replace("#REF2#", assembly2);
        if( assembly3!=null )
            headerLines = headerLines.replace("#REF3#", assembly3);
        if( assembly4!=null )
            headerLines = headerLines.replace("#REF4#", assembly4);

        writer.println(headerLines);

        // load all curated pubmed ids
        final PubmedIdsManager pubmedIdsManager = new PubmedIdsManager();
        pubmedIdsManager.loadCuratedPubmedIds(dao, RgdId.OBJECT_KEY_GENES, si.getSpeciesType());

        // create pipeline managing framework
        PipelineManager manager = new PipelineManager();

        // setup pipeline parser "DB" - 1 thread -- max 1000 GeneExtractRecords in output queue
        manager.addPipelineWorkgroup(new RecordPreprocessor() {
            // parser: break source into a stream of GeneRecord-s
            public void process() throws Exception {
                // process active genes for given species
                int recNo = 0;
                for( Gene gene: dao.getActiveGenes(speciesType) ) {
                    log.debug("processing gene "+gene.getSymbol()+", RGD:"+gene.getRgdId());
                    GeneExtractRecord rec = new GeneExtractRecord();
                    rec.setRecNo(++recNo);
                    rec.setGeneKey(gene.getKey());
                    rec.setRgdId(gene.getRgdId());
                    rec.setGeneSymbol(gene.getSymbol());
                    rec.setGeneFullName(gene.getName());
                    rec.setGeneDesc(Utils.getGeneDescription(gene));
                    rec.setGeneType(gene.getType());
                    rec.setRefSeqStatus(gene.getRefSeqStatus());
                    getSession().putRecordToFirstQueue(rec);
                }
            }
        }, "DB", 1, 1000);


        // setup pipeline "QC" - 8 parallel threads -- max 1000 GeneExtractRecords in output queue
        manager.addPipelineWorkgroup(new RecordProcessor() {

            // gather data from database
            public void process(PipelineRecord r) throws Exception {
                GeneExtractRecord rec = (GeneExtractRecord) r;

                if( rec.getRecNo()%100==0 )
                    log.debug("QC recno="+rec.getRecNo());

                for( MapData md: dao.getMapData(rec.getRgdId()) ) {
                    // skip maps with empty chromosomes
                    if( md.getChromosome()==null || md.getChromosome().trim().length()==0 )
                        continue;

                    if( md.getMapKey()==mapKey1 ) {
                        rec.assembly1Map.add(md);
                    }
                    else if( md.getMapKey()==mapKey2 ) {
                        rec.assembly2Map.add(md);
                    }
                    else if( md.getMapKey()==mapKey3 ) {
                        rec.assembly3Map.add(md);
                    }
                    else if( md.getMapKey()==mapKey4 ) {
                        rec.assembly4Map.add(md);
                    }
                    else if( md.getMapKey()==si.getCeleraAssemblyMapKey() ) {
                        rec.celeraMap.add(md);
                    }
                    else if( md.getMapKey()==si.getCytoMapKey() ) {
                        rec.cytoMap.add(md);
                    }
                    else if( md.getMapKey()==si.getCmMapKey() ) {
                        rec.setAbsPos(md.getAbsPosition());
                    }
                }

                rec.addCuratedRefRgdIds(dao.getCuratedRefs(rec.getRgdId()));
                for( String curatedPubmedId: pubmedIdsManager.getCuratedPubmedIds(rec.getRgdId()) ) {
                    rec.addCuratedPubmedIds(curatedPubmedId);
                }

                for( XdbId xdbId: getXdbIdList(rec.getRgdId())) {
                    switch( xdbId.getXdbKey()) {
                        case XdbId.XDB_KEY_PUBMED:
                            if( !pubmedIdsManager.isCuratedPubmedId(xdbId.getRgdId(), xdbId.getAccId()) )
                                rec.addUncuratedPubmedIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_NCBI_GENE:
                            rec.addNcbiGeneIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_UNIPROT:
                            rec.addUniprotIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_GENEBANKNU:
                            rec.addGeneBankNucleoIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_TIGR:
                            rec.addTigerIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_GENEBANKPROT:
                            rec.addGeneBankProteinIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_UNIGENE:
                            rec.addUniGeneIds(xdbId.getLinkText());
                            break;
                        case XdbId.XDB_KEY_HGNC:
                            rec.addHgncIds(xdbId.getAccId());
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

                // get rgd id and gene symbol for splices
                for( Gene splice: dao.getSplices(rec.getGeneKey()) ) {
                    rec.addSpliceRgdIds(Integer.toString(splice.getRgdId()));
                    rec.addSpliceSymbols(splice.getSymbol());
                }

                // get rgd id and sslp names for sslps
                for( SSLP sslp: dao.getSslps(rec.getGeneKey()) ) {
                    rec.addSslpRgdIds(Integer.toString(sslp.getRgdId()));
                    rec.addSslpNames(sslp.getName());
                }

                // get aliases
                for( Alias alias: dao.getAliases(rec.getRgdId()) ) {
                    if( alias.getTypeName()==null )
                        continue;
                    if( alias.getTypeName().equals("old_gene_name") )
                        rec.addOldGeneNames(alias.getValue());
                    if( alias.getTypeName().equals("old_gene_symbol") )
                        rec.addOldGeneSymbols(alias.getValue());
                }

                // get rgd id and qtl names for associated qtls
                for( QTL qtl: dao.getQtlsForGene(rec.getGeneKey()) ) {
                    rec.addQtlRgdIds(Integer.toString(qtl.getRgdId()));
                    rec.addQtlNames(qtl.getSymbol());
                }

                // get the most recent nomenclature event
                for( NomenclatureEvent event: dao.getNomenEvents(rec.getRgdId())) {
                    rec.setNomenEvents(event.getNomenStatusType());
                    break; // we take only one most recent nomen event even if there are more
                }

            }
        }, "QC", 8, 0);

        // setup data loading pipeline "DL" - 1 thread; writing records to output file
        manager.addPipelineWorkgroup(new RecordProcessor() {
            // write record to a line in output file
            public void process(PipelineRecord r) throws Exception {
                GeneExtractRecord rec = (GeneExtractRecord) r;

                // write out all the parameters to the file
                writeLine(rec, writer, speciesType);
            }
        }, "DL", 1, 0);

        // run pipelines
        manager.run();

        // close the output file
        writer.close();

        return outputFileName;
    }

    void writeLine(GeneExtractRecord rec, PrintWriter writer, int speciesType) throws Exception {

        writer.print(rec.getRgdId());
        writer.append('\t')
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
            .append(checkNull(rec.getUniGeneIds()))
            .append('\t')

            .append(checkNull(rec.getSslpRgdIds()))
            .append('\t')
            .append(checkNull(rec.getSslpNames()))
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

        // species specific section
        switch( speciesType ) {
            case SpeciesType.RAT:
                writer.append('\t')
                    .append(checkNull(rec.getRefSeqStatus()))
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
                writer.append('\t')
                    .append(checkNull(rec.getHgncIds()))
                    .append('\t')
                    .append('\t')
                    .append(checkNull(rec.getOmimIds()))
                    .append('\t')
                    .append(checkNull(rec.getRefSeqStatus()))
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
                writer.append('\t')
                    .append(checkNull(rec.getMgdIds()))
                    .append('\t')
                    .append(rec.getAbsPos()!=null ? Double.toString(rec.getAbsPos()) : "")
                    .append('\t')
                    .append(checkNull(rec.getRefSeqStatus()))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getChromosome"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStartPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStopPos"))
                    .append('\t')
                    .append(getString(rec.assembly3Map, "getStrand"));
                break;
        }

        writer.println();
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

    String generateObsoleteIds() throws Exception {

        // check if the property 'generate_obsolete_ids' is enabled
        if( getCmdLineProperties().get("generate_obsolete_ids")==null )
            return null;

        String outputFileName = getExtractDir()+"/GENES_OBSOLETE_IDS.txt";
        log.info("generating file with obsolete ids for genes: "+outputFileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));

        final String HEADER =
         "# RGD-PIPELINE: ftp-file-extracts\n"
        +"# MODULE: obsolete-ids-version-1.0\n"
        +"# GENERATED-ON: #DATE#\n"
        +"# PURPOSE: list of RGD IDs for genes that have been retired or withdrawn from RGD database\n"
        +"# CONTACT: rgd.data@mcw.edu\n"
        +"# FORMAT: tab delimited text\n"
        +"#\n"
        +"### As of Oct 8, 2013 generates obsolete RGD IDs for genes.\n"
        +"#\n"
        +"#COLUMN INFORMATION:\n"
        +"#1   SPECIES	          name of the species\n"
        +"#2   OLD_GENE_RGD_ID    old gene RGD ID\n"
        +"#3   OLD_GENE_SYMBOL    old gene symbol\n"
        +"#4   OLD_GENE_STATUS    old gene status\n"
        +"#5   OLD_GENE_TYPE      old gene type\n"
        +"#6   NEW_GENE_RGD_ID    new gene RGD ID (if any)\n"
        +"#7   NEW_GENE_SYMBOL    new gene symbol (if any)\n"
        +"#8   NEW_GENE_STATUS    old gene status (if any)\n"
        +"#9   NEW_GENE_TYPE      new gene type (if any)\n"

        +"#\n"
        +"SPECIES\t"
        +"OLD_GENE_RGD_ID\tOLD_GENE_SYMBOL\tOLD_GENE_STATUS\tOLD_GENE_TYPE\t"
        +"NEW_GENE_RGD_ID\tNEW_GENE_SYMBOL\tNEW_GENE_STATUS\tNEW_GENE_TYPE\n";

        // prepare header common lines
        String header = HEADER.replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.write(header);

        for( ObsoleteId id: getDao().getObsoleteIdsForGenes() ) {
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
                .append('\n');
        }
        writer.close();

        return outputFileName;
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