package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.pipelines.PipelineManager;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordPreprocessor;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mtutaj
 * @since Nov 29, 2010
 */
public class SslpExtractor extends BaseExtractor {

    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: sslps-version-2.5.2\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# sslps extracted from RGD database\n"
    +"# CONTACT: rgd.developers@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"#  Where a marker has multiple positions for a single map/assembly\n"
    +"#  (f.e. D1Arb36 has positions 78134192..78134522 and 78153000..78153323 on ref assembly)\n"
    +"#   is how it is presented in the columns CHROMOSOME_3.4, START_POS_3.4 and STOP_POS_3.4:\n"
    +"#   1;1  <tab>   78134192;78153000   <tab>  78134522;78153323\n"
    +"#\n"
    +"### As of Apr 1, 2011 RATMAP_IDs and RHDB_IDs are discontinued.\n"
    +"### As of Jul 1, 2011 fixed generation of UNCURATED_REF_PUBMED_IDs.\n"
    +"### As of Nov 23, 2011 v. 2.3.1: no format changes (UniGene Ids are extracted from db in different way).\n"
    +"### As of Dec 19 2011 v. 2.3.2: no data changes; improved internal QC.\n"
    +"### As of May 31 2012 v. 2.3.3: no data changes; (optimized retrieval of maps data from database)\n"
    +"### As of Oct 22 2012 v. 2.3.4: fixed export of positional information for mouse (positions on assembly build 38 were exported as positions on assembly 37).\n"
    +"### As of Nov 20 2012 v. 2.4: rat: positions on assembly map 3.1 are no longer exported; instead position on assembly 5.0 are exported.\n"
    +"### As of Dec 26 2013 v. 2.4.1: no data changes; improved internal QC.\n"
    +"### As of Sep 8 2014 v. 2.5.0: rat: available positions on assembly Rnor_6.0.\n"
    +"### As of Oct 29 2018 v. 2.5.2: discontinued columns #8 CLONE_SEQ_RGD_ID and #10 PRIMER_SEQ_RGD_ID. Column #8 now shows sslp type.\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 23 columns are in common between rat, mouse and human)\n"
    +"#\n"
    +"#1   SSLP_RGD_ID	           RGD_ID of the sslp\n"
    +"#2   SPECIES                 species name\n"
    +"#3   SSLP_SYMBOL             sslp symbol\n"
    +"#4   EXPECTED_SIZE           sslp expected size (PCR product size)\n"
    +"#5   CURATED_REF_RGD_ID      RGD_ID of paper(s) on sslp\n"
    +"#6   CURATED_REF_PUBMED_ID   PUBMED_ID of paper(s) on sslp\n"
    +"#7   UNCURATED_REF_PUBMED_ID other PUBMED_IDs\n"
    +"#8   SSLP_TYPE               sslp type, if available\n"
    +"#9   CLONE_SEQUENCE          clone sequence itself\n"
    +"#10  (UNUSED)\n"
    +"#11  FORWARD_SEQ             forward sequence\n"
    +"#12  REVERSE_SEQ             reverse sequence\n"
    +"#13  UNISTS_ID               UniSTS ID\n"
    +"#14  GENBANK_NUCLEOTIDE      GenBank Nucleotide ID(s)\n"
    +"#15  UNIGENE_ID              UniGene ID(s)\n"
    +"#16  ALIAS_VALUE             known aliases for this SSLP\n"
    +"#17  ASSOCIATED_GENE_RGD_ID  RGD_IDs for gene associated with this SSLP\n"
    +"#18  ASSOCIATED_GENE_SYMBOL  symbol for gene associated with this SSLP\n"
    +"#19  CHROMOSOME              chromosome\n"
    +"#20  FISH_BAND               fish band\n"
    +"#21  CHROMOSOME_CELERA       chromosome for Celera assembly\n"
    +"#22  START_POS_CELERA        start position for Celera assembly\n"
    +"#23  STOP_POS_CELERA         stop position for Celera assembly\n";

     final String HEADER_LINE_RAT =
     "#24  CHROMOSOME_5.0          chromosome for the previous reference assembly v.5.0\n"
    +"#25  START_POS_5.0           start position for previous reference assembly v.5.0\n"
    +"#26  STOP_POS_5.0            stop position for previous reference assembly v.5.0\n"
    +"#27  CHROMOSOME_3.4          chromosome for the old reference assembly v.3.4\n"
    +"#28  START_POS_3.4           start position for old reference assembly v.3.4\n"
    +"#29  STOP_POS_3.4            stop position for old reference assembly v.3.4\n"
    +"#30  (UNUSED)\n"
    +"#31  (UNUSED)\n"
    +"#32  CHR_FHHxACI             chromosome for FHH x ACI map\n"
    +"#33  POS_FHHxACI             absolute position (cM) on FHH x ACI map\n"
    +"#34  CHR_SHRSPxBN            chromosome for SHRSP x BN map\n"
    +"#35  POS_SHRSPxBN            absolute position (cM) on SHRSP x BN map\n"
    +"#36  CHR_RH_2.0              chromosome for RH 2.0 map\n"
    +"#37  POS_RH_2.0              absolute position (cR) on RH 2.0 map\n"
    +"#38  CHR_RH_3.4              chromosome for RH 3.4 map\n"
    +"#39  POS_RH_3.4              absolute position (cR) on RH 3.4 map\n"
    +"#40  CHROMOSOME_6.0          chromosome for the current reference assembly v.6.0\n"
    +"#41  START_POS_6.0           start position for current reference assembly v.6.0\n"
    +"#42  STOP_POS_6.0            stop position for current reference assembly v.6.0\n"
    +"#\n"
    +"SSLP_RGD_ID\tSPECIES\tSSLP_SYMBOL\tEXPECTED_SIZE\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\t"
    +"UNCURATED_REF_PUBMED_ID\tSSLP_TYPE\tCLONE_SEQUENCE\t(UNUSED)\tFORWARD_SEQ\tREVERSE_SEQ\t"
    +"UNISTS_ID\tGENBANK_NUCLEOTIDE\tUNIGENE_ID\tALIAS_VALUE\t"
    +"ASSOCIATED_GENE_RGD_ID\tASSOCIATED_GENE_SYMBOL\tCHROMOSOME\tFISH_BAND\t"
    +"CHROMOSOME_CELERA\tSTART_POS_CELERA\tSTOP_POS_CELERA\t"
    +"CHROMOSOME_5.0\tSTART_POS_5.0\tSTOP_POS_5.0\t"
    +"CHROMOSOME_3.4\tSTART_POS_3.4\tSTOP_POS_3.4\t"
    +"(UNUSED)\t(UNUSED)\t"
    +"CHR_FHHxACI\tPOS_FHHxACI\tCHR_SHRSPxBN\tPOS_SHRSPxBN\tCHR_RH_2.0\tPOS_RH_2.0\tCHR_RH_3.4\tPOS_RH_3.4\t"
    +"CHROMOSOME_6.0\tSTART_POS_6.0\tSTOP_POS_6.0";

    final String HEADER_LINE_HUMAN =
    "#24  CHROMOSOME_37          chromosome for the current reference assembly v.37\n"
   +"#25  START_POS_37           start position for current reference assembly v.37\n"
   +"#26  STOP_POS_37            stop position for current reference assembly v.37\n"
   +"#27  CHROMOSOME_36          chromosome for the old reference assembly v.36\n"
   +"#28  START_POS_36           start position for old reference assembly v.36\n"
   +"#29  STOP_POS_36            stop position for old reference assembly v.36\n"
   +"#\n"
   +"SSLP_RGD_ID\tSPECIES\tSSLP_SYMBOL\tEXPECTED_SIZE\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\t"
   +"UNCURATED_REF_PUBMED_ID\tSSLP_TYPE\tCLONE_SEQUENCE\t(UNUSED)\tFORWARD_SEQ\tREVERSE_SEQ\t"
   +"UNISTS_ID\tGENBANK_NUCLEOTIDE\tUNIGENE_ID\tALIAS_VALUE\t"
   +"ASSOCIATED_GENE_RGD_ID\tASSOCIATED_GENE_SYMBOL\tCHROMOSOME\tFISH_BAND\t"
   +"CHROMOSOME_CELERA\tSTART_POS_CELERA\tSTOP_POS_CELERA\t"
   +"CHROMOSOME_37\tSTART_POS_37\tSTOP_POS_37\t"
   +"CHROMOSOME_36\tSTART_POS_36\tSTOP_POS_36";

    final String HEADER_LINE_MOUSE =
    "#24  CHROMOSOME_37          chromosome for the current reference assembly v.37\n"
   +"#25  START_POS_37           start position for current reference assembly v.37\n"
   +"#26  STOP_POS_37            stop position for current reference assembly v.37\n"
   +"#27  CHROMOSOME_36          chromosome for the old reference assembly v.36\n"
   +"#28  START_POS_36           start position for old reference assembly v.36\n"
   +"#29  STOP_POS_36            stop position for old reference assembly v.36\n"
   +"#30  MGD_ID                 MGD ID\n"
   +"#31  CM_POS                 mouse cM map absolute position\n"
   +"#\n"
   +"SSLP_RGD_ID\tSPECIES\tSSLP_SYMBOL\tEXPECTED_SIZE\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\t"
   +"UNCURATED_REF_PUBMED_ID\tSSLP_TYPE\tCLONE_SEQUENCE\t(UNUSED)\tFORWARD_SEQ\tREVERSE_SEQ\t"
   +"UNISTS_ID\tGENBANK_NUCLEOTIDE\tUNIGENE_ID\tALIAS_VALUE\t"
   +"ASSOCIATED_GENE_RGD_ID\tASSOCIATED_GENE_SYMBOL\tCHROMOSOME\tFISH_BAND\t"
   +"CHROMOSOME_CELERA\tSTART_POS_CELERA\tSTOP_POS_CELERA\t"
   +"CHROMOSOME_37\tSTART_POS_37\tSTOP_POS_37\t"
   +"CHROMOSOME_36\tSTART_POS_36\tSTOP_POS_36\t"
   +"MGD_ID\tCM_POS";

    Logger log = Logger.getLogger(getClass());
    private int qcThreadCount;

    public void run(SpeciesRecord speciesInfo) throws Exception {

        System.out.println(getVersion()+" - "+speciesInfo.getSpeciesName());

        final SpeciesRecord speciesRec = speciesInfo;
        String outputFile = speciesInfo.getSslpsFileName();
        if( outputFile==null )
            return;
        outputFile = getExtractDir()+'/'+outputFile;

        final FtpFileExtractsDAO dao = getDao();
        String species = speciesInfo.getSpeciesName();
        final int speciesType = speciesInfo.getSpeciesType();

        final PrintWriter writer = new PrintWriter(outputFile);

        // prepare header common lines
        String commonLines = HEADER_COMMON_LINES
                .replace("#SPECIES#", species)
                .replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.print(commonLines);
        writer.println(speciesType== SpeciesType.RAT ? HEADER_LINE_RAT :
                speciesType==SpeciesType.HUMAN ? HEADER_LINE_HUMAN :
                HEADER_LINE_MOUSE);

        // create pipeline managing framework
        PipelineManager manager = new PipelineManager();

        // setup pipeline parser "DB" - 1 thread -- max 1000 records in output queue
        manager.addPipelineWorkgroup(new RecordPreprocessor() {
            // parser: break source into a stream of record-s
            public void process() throws Exception {
                // process active sslps for given species
                int recNo = 0;
                for( SSLP sslp: dao.getActiveSSLPs(speciesType) ) {
                    SslpRecord rec = new SslpRecord();
                    rec.sslp = sslp;
                    rec.setRecNo(++recNo);
                    getSession().putRecordToFirstQueue(rec);
                }
            }
        }, "DB", 1, 1000);


        // setup pipeline "QC" - several parallel threads -- max 1000 records in output queue
        manager.addPipelineWorkgroup(new RecordProcessor() {

            // gather data from database
            public void process(PipelineRecord r) throws Exception {
                SslpRecord rec = (SslpRecord) r;

                if( rec.getRecNo()%100==0 )
                    log.debug("QC recno="+rec.getRecNo());

                rec.gene = dao.getGeneBySslpKey(rec.sslp.getKey());
                int sslpRgdID = rec.sslp.getRgdId();

                // get chromosome and fish band information for given sslp
                rec.mds = dao.getMapData(sslpRgdID);
                rec.chrAndFishBand = dao.getChromosomeAndFishBand(rec.mds, speciesRec.getCytoMapKey());

                // get other maps
                List<MapData> mds = rec.getMapData(speciesRec.getCeleraAssemblyMapKey());
                if( mds.size()>0 ) {
                    rec.mdCelera = mds;
                }
                mds = rec.getMapData(speciesRec.getNewRefAssemblyMapKeyForSslps());
                if( mds.size()>0 ) {
                    rec.mdNewRef = mds;
                }
                mds = rec.getMapData(speciesRec.getOldRefAssemblyMapKeyForSslps());
                if( mds.size()>0 ) {
                    rec.mdOldRef = mds;
                }

                // get sllp aliases (separated by ';' if there are multiple)
                rec.sslpAliases = getAliases(sslpRgdID);

                rec.xdbIds = getXdbIdList(sslpRgdID);

                rec.uncuratedPubmedID = getDao().getUncuratedPubmedIds(sslpRgdID);
                rec.uniSTSID = rec.getAccessionIds(XdbId.XDB_KEY_UNISTS);
                rec.genBankNucleotideID = rec.getAccessionIds(XdbId.XDB_KEY_GENEBANKNU);
                rec.uniGeneIDs = rec.getLinkText(XdbId.XDB_KEY_UNIGENE);

                rec.curatedRefRGDIDs = getDao().getCuratedRefs(sslpRgdID);
                rec.curatedRefPubmedIDs = getDao().getCuratedPubmedIds(sslpRgdID);

                if( speciesType==SpeciesType.RAT ) {
                    // FHH map
                    mds = rec.getMapData(1);
                    if( mds.size()>0 ) {
                        rec.mdFHHxACI = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for FHH map: sslp_rgd_id=" + sslpRgdID);
                    }

                    // SHRSP map
                    mds = rec.getMapData(2);
                    if( mds.size()>0 ) {
                        rec.mdSHRSPxBN = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for SHRSP map: sslp_rgd_id=" + sslpRgdID);
                    }

                    // RH 2.0 map
                    mds = rec.getMapData(3);
                    if( mds.size()>0 ) {
                        rec.mdRH_2_0 = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for RH 2.0 map: sslp_rgd_id=" + sslpRgdID);
                    }

                    // RH 3.4 map
                    mds = rec.getMapData(5);
                    if( mds.size()>0 ) {
                        rec.mdRH_3_4 = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for RH 3.4 map: sslp_rgd_id=" + sslpRgdID);
                    }

                    mds = rec.getMapData(360);
                    if( mds.size()>0 ) {
                        rec.mdRnor6 = mds;
                    }
                }
                else if( speciesType==SpeciesType.MOUSE ) {
                    mds = rec.getMapData(speciesRec.getCmMapKey());
                    if( mds.size()>0 ) {
                        rec.cmPos = mds.get(0).getAbsPosition();
                        if( mds.size()>1 )
                            log.debug("multiple positions for cM map sslp rgd_Id=" + sslpRgdID);
                    }
                    rec.mgdID = rec.getAccessionIds(XdbId.XDB_KEY_MGD);
                }
                else if( speciesType==SpeciesType.HUMAN ) {
                }
            }
        }, "QC", getQcThreadCount(), 0);

        // setup data loading pipeline "DL" - 1 thread; writing records to output file
        manager.addPipelineWorkgroup(new RecordProcessor() {
            // write record to a line in output file
            public void process(PipelineRecord r) throws Exception {
                SslpRecord rec = (SslpRecord) r;

                // write out all the parameters to the file
                writeLine(rec, writer, speciesType);
            }
        }, "DL", 1, 0);

        // run pipelines
        manager.run();

        // close the output file
        writer.close();

        // copy the output file to the staging area
        FtpFileExtractsManager.qcFileContent(outputFile, "sslps", speciesType);
    }

    void writeLine(SslpRecord rec, PrintWriter writer, int speciesType) throws Exception {

        // SSLP_RGD_ID
        writer.print(rec.sslp.getRgdId());
        writer.print('\t');
        // species name
        writer.print(SpeciesType.getCommonName(speciesType).toLowerCase());
        writer.print('\t');
        // SSLP symbol
        writer.print(checkNull(rec.sslp.getName()));
        writer.print('\t');
        // expected size
        if (rec.sslp.getExpectedSize() != 0) {
            writer.print(rec.sslp.getExpectedSize());
        }
        writer.print('\t');

        writer.print(checkNull(rec.curatedRefRGDIDs));
        writer.print('\t');

        writer.print(checkNull(rec.curatedRefPubmedIDs));
        writer.print('\t');

        writer.print(checkNull(rec.uncuratedPubmedID));
        writer.print('\t');

        writer.print(checkNull(rec.sslp.getSslpType()));
        writer.print('\t');

        writer.print(checkNull(rec.sslp.getTemplateSeq()));
        writer.print('\t');

        // primer rgd ids -- discontinued
        writer.print('\t');

        writer.print(checkNull(rec.sslp.getForwardSeq()));
        writer.print('\t');

        writer.print(checkNull(rec.sslp.getReverseSeq()));
        writer.print('\t');

        writer.print(checkNull(rec.uniSTSID));
        writer.print('\t');

        writer.print(checkNull(rec.genBankNucleotideID));
        writer.print('\t');

        writer.print(checkNull(rec.uniGeneIDs));
        writer.print('\t');

        writer.print(checkNull(rec.sslpAliases));
        writer.print('\t');

        // associated gene RGDID
        if( rec.gene != null ) {
            writer.print(rec.gene.getRgdId());
        }
        writer.print('\t');

        // associated gene symbol
        if( rec.gene != null ) {
            writer.print(checkNull(rec.gene.getSymbol()));
        }
        writer.print('\t');

        // print chromosome
        if( rec.chrAndFishBand[0]!=null ) {
            writer.print(checkNull(rec.chrAndFishBand[0]));
        }
        writer.print('\t');

        // print fish band
        if( rec.chrAndFishBand[1]!=null ) {
            writer.print(checkNull(rec.chrAndFishBand[1]));
        }
        writer.print('\t');

        // print chromosome, start and stop position for celera
        writeGenomicPositions(rec.mdCelera, writer);

        // print chromosome, start and stop position for primary reference assembly
        writeGenomicPositions(rec.mdNewRef, writer);

        // print chromosome, start and stop position for old reference assembly
        writeGenomicPositions(rec.mdOldRef, writer);

        if( speciesType==SpeciesType.RAT ) {

            writer.append("\t\t"); // two unused columns

            // write chromosome and absolute pos for FHH map
            writeGeneticPositions(rec.mdFHHxACI, writer);

            // write chromosome and absolute pos for SHRSP map
            writeGeneticPositions(rec.mdSHRSPxBN, writer);

            // write chromosome and absolute pos for RH 2.0 map
            writeGeneticPositions(rec.mdRH_2_0, writer);

            // write chromosome and absolute pos for RH 3.4 map
            writeGeneticPositions(rec.mdRH_3_4, writer);

            // print chromosome, start and stop position for Rnor_6.0 assembly
            writeGenomicPositions(rec.mdRnor6, writer);
        }
        else if( speciesType==SpeciesType.HUMAN ) {

        }
        else if( speciesType==SpeciesType.MOUSE ) {
            writer.append(checkNull(rec.mgdID))
                  .append('\t')
                  .append(rec.cmPos!=null ? Double.toString(rec.cmPos) : "");
        }

        // terminate the line
        writer.println();
    }

    // concatenate all sslp aliases using ';' as separator
    String getAliases(int rgdId) throws Exception {
        List<Alias> aliases = getDao().getAliases(rgdId);
        return Utils.concatenate(";", aliases, "getValue");
    }

    // print chromosome, start and stop position
    void writeGenomicPositions(List<MapData> mds, PrintWriter writer) throws Exception {
        // if there is no data
        if( mds==null || mds.isEmpty() ) {
            writer.print("\t\t\t");
            return;
        }

        // print chromosomes
        writer.print(checkNull(Utils.concatenate(";", mds, "getChromosome")));
        writer.print('\t');

        // print start pos
        writer.print(checkNull(Utils.concatenate(";", mds, "getStartPos")));
        writer.print('\t');

        // print stop pos
        writer.print(checkNull(Utils.concatenate(";", mds, "getStopPos")));
        writer.print('\t');
    }

    // print chromosome and absolute position(s)
    void writeGeneticPositions(MapData md, PrintWriter writer) throws Exception {
        // if there is no data
        if( md==null ) {
            writer.print("\t\t");
            return;
        }

        // print chromosome
        if( md.getChromosome()!=null )
            writer.print(checkNull(md.getChromosome()));
        writer.print('\t');

        // print absolute pos
        if( md.getAbsPosition()!=null )
            writer.print(md.getAbsPosition());
        writer.print('\t');
    }

    private String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

    List<XdbId> getXdbIdList(int rgdId) throws Exception {
        return getDao().getXdbIds(rgdId);
    }

    public void setQcThreadCount(int qcThreadCount) {
        this.qcThreadCount = qcThreadCount;
    }

    public int getQcThreadCount() {
        return qcThreadCount;
    }

    class SslpRecord extends PipelineRecord {

        public SSLP sslp;
        public Gene gene;
        public String[] chrAndFishBand;
        public String sslpAliases;
        public String uncuratedPubmedID;
        public String uniSTSID;
        public String genBankNucleotideID;
        public String uniGeneIDs;

        public String curatedRefRGDIDs;
        public String curatedRefPubmedIDs;

        public List<MapData> mdCelera;
        public List<MapData> mdNewRef;
        public List<MapData> mdOldRef;
        public List<MapData> mdRnor6;// Rnor_6.0 for rat

        public List<XdbId> xdbIds;

        // rat specific
        public MapData mdFHHxACI;
        public MapData mdSHRSPxBN;
        public MapData mdRH_2_0;
        public MapData mdRH_3_4;

        // human specific

        // mouse specific
        public String mgdID;
        public Double cmPos;

        // we are storing list of all positional information here to reduce number of calls to database
        public List<MapData> mds; // list of all positional data

        public List<MapData> getMapData(int mapKey) {

            List<MapData> mdList = new ArrayList<MapData>();
            for( MapData md: mds ) {
                if( mapKey == md.getMapKey() )
                    mdList.add(md);
            }
            return mdList;
        }

        public String getAccessionIds(int xdbKey) {

            if( xdbIds==null || xdbIds.isEmpty() )
                return "";

            List<String> accIds = new ArrayList<String>();
            for( XdbId xdbId: xdbIds ) {
                if( xdbKey == xdbId.getXdbKey() && xdbId.getAccId()!=null )
                    accIds.add(xdbId.getAccId());
            }
            return Utils.concatenate(accIds, FtpFileExtractsManager.MULTIVAL_SEPARATOR);
        }

        public String getLinkText(int xdbKey) {

            if( xdbIds==null || xdbIds.isEmpty() )
                return "";

            List<String> accIds = new ArrayList<String>();
            for( XdbId xdbId: xdbIds ) {
                if( xdbKey == xdbId.getXdbKey() )
                    accIds.add(xdbId.getLinkText());
            }
            return Utils.concatenate(accIds, FtpFileExtractsManager.MULTIVAL_SEPARATOR);
        }
    }
}
