package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
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
public class QtlExtractor  extends BaseExtractor {

    static boolean versionPrintedOut = false;

    public void run(SpeciesRecord speciesRec) throws Exception {

        String outputFile = speciesRec.getQtlFileName();
        if( outputFile==null )
            return;

        synchronized (ArrayIdExtractor.class) {
            if (!versionPrintedOut) {
                System.out.println(getVersion());
                versionPrintedOut = true;
            }
        }

        run(speciesRec, getSpeciesSpecificExtractDir(speciesRec));
    }

    final String HEADER_COMMON_LINES = """
        # RGD-PIPELINE: ftp-file-extracts
        # MODULE: qtls  build Jan 24, 2025
        # GENERATED-ON: #DATE#
        # PURPOSE: information about active #SPECIES# qtls extracted from RGD database
        # CONTACT: rgd.developers@mcw.edu
        # FORMAT: tab delimited text
        # NOTES: multiple values in a single column are separated by ';'
        #
        #CHANGELOG#
        #
        #COLUMN INFORMATION:
        # (First 25 columns are in common between rat, mouse and human)
        #
        #1  QTL_RGD_ID             the RGD_ID of the QTL
        #2  SPECIES                species name
        #3  QTL_SYMBOL             official qtl symbol
        #4  QTL_NAME               current qtl name
        #5  CHROMOSOME_FROM_REF    the chromosome from the original paper
        #6  LOD                    maximum LOD score if given in paper
        #7  P_VALUE                p-value for QTL if given in paper
        #8  VARIANCE               variance if given in paper
        #9  FLANK_1_RGD_ID         RGD_ID for flank marker 1, if in paper
        #10 FLANK_1_SYMBOL         symbol for flank marker 1, if in paper
        #11 FLANK_2_RGD_ID         RGD_ID for flank marker 2, if in paper
        #12 FLANK_2_SYMBOL         symbol for flank marker 2, if in paper
        #13 PEAK_RGD_ID            RGD_ID for peak marker, if in paper
        #14 PEAK_MARKER_SYMBOL     symbol for peak marker, if in paper
        #15 TRAIT_NAME             trait created for QTL
        #16 MEASUREMENT_TYPE       measurement type for QTL
        #17 (UNUSED)
        #18 PHENOTYPES             phenotype ontology annotation
        #19 ASSOCIATED_DISEASES    diseases ontology annotation
        #20 CURATED_REF_RGD_ID     RGD_ID of paper(s) on QTL
        #21 CURATED_REF_PUBMED_ID  PUBMED_ID of paper(s) on QTL
        #22 CANDIDATE_GENE_RGD_IDS RGD_IDS for genes mentioned by paper author
        #23 CANDIDATE_GENE_SYMBOLS symbols for genes mentioned by paper author
        #24 INHERITANCE_TYPE       dominant, recessive etc.
        #25 RELATED_QTLS	       symbols of related QTLS
        """;

    final String HEADER_RAT_CHANGELOG = """
        ### Apr  1 2011  RATMAP_IDs and RHDB_IDs are discontinued
        ### Nov 20 2012  positions on assembly map 3.1 are no longer exported; instead position on assembly 5.0 are exported
        ### Apr 18 2013  crossed strains are reported in separate columns for easier sorting
        ### Sep 09 2014  added positions on Rnor_6.0 assembly
        ### Jan 08 2016  TRAIT_METHODOLOGY column discontinued; column #16 SUBTRAIT_NAME is now called MEASUREMENT_TYPE
        ### Jan 11 2016  added columns STRAIN_RGD_ID3 and STRAIN_RGD_SYMBOL3 for qtls that have 3+ crossed strains
        ### Jun 17 2019  data sorted by RGD ID; files exported into species specific directories
        ### Oct 26 2022  added positions on rn7 assembly
        ### Jan 24 2025  added positions on GRCr8 assembly
        """;

    final String HEADER_MOUSE_CHANGELOG = """
        ### Oct 22 2012  fixed export of positional information (positions on assembly build 38 were exported as positions on assembly 37)
        ### Jul 16 2014  added generation of file QTLS_MOUSE_B38.txt with positions on assemblies 38 and 37
        ### Sep 09 2014  QTLS_MOUSE_B38.txt discontinued; 4 columns added to QTLS_MOUSE.txt file to accommodate assembly 38 positions
        ### Jan 08 2016  TRAIT_METHODOLOGY column discontinued; column #16 SUBTRAIT_NAME is now called MEASUREMENT_TYPE
        ### Jun 17 2019  data sorted by RGD ID; files exported into species specific directories
        ### Jan 24 2025  positions of assembly 34 discontinued; instead positions on GRCm39 assembly are exported
        """;

    final String HEADER_HUMAN_CHANGELOG = """
        ### Oct 23 2012  fixed description of columns for human qtls
        ### Jan 08 2016  TRAIT_METHODOLOGY column discontinued; column #16 SUBTRAIT_NAME is now called MEASUREMENT_TYPE
        ### Jun 17 2019  data sorted by RGD ID; files exported into species specific directories
        ### Jan 24 2025  added positions on GRCh8 assembly
        """;

    // column common for all species
    final String HEADER_COMMON = "QTL_RGD_ID\tSPECIES\tQTL_SYMBOL\tQTL_NAME\tCHROMOSOME_FROM_REF"
        +"\tLOD\tP_VALUE\tVARIANCE\tFLANK_1_RGD_ID\tFLANK_1_SYMBOL\tFLANK_2_RGD_ID\tFLANK_2_SYMBOL"
        +"\tPEAK_RGD_ID\tPEAK_MARKER_SYMBOL\tTRAIT_NAME\tMEASUREMENT_TYPE\t(UNUSED)"
        +"\tPHENOTYPES\tASSOCIATED_DISEASES\tCURATED_REF_RGD_ID\tCURATED_REF_PUBMED_ID"
        +"\tCANDIDATE_GENE_RGD_IDS\tCANDIDATE_GENE_SYMBOLS\tINHERITANCE_TYPE\tRELATED_QTLS";

    // additional columns for rat
    final String HEADER_RAT =
        "#26 (UNUSED)\n"
        +"#27 5.0_MAP_POS_CHR     chromosome for assembly Rnor_5.0\n"
        +"#28 5.0_MAP_POS_START   start position for assembly Rnor_5.0\n"
        +"#29 5.0_MAP_POS_STOP    stop position for assembly Rnor_5.0\n"
        +"#30 5.0_MAP_POS_METHOD  qtl positioning method for assembly Rnor_5.0\n"
        +"#31 3.4_MAP_POS_CHR     chromosome for assembly RGSC 3.4\n"
        +"#32 3.4_MAP_POS_START   start position for assembly RGSC 3.4\n"
        +"#33 3.4_MAP_POS_STOP    stop position for assembly RGSC 3.4\n"
        +"#34 3.4_MAP_POS_METHOD  qtl positioning method for assembly RGSC 3.4\n"
        +"#35 CROSS_TYPE          strain cross type\n"
        +"#36 CROSS_PAIR          pairing of strains for cross\n"
        +"#37 STRAIN_RGD_ID1      RGD_ID of first strain crossed\n"
        +"#38 STRAIN_RGD_ID2      RGD_ID of second strain crossed\n"
        +"#39 STRAIN_RGD_SYMBOL1  symbol of first strain crossed\n"
        +"#40 STRAIN_RGD_SYMBOL2  symbol of second strain crossed\n"
        +"#41 6.0_MAP_POS_CHR     chromosome for assembly Rnor_6.0\n"
        +"#42 6.0_MAP_POS_START   start position for assembly Rnor_6.0\n"
        +"#43 6.0_MAP_POS_STOP    stop position for assembly Rnor_6.0\n"
        +"#44 6.0_MAP_POS_METHOD  qtl positioning method for assembly Rnor_6.0\n"
        +"#45 STRAIN_RGD_ID3      RGD_ID of third strain crossed\n"
        +"#46 STRAIN_RGD_SYMBOL3  symbol of third strain crossed\n"
        +"#47 7.2_MAP_POS_CHR     chromosome for assembly mRatBN7.2\n"
        +"#48 7.2_MAP_POS_START   start position for assembly mRatBN7.2\n"
        +"#49 7.2_MAP_POS_STOP    stop position for assembly mRatBN7.2\n"
        +"#50 7.2_MAP_POS_METHOD  qtl positioning method for assembly mRatBN7.2\n"
        +"#51 8_MAP_POS_CHR       chromosome for assembly GRCr8\n"
        +"#52 8_MAP_POS_START     start position for assembly GRCr8\n"
        +"#53 8_MAP_POS_STOP      stop position for assembly GRCr8\n"
        +"#54 8_MAP_POS_METHOD    qtl positioning method for assembly GRCr8\n"
        +"#\n#COLUMNS#"
        +"\t(UNUSED)\t5.0_MAP_POS_CHR\t5.0_MAP_POS_START\t5.0_MAP_POS_STOP\t5.0_MAP_POS_METHOD"
        +"\t3.4_MAP_POS_CHR\t3.4_MAP_POS_START\t3.4_MAP_POS_STOP\t3.4_MAP_POS_METHOD"
        +"\tCROSS_TYPE\tCROSS_PAIR\tSTRAIN_RGD_ID1\tSTRAIN_RGD_ID2\tSTRAIN_RGD_SYMBOL1\tSTRAIN_RGD_SYMBOL2"
        +"\t6.0_MAP_POS_CHR\t6.0_MAP_POS_START\t6.0_MAP_POS_STOP\t6.0_MAP_POS_METHOD\tSTRAIN_RGD_ID3\tSTRAIN_RGD_SYMBOL3"
        +"\t7.2_MAP_POS_CHR\t7.2_MAP_POS_START\t7.2_MAP_POS_STOP\t7.2_MAP_POS_METHOD\t"
        +"\t8_MAP_POS_CHR\t8_MAP_POS_START\t8_MAP_POS_STOP\t8_MAP_POS_METHOD";

    // additional columns for mouse
    final String HEADER_MOUSE =
        "#26 MGI_ID             MGI ID\n"
        +"#27 37_MAP_POS_CHR     chromosome for assembly MGSCv37\n"
        +"#28 37_MAP_POS_START   start position for assembly MGSCv37\n"
        +"#29 37_MAP_POS_STOP    stop position for assembly MGSCv37\n"
        +"#30 37_MAP_POS_METHOD  qtl positioning method for assembly MGSCv37\n"
        +"#31 39_MAP_POS_CHR     chromosome for old assembly GRCm39\n"
        +"#32 39_MAP_POS_START   start position for old assembly GRCm39\n"
        +"#33 39_MAP_POS_STOP    stop position for old assembly GRCm39\n"
        +"#34 39_MAP_POS_METHOD  qtl positioning method for GRCm39\n"
        +"#35 CM_MAP_CHR         chromosome on cM map\n"
        +"#36 CM_MAP_POS         absolute position on cM map\n"
        +"#37 38_MAP_POS_CHR     chromosome for assembly GRCm38\n"
        +"#38 38_MAP_POS_START   start position for assembly GRCm38\n"
        +"#39 38_MAP_POS_STOP    stop position for assembly GRCm38\n"
        +"#40 38_MAP_POS_METHOD  qtl positioning method for assembly GRCm38\n"
        +"#\n#COLUMNS#"
        +"\tMGI_ID\t37_MAP_POS_CHR\t37_MAP_POS_START\t37_MAP_POS_STOP\t37_MAP_POS_METHOD"
        +"\t39_MAP_POS_CHR\t39_MAP_POS_START\t39_MAP_POS_STOP\t39_MAP_POS_METHOD"
        +"\tCM_MAP_CHR\tCM_MAP_POS"
        +"\t38_MAP_POS_CHR\t38_MAP_POS_START\t38_MAP_POS_STOP\t38_MAP_POS_METHOD";

    // additional columns for human
    final String HEADER_HUMAN =
        "#26 OMIM_ID            OMIM ID\n"
        +"#27 37_MAP_POS_CHR     chromosome for current assembly GRCh37\n"
        +"#28 37_MAP_POS_START   start position for current assembly GRCh37\n"
        +"#29 37_MAP_POS_STOP    stop position for current assembly GRCh37\n"
        +"#30 37_MAP_POS_METHOD  qtl positioning method for current assembly GRCh37\n"
        +"#31 36_MAP_POS_CHR     chromosome for assembly NCBI36\n"
        +"#32 36_MAP_POS_START   start position for assembly NCBI36\n"
        +"#33 36_MAP_POS_STOP    stop position for assembly NCBI36\n"
        +"#34 36_MAP_POS_METHOD  qtl positioning method for assembly NCBI36\n"
        +"#35 38_MAP_POS_CHR     chromosome for assembly GRCh38\n"
        +"#36 38_MAP_POS_START   start position for assembly GRCh38\n"
        +"#37 38_MAP_POS_STOP    stop position for assembly GRCh38\n"
        +"#38 38_MAP_POS_METHOD  qtl positioning method for assembly GRCh38\n"
        +"#\n#COLUMNS#"
        +"\tOMIM_ID\t37_MAP_POS_CHR\t37_MAP_POS_START\t37_MAP_POS_STOP\t37_MAP_POS_METHOD"
        +"\t36_MAP_POS_CHR\t36_MAP_POS_START\t36_MAP_POS_STOP\t36_MAP_POS_METHOD"
        +"\t38_MAP_POS_CHR\t38_MAP_POS_START\t38_MAP_POS_STOP\t38_MAP_POS_METHOD";

    FtpFileExtractsDAO dao;
    SpeciesRecord species;

    Logger log = LogManager.getLogger("qtl");

    /** examine all qtls and export them into tab separated files
     *
     * @param species
     * @param tmpDir
     * @throws Exception
     */
    public void run(final SpeciesRecord species, String tmpDir) throws Exception {

        long time0 = System.currentTimeMillis();

        this.species = species;
        final int speciesType = species.getSpeciesType();

        // open output file
        String fileName = tmpDir+'/'+species.getQtlFileName();
        final PrintWriter writer = new PrintWriter(fileName);

        String header, speciesSpecificChangeLog;
        switch (speciesType) {
            case SpeciesType.HUMAN -> {
                header = HEADER_HUMAN;
                speciesSpecificChangeLog = HEADER_HUMAN_CHANGELOG;
            }
            case SpeciesType.MOUSE -> {
                header = HEADER_MOUSE;
                speciesSpecificChangeLog = HEADER_MOUSE_CHANGELOG;
            }
            case SpeciesType.RAT -> {
                header = HEADER_RAT;
                speciesSpecificChangeLog = HEADER_RAT_CHANGELOG;
            }
            default -> {
                header = "";
                speciesSpecificChangeLog = "";
            }
        }

        // write the header
        String commonLines = HEADER_COMMON_LINES
                .replace("#SPECIES#", species.getSpeciesName())
                .replace("#DATE#", SpeciesRecord.getTodayDate())
                .replace("#CHANGELOG#", speciesSpecificChangeLog);
        writer.println(commonLines);

        writer.println(header.replace("#COLUMNS#", HEADER_COMMON));

        List<QTL> inRgdQtls = dao.getActiveQtls(speciesType);
        List<QtlRecord> qtls = new ArrayList<>(inRgdQtls.size());
        for( QTL qtl: inRgdQtls ) {
            QtlRecord rec = new QtlRecord();
            rec.qtl = qtl;
            qtls.add(rec);
        }

        final java.util.Map<Integer, String> lineMap = new ConcurrentHashMap<>(qtls.size());

        qtls.parallelStream().forEach( rec -> {
            try {

                QTL qtl = rec.qtl;

                if( qtl.getFlank1RgdId()!=null ) {
                    rec.flank1MarkerSymbol = dao.getSymbolForMarker(qtl.getFlank1RgdId());
                }
                if( qtl.getFlank2RgdId()!=null ) {
                    rec.flank2MarkerSymbol = dao.getSymbolForMarker(qtl.getFlank2RgdId());
                }
                if( qtl.getPeakRgdId()!=null ) {
                    rec.peakMarkerSymbol = dao.getSymbolForMarker(qtl.getPeakRgdId());
                }

                rec.terms = getOntologyAnnotations(qtl.getRgdId());
                rec.curatedRefs = dao.getCuratedRefs(qtl.getRgdId());
                rec.curatedPubmedIds = dao.getCuratedPubmedIds(qtl.getRgdId());
                rec.candidateGenes = getCandidateGenes(qtl.getRgdId());
                rec.relQtls = dao.getRelatedQtls(qtl.getKey());
                rec.notes = getNotes(qtl.getRgdId());
                rec.xdbIds = getXdbIds(qtl.getRgdId());

                // species specific data
                if( speciesType==SpeciesType.RAT ) {
                    rec.strains = getStrains(qtl.getRgdId());
                    rec.mapData1 = dao.getMapData(qtl.getRgdId(), 70); // Rnor_5.0
                    rec.mapData2 = dao.getMapData(qtl.getRgdId(), 60); // RGSC 3.4
                    rec.mapData3 = dao.getMapData(qtl.getRgdId(), 360);// Rnor_6.0
                    rec.mapData4 = dao.getMapData(qtl.getRgdId(), 372);// mRatBN7.2
                    rec.mapData5 = dao.getMapData(qtl.getRgdId(), 380);// GRCr8
                }
                else if( speciesType==SpeciesType.MOUSE ) {
                    rec.mapData1 = dao.getMapData(qtl.getRgdId(), 18);
                    rec.mapData2 = dao.getMapData(qtl.getRgdId(), 239);
                    rec.mapData3 = dao.getMapData(qtl.getRgdId(), 35);
                    rec.mapData4 = dao.getMapData(qtl.getRgdId(), 31); // 31: mouse cM map key
                }
                else if( speciesType==SpeciesType.HUMAN ) {
                    rec.mapData1 = dao.getMapData(qtl.getRgdId(), 17);
                    rec.mapData2 = dao.getMapData(qtl.getRgdId(), 13);
                    rec.mapData3 = dao.getMapData(qtl.getRgdId(), 38);
                }

                // cross compare notes of type "qtl_related_qtls"
                // against symbol of related qtls
                if( rec.notes[0]!=null ) {
                    // we have notes like that - split it into an array of qtl symbols
                    String[] symbolsFromNotes = rec.notes[0].replace(" ","").split("[,;:]");
                    // remove those symbols from notes that match with symbols of related qtls
                    StringBuilder buf = null;
                    for( String symbolFromNotes: symbolsFromNotes ) {
                        // is this symbol in related qtls?
                        if( !rec.relQtls.contains(symbolFromNotes) ) {
                            // build modified symbols-from-notes
                            if( buf==null )
                                buf = new StringBuilder(symbolFromNotes);
                            else
                                buf.append(';').append(symbolFromNotes);
                        }
                    }
                    String newNotes = buf==null ? "" : buf.toString();
                    if( newNotes.compareToIgnoreCase(rec.notes[0]) != 0 ) {
                        rec.notes[0] = newNotes;
                    }
                }

                String line = generateDataLine(rec, speciesType);
                lineMap.put(rec.qtl.getRgdId(), line);

            } catch( Exception e ) {
                throw new RuntimeException(e);
            }
        });

        // write data lines sorted by RGD ID
        writeDataLines(writer, lineMap);

        // close the output file
        writer.close();

        log.info("   "+ species.getQtlFileName()+",  elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis()));

        // copy the output file to the staging area
        FtpFileExtractsManager.qcFileContent(fileName, "qtls", speciesType);
    }

    String generateDataLine(QtlRecord rec, int speciesType) {
        QTL qtl = rec.qtl;
        StringBuilder buf = new StringBuilder();

        // 1. the RGD_ID of the QTL
        buf.append(qtl.getRgdId());
        buf.append('\t');

        // 2. species
        buf.append(SpeciesType.getCommonName(qtl.getSpeciesTypeKey()).toLowerCase());
        buf.append('\t');

        // 3. qtl symbol
        buf.append(checkNull(qtl.getSymbol()));
        buf.append('\t');

        // 4. qtl name
        buf.append(checkNull(qtl.getName()));
        buf.append('\t');

        // 5. the chromosome from the original paper
        buf.append(checkNull(qtl.getChromosome()));
        buf.append('\t');

        // 6. maximum LOD score if given in paper
        if( qtl.getLod()!=null )
            buf.append(qtl.getLod());
        buf.append('\t');

        // 7. p-value for QTL if given in paper
        if( qtl.getPValue()!=null )
            buf.append(qtl.getPValue());
        buf.append('\t');

        // 8. variance if given in paper
        if( qtl.getVariance()!=null )
            buf.append(qtl.getVariance());
        buf.append('\t');

        // marker symbols: markers could be MARKERS, GENES
        if( qtl.getFlank1RgdId()!=null ) {
            // 9. RGD_ID for flank marker 1, if in paper
            buf.append(qtl.getFlank1RgdId());
            buf.append('\t');

            // 10. symbol for flank marker 1, if in paper
            buf.append(checkNull(rec.flank1MarkerSymbol));
            buf.append('\t');
        }
        else // 9. 10.
            buf.append("\t\t");

        if( qtl.getFlank2RgdId()!=null ) {
            // 11. RGD_ID for flank marker 2, if in paper
            buf.append(qtl.getFlank2RgdId());
            buf.append('\t');

            // 12. symbol for flank marker 2, if in paper
            buf.append(checkNull(rec.flank2MarkerSymbol));
            buf.append('\t');
        }
        else // 11. 12.
            buf.append("\t\t");

        if( qtl.getPeakRgdId()!=null ) {
            // 13. RGD_ID for peak marker, if in paper
            buf.append(qtl.getPeakRgdId());
            buf.append('\t');

            // 14. symbol for peak marker, if in paper
            buf.append(checkNull(rec.peakMarkerSymbol));
            buf.append('\t');
        }
        else // 13. 14.
            buf.append("\t\t");

        // 15. trait name -- use VT term name if available, otherwise use note of type qtl_trait
        if( rec.terms[2]!=null ) {
            buf.append(checkNull(rec.terms[2]));
        } else {
            buf.append(checkNull(rec.notes[3]));
        }
        buf.append('\t');

        // 16. measurement type -- use CMO term name if available, otherwise use note of type qtl_subtrait
        if( rec.terms[3]!=null ) {
            buf.append(checkNull(rec.terms[3]));
        } else {
            buf.append(checkNull(rec.notes[4]));
        }
        buf.append('\t');

        // 17. unused
        buf.append('\t');

        // 18. phenotype ontology annotation
        buf.append(checkNull(rec.terms[0]));
        buf.append('\t');

        // 19. diseases ontology annotation
        buf.append(checkNull(rec.terms[1]));
        buf.append('\t');

        // 20. curated ref rgd ids: RGD_ID of paper(s) on QTL
        if( rec.curatedRefs!=null && rec.curatedRefs.length()> 0 ) {
            buf.append(checkNull(rec.curatedRefs.replace(',', ';')));
        }
        buf.append('\t');

        // 21. curated pumbed ids: PUBMED_ID of paper(s) on QTL
        if( rec.curatedPubmedIds!=null && rec.curatedPubmedIds.length()> 0 ) {
            buf.append(checkNull(rec.curatedPubmedIds.replace(',', ';')));
        }
        buf.append('\t');

        // 22. CANDIDATE_GENE_RGD_IDS - RGD_IDS genes mentioned by paper author
        buf.append(checkNull(rec.candidateGenes[0]));
        buf.append('\t');

        // 23. CANDIDATE_GENE_SYMBOLS
        buf.append(checkNull(rec.candidateGenes[1]));
        buf.append('\t');

        // 24. INHERITANCE_TYPE - dominant, recessive etc.
        buf.append(checkNull(qtl.getInheritanceType()));
        buf.append('\t');

        // 25. RELATED_QTLS	       symbols of related QTLS
        buf.append(checkNull(rec.relQtls));

        // warn about any associated notes of type "qtl_rel_qtls" -- this is obsolete
        if( rec.notes[0]!=null && rec.notes[0].length()>0  )
            log.info("NOTE: qtl "+qtl.getSymbol()+" with qtl_key="+qtl.getKey()+" has unmatching public qtl_rel_qtls notes:"+rec.notes[0]);

        buf.append('\t');

        // 26. (UNUSED) for rat
        // 26. OMIM_ID for human, if available
        // 26. MGI_ID for mouse, if available
        buf.append(checkNull(rec.xdbIds));
        buf.append('\t');

        // 27-30 for rat, 5.0 assembly position (see explanation above)
        // 27-30 for human, 37 assembly position
        // 27-30 for mouse, 37 assembly position
        writeMapData(buf, rec.mapData1);

        // 31-34 for rat, 3.4 assembly position
        // 31-34 for human, 36 assembly position
        // 31-34 for mouse, GRCm39 assembly position
        writeMapData(buf, rec.mapData2);

        String[] strains = rec.strains;
        if( speciesType==SpeciesType.RAT ) {
            // 35. CROSS_TYPE self explanatory
            buf.append(checkNull(rec.notes[2]));
            buf.append('\t');
            // 36. CROSS_PAIR pairing of strains for cross
            buf.append(checkNull(rec.notes[1]));
            buf.append('\t');

            // 37. STRAIN_RGD_ID1 RGD_ID of first strain crossed
            buf.append(checkNull(strains[0]));
            buf.append('\t');
            // 38. STRAIN_RGD_ID1 RGD_ID of second strain crossed
            buf.append(checkNull(strains[1]));
            buf.append('\t');
            // 39. STRAIN_RGD_SYMBOL1 symbol of first strain crossed
            buf.append(checkNull(strains[3]));
            buf.append('\t');
            // 40. STRAIN_RGD_SYMBOL2 symbol of second strain crossed
            buf.append(checkNull(strains[4]));
            buf.append('\t');
        }
        else if( speciesType==SpeciesType.MOUSE ) {
            if( rec.mapData4.isEmpty() )
                buf.append("\t\t");
            else {
                MapData md = rec.mapData4.get(0);
                // 35. cM map chromosome, for mouse
                buf.append(checkNull(md.getChromosome()));
                buf.append('\t');
                // 36. cM map absolute position, for mouse
                if( md.getAbsPosition()!=null )
                    buf.append(md.getAbsPosition());
                buf.append('\t');
            }
        }

        // rat, Rnor_6.0 assembly position
        // mouse, 38 assembly position
        // human, 38 assembly position
        if( rec.mapData3!=null )
            writeMapData(buf, rec.mapData3);

        // rat: RGD_ID and symbol of 3rd strain crossed, rn7 and rn8 positions
        if( speciesType==SpeciesType.RAT ) {
            // 45. STRAIN_RGD_ID3 RGD_ID of third strain crossed
            buf.append(checkNull(strains[2]));
            buf.append('\t');
            // 46. STRAIN_RGD_SYMBOL3 symbol of third strain crossed
            buf.append(checkNull(strains[5]));
            buf.append('\t');

            writeMapData(buf, rec.mapData4);
            writeMapData(buf, rec.mapData5);
        }

        // terminate the line
        buf.append("\n");

        return buf.toString();
    }

    void writeMapData(StringBuilder buf, List<MapData> mds) {
        if( mds==null || mds.isEmpty() )
            buf.append("\t\t\t\t");
        else {
            MapData md = mds.get(0);
            if( mds.size()>1 ) {
                System.out.println("***MULTIS for RGDID:"+md.getRgdId()+", map_key:"+md.getMapKey());
            }
            // chromosome
            buf.append(checkNull(md.getChromosome()));
            buf.append('\t');
            // start pos
            if( md.getStartPos()!=null )
                buf.append(md.getStartPos());
            buf.append('\t');
            // stop pos
            if( md.getStopPos()!=null )
                buf.append(md.getStopPos());
            buf.append('\t');
            // method
            buf.append(checkNull(md.getMapPositionMethod()));
            buf.append('\t');
        }
    }

    String[] getOntologyAnnotations(int qtlRgdId) throws Exception {
        String[] terms = new String[4];
        for( Annotation annot: dao.getAnnotations(qtlRgdId) ) {
            int termArrayIndex = -1;
            boolean appendAccId = false;
            switch(annot.getAspect()) {
                case "N": // phenotype MP
                    termArrayIndex = 0;
                    break;
                case "D": // disease RDO
                    termArrayIndex = 1;
                    break;
                case "V": // vertebrate trait VT
                    termArrayIndex = 2;
                    appendAccId = true;
                    break;
                case "L": // measurement type CMO
                    termArrayIndex = 3;
                    appendAccId = true;
                    break;
            }
            if( termArrayIndex>=0 ) {
                String term = annot.getTerm();
                if( appendAccId ) {
                    term += " ("+annot.getTermAcc()+")";
                }
                if( terms[termArrayIndex]==null )
                    terms[termArrayIndex] = term;
                else
                    terms[termArrayIndex] += ";"+term;
            }
        }
        return terms;
    }

    String[] getCandidateGenes(int qtlRgdId) throws Exception {
        String[] out = new String[2];
        for( Gene gene: dao.getGeneAssociationsByQtl(qtlRgdId) ) {
            if( out[0]==null )
                out[0] = Integer.toString(gene.getRgdId());
            else
                out[0] += ";"+Integer.toString(gene.getRgdId());
            if( out[1]==null )
                out[1] = gene.getSymbol();
            else
                out[1] += ";"+gene.getSymbol();
        }
        return out;
    }

    /**
     * return notes for related qtls, cross_pair, cross_type qtls, qtl_trait and qtl_subtrait
     * @param qtlRgdId qtl rgd id
     * @return array with combined notes for related, cross pair and cross type qtls
     * @throws Exception
     */
    String[] getNotes(int qtlRgdId) throws Exception {
        String[] out = new String[5];
        for( Note note: dao.getNotes(qtlRgdId) ) {
            if( note.getNotes()==null || note.getNotes().trim().length()==0 || note.getNotesTypeName()==null )
                continue;
            int outArrayIndex = -1;
            switch(note.getNotesTypeName()) {
                case "qtl_related_qtls":
                    if( note.getPublicYN().equals("Y") ) {
                        outArrayIndex = 0;
                    }
                    break;
                case "qtl_cross_pair":
                    outArrayIndex = 1;
                    break;
                case "qtl_cross_type":
                    outArrayIndex = 2;
                    break;
                case "qtl_trait":
                    outArrayIndex = 3;
                    break;
                case "qtl_subtrait":
                    outArrayIndex = 4;
                    break;
            }
            if( outArrayIndex>=0 ) {
                if( out[outArrayIndex]==null )
                    out[outArrayIndex] = note.getNotes();
                else
                    out[outArrayIndex] += ";"+note.getNotes();
            }
        }
        return out;
    }

    /**
     * return combined list of xdb ids; specific for species
     * @param qtlRgdId qtl rgd id
     * @return comma separated string of ids: (UNUSED) for rat, OMIM_IDs for human, MGI_IDs for mouse
     * @throws Exception
     */
    String getXdbIds(int qtlRgdId) throws Exception {

        int xdbKey = species.getSpeciesType()==SpeciesType.HUMAN ? XdbId.XDB_KEY_OMIM
                : species.getSpeciesType()==SpeciesType.MOUSE ? XdbId.XDB_KEY_MGD
                : 0;
        String xdbIds = null;
        if( xdbKey>0 )
            for( XdbId xdbId: dao.getXdbIds(qtlRgdId, xdbKey)) {
                if( xdbIds==null )
                    xdbIds = xdbId.getAccId();
                else
                    xdbIds += ";" + xdbId.getAccId();
            }
        return xdbIds;
    }

    // crossed strains
    String[] getStrains(int qtlRgdId) throws Exception {
        String[] out = new String[6];
        for( Strain strain: dao.getStrainsAssociatedWithQtl(qtlRgdId) ) {
            if( out[0]==null )
                out[0] = Integer.toString(strain.getRgdId());
            else
            if( out[1]==null )
                out[1] = Integer.toString(strain.getRgdId());
            else
            if( out[2]==null )
                out[2] = Integer.toString(strain.getRgdId());
            else {
                System.out.println("problems with crossed strain rgd_id export for qtlRgdId="+qtlRgdId);
            }

            if( out[3]==null )
                out[3] = strain.getSymbol();
            else
            if( out[4]==null )
                out[4] = strain.getSymbol();
            else
            if( out[5]==null )
                out[5] = strain.getSymbol();
            else {
                System.out.println("problems with crossed strain symbol export for qtlRgdId="+qtlRgdId);
            }
        }
        return out;

    }

    private String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

    public FtpFileExtractsDAO getDao() {
        return dao;
    }

    public void setDao(FtpFileExtractsDAO dao) {
        this.dao = dao;
    }

    class QtlRecord {

        public QTL qtl;
        public String flank1MarkerSymbol;
        public String flank2MarkerSymbol;
        public String peakMarkerSymbol;
        public String[] terms; // ontology annotations
        public String curatedRefs;
        public String curatedPubmedIds;
        public String[] candidateGenes;
        public String relQtls;
        public String[] notes;
        public String xdbIds;
        public List<MapData> mapData1;
        public List<MapData> mapData2;
        public List<MapData> mapData3;
        public List<MapData> mapData4;
        public List<MapData> mapData5;
        public String[] strains;
    }
}
