package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mtutaj
 * @since Nov 29, 2010
 */
public class SslpExtractor extends BaseExtractor {

    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: markers  build 2021-04-30\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# markers extracted from RGD database\n"
    +"# CONTACT: rgd.developers@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"#  Where a marker has multiple positions for a single map/assembly\n"
    +"#  (f.e. D1Arb36 has positions 78134192..78134522 and 78153000..78153323 on ref assembly)\n"
    +"#   is how it is presented in the columns CHROMOSOME_3.4, START_POS_3.4 and STOP_POS_3.4:\n"
    +"#   1;1  <tab>   78134192;78153000   <tab>  78134522;78153323\n"
    +"#\n"
    +"### Apr 1, 2011 RATMAP_IDs and RHDB_IDs are discontinued.\n"
    +"### Jul 1, 2011 fixed generation of UNCURATED_REF_PUBMED_IDs.\n"
    +"### Nov 23, 2011 no format changes (UniGene Ids are extracted from db in different way).\n"
    +"### Dec 19 2011 no data changes; improved internal QC.\n"
    +"### May 31 2012 no data changes; (optimized retrieval of maps data from database)\n"
    +"### Oct 22 2012 fixed export of positional information for mouse (positions on assembly build 38 were exported as positions on assembly 37).\n"
    +"### Nov 20 2012 rat: positions on assembly map 3.1 are no longer exported; instead position on assembly 5.0 are exported.\n"
    +"### Dec 26 2013 no data changes; improved internal QC.\n"
    +"### Sep 8 2014 rat: available positions on assembly Rnor_6.0.\n"
    +"### Oct 29 2018 discontinued columns #8 CLONE_SEQ_RGD_ID and #10 PRIMER_SEQ_RGD_ID. Column #8 now shows marker type.\n"
    +"### Nov 1 2018 renamed columns SSLP_RGD_ID => MARKER_RGD_ID, SSLP_SYMBOL => MARKER_SYMBOL, SSLP_TYPE => MARKER_TYPE.\n"
    +"### Jun 17 2019 data sorted by RGD ID; files exported into species specific directories\n"
    +"### Jan 19 2021 discontinued column 15 with UniGene IDs\n"
    +"### Apr 30 2021 added export of positions for new rat assembly mRatBN7.2\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 23 columns are in common between rat, mouse and human)\n"
    +"#\n"
    +"#1   MARKER_RGD_ID	       RGD_ID of the marker\n"
    +"#2   SPECIES                 species name\n"
    +"#3   MARKER_SYMBOL           marker symbol\n"
    +"#4   EXPECTED_SIZE           marker expected size (PCR product size)\n"
    +"#5   CURATED_REF_RGD_ID      RGD_ID of paper(s) about marker\n"
    +"#6   CURATED_REF_PUBMED_ID   PUBMED_ID of paper(s) about marker\n"
    +"#7   UNCURATED_REF_PUBMED_ID other PUBMED_IDs\n"
    +"#8   MARKER_TYPE             marker type, if available\n"
    +"#9   CLONE_SEQUENCE          clone sequence itself\n"
    +"#10  (UNUSED)\n"
    +"#11  FORWARD_SEQ             forward sequence\n"
    +"#12  REVERSE_SEQ             reverse sequence\n"
    +"#13  UNISTS_ID               UniSTS ID\n"
    +"#14  GENBANK_NUCLEOTIDE      GenBank Nucleotide ID(s)\n"
    +"#15  (UNUSED)\n"
    +"#16  ALIAS_VALUE             known aliases for this marker\n"
    +"#17  ASSOCIATED_GENE_RGD_ID  RGD_IDs for gene associated with this marker\n"
    +"#18  ASSOCIATED_GENE_SYMBOL  symbol for gene associated with this marker\n"
    +"#19  CHROMOSOME              chromosome\n"
    +"#20  FISH_BAND               fish band\n"
    +"#21  CHROMOSOME_CELERA       chromosome for Celera assembly\n"
    +"#22  START_POS_CELERA        start position for Celera assembly\n"
    +"#23  STOP_POS_CELERA         stop position for Celera assembly\n";

     final String HEADER_LINE_RAT =
     "#24  CHROMOSOME_5.0          chromosome for Rnor_5.0 assembly\n"
    +"#25  START_POS_5.0           start position for Rnor_5.0 assembly\n"
    +"#26  STOP_POS_5.0            stop position for Rnor_5.0 assembly\n"
    +"#27  CHROMOSOME_3.4          chromosome for RGSC_v3.4 assembly\n"
    +"#28  START_POS_3.4           start position for RGSC_v3.4 assembly\n"
    +"#29  STOP_POS_3.4            stop position for RGSC_v3.4 assembly\n"
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
    +"#40  CHROMOSOME_6.0          chromosome for Rnor_6.0 assembly\n"
    +"#41  START_POS_6.0           start position for Rnor_6.0 assembly\n"
    +"#42  STOP_POS_6.0            stop position for Rnor_6.0 assembly\n"
    +"#43  CHROMOSOME_7.2          chromosome for mRatBN7.2 assembly\n"
    +"#44  START_POS_7.2           start position for mRatBN7.2 assembly\n"
    +"#45  STOP_POS_7.2            stop position for mRatBN7.2 assembly\n"
    +"#\n"
    +"MARKER_RGD_ID\tSPECIES\tMARKER_SYMBOL\tEXPECTED_SIZE\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\t"
    +"UNCURATED_REF_PUBMED_ID\tMARKER_TYPE\tCLONE_SEQUENCE\t(UNUSED)\tFORWARD_SEQ\tREVERSE_SEQ\t"
    +"UNISTS_ID\tGENBANK_NUCLEOTIDE\t(UNUSED)\tALIAS_VALUE\t"
    +"ASSOCIATED_GENE_RGD_ID\tASSOCIATED_GENE_SYMBOL\tCHROMOSOME\tFISH_BAND\t"
    +"CHROMOSOME_CELERA\tSTART_POS_CELERA\tSTOP_POS_CELERA\t"
    +"CHROMOSOME_5.0\tSTART_POS_5.0\tSTOP_POS_5.0\t"
    +"CHROMOSOME_3.4\tSTART_POS_3.4\tSTOP_POS_3.4\t"
    +"(UNUSED)\t(UNUSED)\t"
    +"CHR_FHHxACI\tPOS_FHHxACI\tCHR_SHRSPxBN\tPOS_SHRSPxBN\tCHR_RH_2.0\tPOS_RH_2.0\tCHR_RH_3.4\tPOS_RH_3.4\t"
    +"CHROMOSOME_6.0\tSTART_POS_6.0\tSTOP_POS_6.0\tCHROMOSOME_7.2\tSTART_POS_7.2\tSTOP_POS_7.2";

    final String HEADER_LINE_HUMAN =
    "#24  CHROMOSOME_37          chromosome for the current reference assembly v.37\n"
   +"#25  START_POS_37           start position for current reference assembly v.37\n"
   +"#26  STOP_POS_37            stop position for current reference assembly v.37\n"
   +"#27  CHROMOSOME_36          chromosome for the old reference assembly v.36\n"
   +"#28  START_POS_36           start position for old reference assembly v.36\n"
   +"#29  STOP_POS_36            stop position for old reference assembly v.36\n"
   +"#\n"
   +"MARKER_RGD_ID\tSPECIES\tMARKER_SYMBOL\tEXPECTED_SIZE\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\t"
   +"UNCURATED_REF_PUBMED_ID\tMARKER_TYPE\tCLONE_SEQUENCE\t(UNUSED)\tFORWARD_SEQ\tREVERSE_SEQ\t"
   +"UNISTS_ID\tGENBANK_NUCLEOTIDE\t(UNUSED)\tALIAS_VALUE\t"
   +"ASSOCIATED_GENE_RGD_ID\tASSOCIATED_GENE_SYMBOL\tCHROMOSOME\tFISH_BAND\t"
   +"CHROMOSOME_CELERA\tSTART_POS_CELERA\tSTOP_POS_CELERA\t"
   +"CHROMOSOME_37\tSTART_POS_37\tSTOP_POS_37\t"
   +"CHROMOSOME_36\tSTART_POS_36\tSTOP_POS_36";

    final String HEADER_LINE_MOUSE =
    "#24  CHROMOSOME_37          chromosome for the current reference assembly v.37\n"
   +"#25  START_POS_37           start position for current reference assembly v.37\n"
   +"#26  STOP_POS_37            stop position for current reference assembly v.37\n"
   +"#27  (UNUSED)\n"
   +"#28  (UNUSED)\n"
   +"#29  (UNUSED)\n"
   +"#30  MGD_ID                 MGD ID\n"
   +"#31  CM_POS                 mouse cM map absolute position\n"
   +"#\n"
   +"MARKER_RGD_ID\tSPECIES\tMARKER_SYMBOL\tEXPECTED_SIZE\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID\t"
   +"UNCURATED_REF_PUBMED_ID\tMARKER_TYPE\tCLONE_SEQUENCE\t(UNUSED)\tFORWARD_SEQ\tREVERSE_SEQ\t"
   +"UNISTS_ID\tGENBANK_NUCLEOTIDE\t(UNUSED)\tALIAS_VALUE\t"
   +"ASSOCIATED_GENE_RGD_ID\tASSOCIATED_GENE_SYMBOL\tCHROMOSOME\tFISH_BAND\t"
   +"CHROMOSOME_CELERA\tSTART_POS_CELERA\tSTOP_POS_CELERA\t"
   +"CHROMOSOME_37\tSTART_POS_37\tSTOP_POS_37\t"
   +"(UNUSED)\t(UNUSED)\t(UNUSED)\t"
   +"MGD_ID\tCM_POS";

    Logger log = LogManager.getLogger("sslp");

    public void run(SpeciesRecord speciesInfo) throws Exception {

        final SpeciesRecord speciesRec = speciesInfo;
        String outputFile = speciesInfo.getMarkerFileName();
        if( outputFile==null )
            return;

        long time0 = System.currentTimeMillis();

        // create species specific output dir
        outputFile = getSpeciesSpecificExtractDir(speciesInfo)+'/'+outputFile;

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


        List<MarkerRecord> markers = getMarkerList(speciesType);

        final java.util.Map<Integer, String> lineMap = new ConcurrentHashMap<>(markers.size());

        markers.parallelStream().forEach( rec -> {
            try {

                rec.gene = dao.getGeneByMarkerKey(rec.marker.getKey());
                int markerRgdID = rec.marker.getRgdId();

                // get chromosome and fish band information for given marker
                rec.mds = dao.getMapData(markerRgdID);
                rec.chrAndFishBand = dao.getChromosomeAndFishBand(rec.mds, speciesRec.getCytoMapKey());

                // get other maps
                List<MapData> mds = rec.getMapData(speciesRec.getCeleraAssemblyMapKey());
                if( mds.size()>0 ) {
                    rec.mdCelera = mds;
                }
                mds = rec.getMapData(speciesRec.getNewRefAssemblyMapKeyForMarkers());
                if( mds.size()>0 ) {
                    rec.mdNewRef = mds;
                }
                mds = rec.getMapData(speciesRec.getOldRefAssemblyMapKeyForMarkers());
                if( mds.size()>0 ) {
                    rec.mdOldRef = mds;
                }

                // get marker aliases (separated by ';' if there are multiple)
                rec.aliases = getAliases(markerRgdID);

                rec.xdbIds = getXdbIdList(markerRgdID);

                rec.uncuratedPubmedID = getDao().getUncuratedPubmedIds(markerRgdID);
                rec.uniSTSID = rec.getAccessionIds(XdbId.XDB_KEY_UNISTS);
                rec.genBankNucleotideID = rec.getAccessionIds(XdbId.XDB_KEY_GENEBANKNU);

                rec.curatedRefRGDIDs = getDao().getCuratedRefs(markerRgdID);
                rec.curatedRefPubmedIDs = getDao().getCuratedPubmedIds(markerRgdID);

                if( speciesType==SpeciesType.RAT ) {
                    // FHH map
                    mds = rec.getMapData(1);
                    if( mds.size()>0 ) {
                        rec.mdFHHxACI = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for FHH map: marker_rgd_id=" + markerRgdID);
                    }

                    // SHRSP map
                    mds = rec.getMapData(2);
                    if( mds.size()>0 ) {
                        rec.mdSHRSPxBN = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for SHRSP map: marker_rgd_id=" + markerRgdID);
                    }

                    // RH 2.0 map
                    mds = rec.getMapData(3);
                    if( mds.size()>0 ) {
                        rec.mdRH_2_0 = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for RH 2.0 map: marker_rgd_id=" + markerRgdID);
                    }

                    // RH 3.4 map
                    mds = rec.getMapData(5);
                    if( mds.size()>0 ) {
                        rec.mdRH_3_4 = mds.get(0);
                        if( mds.size()>1 )
                            log.debug("multiple positions for RH 3.4 map: marker_rgd_id=" + markerRgdID);
                    }

                    mds = rec.getMapData(360);
                    if( mds.size()>0 ) {
                        rec.mdRnor6 = mds;
                    }

                    mds = rec.getMapData(372);
                    if( mds.size()>0 ) {
                        rec.md7_2 = mds;
                    }
                }
                else if( speciesType==SpeciesType.MOUSE ) {
                    mds = rec.getMapData(speciesRec.getCmMapKey());
                    if( mds.size()>0 ) {
                        rec.cmPos = mds.get(0).getAbsPosition();
                        if( mds.size()>1 )
                            log.debug("multiple positions for cM map marker_rgd_id rgd_Id=" + markerRgdID);
                    }
                    rec.mgdID = rec.getAccessionIds(XdbId.XDB_KEY_MGD);
                }
                else if( speciesType==SpeciesType.HUMAN ) {
                }

                lineMap.put(rec.marker.getRgdId(), generateLine(rec, speciesType));

            } catch( Exception e ) {
                throw new RuntimeException(e);
            }
        });

        writeDataLines(writer, lineMap);

        // close the output file
        writer.close();

        System.out.println(getVersion()+" - "+speciesInfo.getSpeciesName() + "  - data lines written: "+lineMap.size()+",  elapsed "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));

        // copy the output file to the staging area
        FtpFileExtractsManager.qcFileContent(outputFile, "markers", speciesType);
    }

    List<MarkerRecord> getMarkerList(int speciesType) throws Exception {

        List<SSLP> markersInRgd = getDao().getActiveMarkers(speciesType);
        List<MarkerRecord> records = new ArrayList<>(markersInRgd.size());

        for( SSLP marker: markersInRgd ) {
            MarkerRecord rec = new MarkerRecord();
            rec.marker = marker;
            records.add(rec);
        }

        return records;
    }

    String generateLine(MarkerRecord rec, int speciesType) throws Exception {

        StringBuilder buf = new StringBuilder();

        // MARKER_RGD_ID
        buf.append(rec.marker.getRgdId());
        buf.append('\t');
        // species name
        buf.append(SpeciesType.getCommonName(speciesType).toLowerCase());
        buf.append('\t');
        // marker symbol
        buf.append(checkNull(rec.marker.getName()));
        buf.append('\t');
        // expected size
        if (rec.marker.getExpectedSize() != 0) {
            buf.append(rec.marker.getExpectedSize());
        }
        buf.append('\t');

        buf.append(checkNull(rec.curatedRefRGDIDs));
        buf.append('\t');

        buf.append(checkNull(rec.curatedRefPubmedIDs));
        buf.append('\t');

        buf.append(checkNull(rec.uncuratedPubmedID));
        buf.append('\t');

        buf.append(checkNull(rec.marker.getSslpType()));
        buf.append('\t');

        buf.append(checkNull(rec.marker.getTemplateSeq()));
        buf.append('\t');

        // primer rgd ids -- discontinued
        buf.append('\t');

        buf.append(checkNull(rec.marker.getForwardSeq()));
        buf.append('\t');

        buf.append(checkNull(rec.marker.getReverseSeq()));
        buf.append('\t');

        buf.append(checkNull(rec.uniSTSID));
        buf.append('\t');

        buf.append(checkNull(rec.genBankNucleotideID));
        buf.append('\t');

        // UniGene Ids discontinued
        buf.append('\t');

        buf.append(checkNull(rec.aliases));
        buf.append('\t');

        // associated gene RGDID
        if( rec.gene != null ) {
            buf.append(rec.gene.getRgdId());
        }
        buf.append('\t');

        // associated gene symbol
        if( rec.gene != null ) {
            buf.append(checkNull(rec.gene.getSymbol()));
        }
        buf.append('\t');

        // print chromosome
        if( rec.chrAndFishBand[0]!=null ) {
            buf.append(checkNull(rec.chrAndFishBand[0]));
        }
        buf.append('\t');

        // print fish band
        if( rec.chrAndFishBand[1]!=null ) {
            buf.append(checkNull(rec.chrAndFishBand[1]));
        }
        buf.append('\t');

        // print chromosome, start and stop position for celera
        writeGenomicPositions(rec.mdCelera, buf);

        // print chromosome, start and stop position for primary reference assembly
        writeGenomicPositions(rec.mdNewRef, buf);

        // print chromosome, start and stop position for old reference assembly
        writeGenomicPositions(rec.mdOldRef, buf);

        if( speciesType==SpeciesType.RAT ) {

            buf.append("\t\t"); // two unused columns

            // write chromosome and absolute pos for FHH map
            writeGeneticPositions(rec.mdFHHxACI, buf);

            // write chromosome and absolute pos for SHRSP map
            writeGeneticPositions(rec.mdSHRSPxBN, buf);

            // write chromosome and absolute pos for RH 2.0 map
            writeGeneticPositions(rec.mdRH_2_0, buf);

            // write chromosome and absolute pos for RH 3.4 map
            writeGeneticPositions(rec.mdRH_3_4, buf);

            // print chromosome, start and stop position for Rnor_6.0 assembly
            writeGenomicPositions(rec.mdRnor6, buf);

            // print chromosome, start and stop position for mRatBN7.2 assembly
            writeGenomicPositions(rec.md7_2, buf);
        }
        else if( speciesType==SpeciesType.HUMAN ) {

        }
        else if( speciesType==SpeciesType.MOUSE ) {
            buf.append(checkNull(rec.mgdID))
                  .append('\t')
                  .append(rec.cmPos!=null ? Double.toString(rec.cmPos) : "");
        }

        // terminate the line
        buf.append("\n");

        return buf.toString();
    }

    // concatenate all marker aliases using ';' as separator
    String getAliases(int rgdId) throws Exception {
        List<Alias> aliases = getDao().getAliases(rgdId);
        return Utils.concatenate(";", aliases, "getValue");
    }

    // print chromosome, start and stop position
    void writeGenomicPositions(List<MapData> mds, StringBuilder buf) throws Exception {
        // if there is no data
        if( mds==null || mds.isEmpty() ) {
            buf.append("\t\t\t");
            return;
        }

        // print chromosomes
        buf.append(checkNull(Utils.concatenate(";", mds, "getChromosome")));
        buf.append('\t');

        // print start pos
        buf.append(checkNull(Utils.concatenate(";", mds, "getStartPos")));
        buf.append('\t');

        // print stop pos
        buf.append(checkNull(Utils.concatenate(";", mds, "getStopPos")));
        buf.append('\t');
    }

    // print chromosome and absolute position(s)
    void writeGeneticPositions(MapData md, StringBuilder buf) throws Exception {
        // if there is no data
        if( md==null ) {
            buf.append("\t\t");
            return;
        }

        // print chromosome
        if( md.getChromosome()!=null )
            buf.append(checkNull(md.getChromosome()));
        buf.append('\t');

        // print absolute pos
        if( md.getAbsPosition()!=null )
            buf.append(md.getAbsPosition());
        buf.append('\t');
    }

    private String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

    List<XdbId> getXdbIdList(int rgdId) throws Exception {
        return getDao().getXdbIds(rgdId);
    }

    class MarkerRecord {

        public SSLP marker;
        public Gene gene;
        public String[] chrAndFishBand;
        public String aliases;
        public String uncuratedPubmedID;
        public String uniSTSID;
        public String genBankNucleotideID;

        public String curatedRefRGDIDs;
        public String curatedRefPubmedIDs;

        public List<MapData> mdCelera;
        public List<MapData> mdNewRef;
        public List<MapData> mdOldRef;
        public List<MapData> mdRnor6;// Rnor_6.0 for rat
        public List<MapData> md7_2;// mRatBN7.2

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
