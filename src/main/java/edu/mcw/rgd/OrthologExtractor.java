package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.Map;

/**
 * @author mtutaj
 * @since Nov 11, 2010
 */
public class OrthologExtractor extends BaseExtractor {

    private String fileName;
    private String fileNameRatMine;

    final String HEADER =
        "# RGD-PIPELINE: ftp-file-extracts\n"+
        "# MODULE: orthologs-version-2000-01-13\n"+
        "# GENERATED-ON: #DATE#\n"+
        "# RGD Ortholog FTP file\n" +
        "# From: RGD\n" +
        "# URL: http://rgd.mcw.edu\n" +
        "# Contact: RGD.Data@mcw.edu\n" +
        "#\n" +
        "### As of Dec 19 2011 v. 2.0.1: no data changes; improved internal QC.\n" +
        "### As of Feb 11 2013 v. 2.1.0: added column HUMAN_ORTHOLOG_HGNC_ID as the last column in the file\n" +
        "### As of Feb 4 2015 v. 2.1.1: updated description in header\n" +
        "#\n" +
        "# Format:\n" +
        "# Comment lines begin with '#'\n" +
        "# Tab delimited fields, one line per rat gene.\n" +
        "# Fields in order:\n" +
        "# 1. RAT_GENE_SYMBOL\n" +
        "# 2. RAT_GENE_RGD_ID\n" +
        "# 3. RAT_GENE_NCBI_GENE_ID\n" +
        "# 4. HUMAN_ORTHOLOG_SYMBOL - human ortholog(s) to rat gene\n" +
        "# 5. HUMAN_ORTHOLOG_RGD_ID\n" +
        "# 6. HUMAN_ORTHOLOG_NCBI_GENE_ID\n" +
        "# 7. HUMAN_ORTHOLOG_SOURCE - RGD or HGNC\n" +
        "# 8. MOUSE_ORTHOLOG_SYMBOL - mouse ortholog(s) to rat gene\n" +
        "# 9. MOUSE_ORTHOLOG_RGD_ID\n" +
        "#10. MOUSE_ORTHOLOG_NCBI_GENE_ID\n" +
        "#11. MOUSE_ORTHOLOG_MGI_ID\n" +
        "#12. MOUSE_ORTHOLOG_SOURCE - RGD or MGI\n" +
        "#13. HUMAN_ORTHOLOG_HGNC_ID\n" +
        "#\n" +
        "# Most orthologs listed will be one-to-one-to-one across all three species.\n" +
        "# Where multiple human or mouse homologs are present in RGD the human and mouse\n" +
        "# fields will contain multiple entries separated by '|'.\n" +
        "#\n" +
        "# There may not be a curated ortholog for human or mouse in which case the fields\n" +
        "# for the respective species will be blank\n" +
        "#\n" +
        "# Multiple orthologs may be redundant when assigned by both RGD and MGI. These will\n" +
        "# be resolved over time.\n" +
        "#\n" +
        "# SOURCE fields will be either 'RGD', for orthologs manually created by RGD curators,\n" +
        "# or 'HGNC' for ortholog data derived from the HGNC Comparison of Orthology Predictions (HCOP) data compiled by:\n" +
        "# HUGO Gene Nomenclature Committee, European Bioinformatics Institute (EMBL-EBI)\n" +
        "# Wellcome Trust Genome Campus, Hinxton, Cambridge, UK\n" +
        "# URL:  http://www.genenames.org/\n" +
        "#\n" +
        "# Reference for HGNC and HCOP:\n" +
        "# Gray KA, Yates B, Seal RL, Wright MW, Bruford EA. \n" +
        "# Genenames.org: the HGNC resources in 2015.\n" +
        "# Nucleic Acids Res. 2015 Jan 28;43(Database issue):D1079-85. \n" +
        "# PMID: 25361968,  doi: 10.1093/nar/gku1071.\n" +
        "#\n"+
        "RAT_GENE_SYMBOL\tRAT_GENE_RGD_ID\tRAT_GENE_NCBI_GENE_ID\t"+
        "HUMAN_ORTHOLOG_SYMBOL\tHUMAN_ORTHOLOG_RGD\tHUMAN_ORTHOLOG_NCBI_GENE_ID\tHUMAN_ORTHOLOG_SOURCE\t"+
        "MOUSE_ORTHOLOG_SYMBOL\tMOUSE_ORTHOLOG_RGD\tMOUSE_ORTHOLOG_NCBI_GENE_ID\tMOUSE_ORTHOLOG_MGI\tMOUSE_ORTHOLOG_SOURCE\t"+
        "HUMAN_ORTHOLOG_HGNC_ID\n";

    Logger log = Logger.getLogger(getClass());

    public void run(SpeciesRecord speciesRec) throws Exception {

        // we extract only rat orthologs; ignore species other than rat
        if( speciesRec.getSpeciesType()!=SpeciesType.RAT )
            return;
        System.out.println(getVersion());

        // initialize static members
        GeneInfo.dao = getDao();
        final Map<Integer,HomologyRecord> map = new HashMap<>();

        // build map of all orthologs -- all entries of any SRC_RGD_ID
        for( Ortholog ortholog: getDao().getOrthologs(SpeciesType.RAT) ) {
            int srcRgdId = ortholog.getSrcRgdId();
            HomologyRecord rec = map.get(srcRgdId);
            if( rec==null ) {
                rec = new HomologyRecord(srcRgdId);
                map.put(srcRgdId, rec);
            }
            rec.orthologs.add(ortholog);
        }

        // put all homology record to the pipeline system
        map.values().parallelStream().forEach( rec -> {

            try {
                // get rat information from database
                rec.loadRatInfo();

                // load supplementary homolog info from database
                rec.loadHomologInfo(false);

                // write out all the parameters to the file
                rec.overviewLine = rec.printRecord(false);

                // load supplementary homolog info from database
                rec.loadHomologInfo(true);

                // write out all the parameters to the file
                rec.detailedLines = rec.printRecord(true);
            } catch( Exception e ) {
                throw new RuntimeException(e);
            }
        });


        // sort homology records by symbol
        Object[] records = map.values().toArray();
        Arrays.sort(records);

        // print standard report file and copy it to staging area
        printReport(getFileName(), records, false);
        // print detailed report file and copy it to staging area
        printReport(getFileNameRatMine(), records, true);

        splitOrthologFilesForRatmine();
    }

    void printReport(String fileName, Object[] records, boolean detailMode) throws Exception {
        File dir = new File(getExtractDir());
        if( !dir.exists() ) {
            dir.mkdirs();
        }

        String outputFileName = getExtractDir()+'/'+fileName;
        log.info("started extraction to "+outputFileName);
        final PrintWriter writer = new PrintWriter(outputFileName);

        // prepare header common lines
        String commonLines = HEADER
                .replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.print(commonLines);

        for( Object record: records ) {
            HomologyRecord rec = (HomologyRecord) record;
            if( detailMode ) {
                writer.print(rec.detailedLines);
            } else {
                writer.print(rec.overviewLine);
            }
        }
        writer.close();

        // copy the output file to the staging area
        FtpFileExtractsManager.qcFileContent(outputFileName, "ortholog", SpeciesType.RAT);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileNameRatMine(String fileNameRatMine) {
        this.fileNameRatMine = fileNameRatMine;
    }

    public String getFileNameRatMine() {
        return fileNameRatMine;
    }

    public void splitOrthologFilesForRatmine() throws Exception {

        String fname = "data/RGD_ORTHOLOGS_RATMINE.txt";
        String line;
        Map<String, BufferedWriter> writers = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(fname));
        String header = "";
        while( (line=reader.readLine())!=null ) {
            header += line + '\n';
            if( !line.startsWith("#") ) {
                break;
            }
        }

        while( (line=reader.readLine())!=null ) {
            String[] cols = line.split("[\\t]");
            String src = cols[6];
            BufferedWriter out = writers.get(src);
            if( out==null ) {
                out = new BufferedWriter(new FileWriter("data/RGD_ORTHOLOGS_"+src+".txt"));
                out.write(header);
                writers.put(src, out);
            }

            out.write(line+'\n');
        }

        reader.close();

        for( BufferedWriter out: writers.values() ) {
            out.close();
        }
    }
}

class HomologyRecord implements Comparable<HomologyRecord> {
    public int ratRgdId;
    public List<Ortholog> orthologs = new ArrayList<>();

    public GeneInfo ratInfo;
    public List<HumanHomolog> humanHomologs = new ArrayList<>();
    public List<MouseHomolog> mouseHomologs = new ArrayList<>();

    // line with all data columns as to be written to output file
    public String overviewLine;
    public String detailedLines;

    public HomologyRecord(int ratRgdId) {
        this.ratRgdId = ratRgdId;
    }

    public void loadRatInfo() throws Exception {
        this.ratInfo = new GeneInfo(ratRgdId);
    }

    void addHomolog(int homolRgdID, String src, int species_type_key) throws Exception {
        switch (species_type_key) {
        case SpeciesType.HUMAN:
            HumanHomolog hh = new HumanHomolog(homolRgdID, src);
            appendHumanHomolog(hh);
            break;

        case SpeciesType.MOUSE:
            MouseHomolog mh = new MouseHomolog(homolRgdID, src);
            if( mh.mgiID != null )
                appendMouseHomolog(mh);
            break;

        default:
            // ignore other species types
            //throw new Exception("unexpected species type");
        }
    }

    /**
     * populate humanHomologs and mouseHomologs
     * @throws Exception
     */
    public void loadHomologInfo(boolean detailedMode) throws Exception {
        // clear existing homologs
        this.humanHomologs.clear();
        this.mouseHomologs.clear();

        if(!detailedMode) {
            //(row.other_rgd_id, row.src, row.species_type_key)
            for (Ortholog o : orthologs) {
                addHomolog(o.getDestRgdId(), o.getXrefDataSrc(), o.getDestSpeciesTypeKey());
            }
        } else {
            for (Ortholog o : orthologs) {
                if( Utils.isStringEmpty(o.getXrefDataSet()) ) {
                    addHomolog(o.getDestRgdId(), o.getXrefDataSrc(), o.getDestSpeciesTypeKey());
                } else {
                    String[] detailedSources = o.getXrefDataSet().split(", ");
                    for (String source : detailedSources) {
                        addHomolog(o.getDestRgdId(), source, o.getDestSpeciesTypeKey());
                    }
                }
            }
        }
    }

    void appendHumanHomolog(HumanHomolog hh) {
        if (hh.symbol.equalsIgnoreCase(ratInfo.symbol)) {
            humanHomologs.add(0, hh);
        } else {
            humanHomologs.add(hh);
        }
    }

    void appendMouseHomolog(MouseHomolog mh) {
        if (mh.symbol.equalsIgnoreCase(ratInfo.symbol)) {
            mouseHomologs.add(0, mh);
        } else {
            mouseHomologs.add(mh);
        }
    }

    String printRecord(boolean detailMode) {

        if(!detailMode) {
            return printRecord();
        }

        StringBuilder buf = new StringBuilder(120);

        for( HumanHomolog hh: humanHomologs ) {
            for (MouseHomolog mh : mouseHomologs) {
                // output rat fields
                buf.append(checkNull(ratInfo.symbol))
                        .append('\t')
                        .append(ratInfo.rgdID)
                        .append('\t')
                        .append(checkNull(ratInfo.egID))
                        .append('\t');

                // output human fields
                buf.append(checkNull(hh.symbol))
                        .append('\t')
                        .append(checkNull(Integer.toString(hh.rgdID)))
                        .append('\t')
                        .append(checkNull(hh.egID))
                        .append('\t')
                        .append(checkNull(hh.src))
                        .append('\t');

                // output mouse fields
                buf.append(checkNull(mh.symbol))
                        .append('\t')
                        .append(checkNull(Integer.toString(mh.rgdID)))
                        .append('\t')
                        .append(checkNull(mh.egID))
                        .append('\t')
                        .append(checkNull(mh.mgiID))
                        .append('\t')
                        .append(checkNull(mh.src))
                        .append('\t');

                // extra fields
                buf.append(checkNull(hh.hgncID))
                        .append('\n');

            }
        }
        return buf.toString();
    }

    String printRecord() {

        StringBuilder writer = new StringBuilder(120);
        // output rat fields
        writer.append(checkNull(ratInfo.symbol))
              .append('\t')
              .append(ratInfo.rgdID)
              .append('\t')
              .append(checkNull(ratInfo.egID))
              .append('\t');

        // build human fields
        String symbols, rgdIds, egIds, sources, hgncIds;
        symbols = rgdIds = egIds = sources = hgncIds = null;
        for( HumanHomolog hh: humanHomologs ) {
            symbols = append(symbols, hh.symbol);
            rgdIds = append(rgdIds, Integer.toString(hh.rgdID));
            egIds = append(egIds, hh.egID);
            sources = append(sources, hh.src);
            hgncIds = append(hgncIds, hh.hgncID);
        }

        // output human fields
        writer.append(checkNull(symbols))
              .append('\t')
              .append(checkNull(rgdIds))
              .append('\t')
              .append(checkNull(egIds))
              .append('\t')
              .append(checkNull(sources))
              .append('\t');

        // build mouse fields
        symbols = rgdIds = egIds = sources = null;
        String mgiIds = null;
        for( MouseHomolog mh: mouseHomologs ) {
            symbols = append(symbols, mh.symbol);
            rgdIds = append(rgdIds, Integer.toString(mh.rgdID));
            egIds = append(egIds, mh.egID);
            mgiIds = append(mgiIds, mh.mgiID);
            sources = append(sources, mh.src);
        }

        // output mouse fields
        writer.append(checkNull(symbols))
              .append('\t')
              .append(checkNull(rgdIds))
              .append('\t')
              .append(checkNull(egIds))
              .append('\t')
              .append(checkNull(mgiIds))
              .append('\t')
              .append(checkNull(sources))
              .append('\t');

        // extra fields
        writer.append(checkNull(hgncIds))
            .append('\n');

        return writer.toString();
    }

    private String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

    String append(String oldVal, String val) {
        if( oldVal==null )
            return val;
        return oldVal+"|"+val;
    }

    public int compareTo(HomologyRecord o) {
        // we sort homology records by rgd gene symbol
        return ratInfo.symbol.compareToIgnoreCase(o.ratInfo.symbol);
    }
}

class GeneInfo {
    int rgdID;
    String symbol;
    String egID;

    public static FtpFileExtractsDAO dao;

    public GeneInfo(int rgdID) throws Exception {
        this.rgdID = rgdID;
        this.symbol = dao.getSymbolForMarker(rgdID);
        initEgID();
    }

    void initEgID() throws Exception {
        List<XdbId> xdbIds = dao.getXdbIds(rgdID, XdbId.XDB_KEY_NCBI_GENE);
        if( xdbIds.size() > 1 )
		    System.out.println("multiple NCBI Gene references ("+xdbIds.size()+") found for RGD:"+rgdID+", symbol="+symbol);
	    else if( xdbIds.isEmpty() )
		    System.out.println("missing NCBI Gene reference for RGD:"+rgdID+", symbol="+symbol);

    	if( !xdbIds.isEmpty() )
		    egID = xdbIds.get(0).getAccId();
    }
}

class HomologInfo extends GeneInfo {
    String src;
    public HomologInfo(int rgdID, String src) throws Exception {
        super(rgdID);
        this.src = src;
    }

    Collection<String> getXdbIds(int xdbKey) throws Exception {
        Set<String> accIds = new HashSet<>();
        for( XdbId xdbId: dao.getXdbIds(rgdID, xdbKey) ) {
            accIds.add(xdbId.getAccId());
        }
        return accIds;
    }
}

class HumanHomolog extends HomologInfo {
    String hgncID;
    public HumanHomolog(int rgdID, String src) throws Exception {
        super(rgdID, src);
        initHgncID();
    }

    void initHgncID() throws Exception {
        Collection<String> xdbIds = getXdbIds(XdbId.XDB_KEY_HGNC);
        if( xdbIds.size() > 1 ) {
            System.out.println("multiple HGNC IDs (" + xdbIds.size() + ") found for RGD:" + rgdID + ", symbol=" + symbol);
        }

    	if( !xdbIds.isEmpty() ) {
            hgncID = xdbIds.iterator().next();
        }
    }
}

class MouseHomolog extends HomologInfo {
    String mgiID;
    public MouseHomolog(int rgdID, String src) throws Exception {
        super(rgdID, src);
        initMgiID();
    }

    void initMgiID() throws Exception {
        Collection<String> xdbIds = getXdbIds(XdbId.XDB_KEY_MGD);
        if( xdbIds.size() > 1 ) {
            System.out.println("multiple MGI IDs (" + xdbIds.size() + ") found for RGD:" + rgdID + ", symbol=" + symbol);
        } else if( xdbIds.isEmpty() ) {
            System.out.println("missing MGI ID for RGD:" + rgdID + ", symbol=" + symbol);
        }

    	if( !xdbIds.isEmpty() ) {
            mgiID = xdbIds.iterator().next();
        }
    }
}
