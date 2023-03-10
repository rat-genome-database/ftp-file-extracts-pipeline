package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mtutaj
 * @since 7/14/14
 * extract chinchilla data from RGD database into tab-separated files to be put on RGD FTP site
 */
public class ChinchillaExtractor extends BaseExtractor {

    final int speciesType = SpeciesType.CHINCHILLA;

    final String GENES_HEADER =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: chinchilla-genes-version-2023-03-10\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active chinchilla genes extracted from RGD database\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"#1   GENE_RGD_ID	      RGD_ID of the gene\n"
    +"#2   GENE_TYPE          gene type\n"
    +"#3   SYMBOL             official gene symbol\n"
    +"#4   NAME    	          gene name\n"
    +"#5   GENE_DESC          gene description (if available)\n"
    +"#6   CONTIG             contig acc id\n"
    +"#7   START_POS          start position within contig\n"
    +"#8   STOP_POS           stop position within contig\n"
    +"#9   STRAND             strand information\n"
    +"#10  NCBI_GENE_ID       NCBI Gene ID\n"
    +"#11  GENBANK_NUCLEOTIDE GenBank Nucleotide ID(s)\n"
    +"#12  GENBANK_PROTEIN    GenBank Protein ID(s)\n"
    +"#13  GENE_REFSEQ_STATUS gene RefSeq Status (from NCBI)\n";

    Logger log = LogManager.getLogger(getClass());
    private String orthologsFile;
    private String bedFile;
    private String genesFile;
    private int mapKey;

    public void run(SpeciesRecord species) throws Exception {

        extractGenes(getGenesFile(), getMapKey());
        extractOrthologs();
    }

    public void extractGenes(String fileName, int mapKey) throws Exception {

        // prepare header common lines
        String headerLines = GENES_HEADER.replace("#DATE#", SpeciesRecord.getTodayDate());

        // generate regular file
        runGenes(headerLines, getExtractDir()+"/"+fileName, getExtractDir()+"/"+getBedFile(), mapKey);
    }

    String runGenes(String headerLines, String outputFileName, String bedFileName, final int mapKey) throws Exception {

        log.info("### started extraction to "+outputFileName);
        long time0 = System.currentTimeMillis();

        final FtpFileExtractsDAO dao = getDao();
        final PrintWriter writer = new PrintWriter(outputFileName);
        writer.println(headerLines);

        final PrintWriter bedWriter = new PrintWriter(bedFileName);
        bedWriter.println("track name=\"chinchillaRefSeqData\" description=\"format: GeneSymbol|RgdId|GeneId|NucleotideAccIds|ProteinAccIds\" useScore=0");

        List<GeneExtractRecord> genesToProcess = loadGeneRecords(speciesType);
        AtomicInteger recCount = new AtomicInteger();

        genesToProcess.parallelStream().forEach( rec -> {

            try {
                for (MapData md : dao.getMapData(rec.getRgdId())) {
                    // skip maps with empty chromosomes
                    if (md.getChromosome() == null || md.getChromosome().trim().length() == 0)
                        continue;

                    if (md.getMapKey() == mapKey) {
                        rec.assembly1Map.add(md);
                    }
                }

                for (XdbId xdbId : getXdbIdList(rec.getRgdId())) {
                    switch (xdbId.getXdbKey()) {
                        case XdbId.XDB_KEY_NCBI_GENE:
                            rec.addNcbiGeneIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_GENEBANKNU:
                            rec.addGeneBankNucleoIds(xdbId.getAccId());
                            break;
                        case XdbId.XDB_KEY_GENEBANKPROT:
                            rec.addGeneBankProteinIds(xdbId.getAccId());
                            break;
                    }
                }

                // get aliases
                for (Alias alias : dao.getAliases(rec.getRgdId())) {
                    if (alias.getTypeName() == null)
                        continue;
                    if (alias.getTypeName().equals("old_gene_name"))
                        rec.addOldGeneNames(alias.getValue());
                    if (alias.getTypeName().equals("old_gene_symbol"))
                        rec.addOldGeneSymbols(alias.getValue());
                }

                // write out all the parameters to the file
                String line = generateLine(rec);
                writeLine(line, writer);
                recCount.incrementAndGet();

                String bedLine = generateBedLine(rec);
                writeBedLine(bedLine, bedWriter);
            }catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

        // close the output files
        writer.close();
        bedWriter.close();

        log.info("### extracted "+recCount.intValue()+"  rows;  elapsed "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));

        return outputFileName;
    }

    String generateLine(GeneExtractRecord rec) throws Exception {

        return String.valueOf(rec.getRgdId()) +
            '\t' +
            checkNull(rec.getGeneType()) +
            '\t' +
            checkNull(rec.getGeneSymbol()) +
            '\t' +
            checkNull(rec.getGeneFullName()) +
            '\t' +
            checkNull(rec.getGeneDesc()) +
            '\t' +
            getString(rec.assembly1Map, "getChromosome") +
            '\t' +
            getString(rec.assembly1Map, "getStartPos") +
            '\t' +
            getString(rec.assembly1Map, "getStopPos") +
            '\t' +
            getString(rec.assembly1Map, "getStrand") +
            '\t' +
            checkNull(rec.getNcbiGeneIds()) +
            '\t' +
            checkNull(rec.getGeneBankNucleoIds()) +
            '\t' +
            checkNull(rec.getGeneBankProteinIds()) +
            '\t' +
            checkNull(rec.getRefSeqStatus()) +
            '\n';
    }

    synchronized void writeLine(String line, PrintWriter writer) throws Exception {
        writer.append(line);
    }

    String getString(List<MapData> mds, String method) throws Exception {
        if( mds==null || mds.isEmpty() )
            return "";
        return Utils.concatenate(";", mds, method);
    }

    String generateBedLine(GeneExtractRecord rec) throws Exception {

        // according to BED spec, the first 3 columns {CHR, START_POS, END_POS} are required;
        //  if position is not available, we do not emit a row
        if( rec.assembly1Map==null || rec.assembly1Map.isEmpty() ) {
            return "";
        }

        return getString(rec.assembly1Map, "getChromosome") +
        '\t' +
        checkNull(rec.assembly1Map.get(0).getStartPos() - 1) +
        '\t' +
        checkNull(rec.assembly1Map.get(0).getStopPos() - 1) +
        '\t' +
        rec.getGeneSymbol() +
        "|" + String.valueOf(rec.getRgdId()) +
        "|" + rec.getNcbiGeneIds() +
        "|" + rec.getRefSeqNucleoIds() +
        "|" + rec.getRefSeqProteinIds() +
        '\t' +
        '0' +
        '\t' +
        getString(rec.assembly1Map, "getStrand") +
        '\n';
    }

    synchronized void writeBedLine(String line, PrintWriter writer) throws Exception {
        writer.append(line);
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


    final String ORTHOLOGS_HEADER =
        "# RGD-PIPELINE: ftp-file-extracts\n"+
        "# MODULE: chinchilla-orthologs-version-2019-12-02\n"+
        "# GENERATED-ON: #DATE#\n"+
        "# RGD Ortholog FTP file\n" +
        "# From: RGD\n" +
        "# URL: https://rgd.mcw.edu\n" +
        "# Contact: RGD.Data@mcw.edu\n" +
        "#\n" +
        "# Format:\n" +
        "# Comment lines begin with '#'\n" +
        "# Tab delimited fields, one line per rat gene.\n" +
        "# Fields in order:\n" +
        "# 1. CHINCHILLA_GENE_SYMBOL\n" +
        "# 2. CHINCHILLA_GENE_RGD_ID\n" +
        "# 3. CHINCHILLA_GENE_NCBI_GENE_ID\n" +
        "# 4. HUMAN_ORTHOLOG_SYMBOL - human ortholog(s) to chinchilla gene\n" +
        "# 5. HUMAN_ORTHOLOG_RGD_ID\n" +
        "# 6. HUMAN_ORTHOLOG_NCBI_GENE_ID\n" +
        "#\n" +
        "# Where multiple human homologs are present in RGD, the human\n" +
        "# fields will contain multiple entries separated by '|'.\n" +
        "#\n"+
        "CHINCHILLA_GENE_SYMBOL\tCHINCHILLA_GENE_RGD_ID\tCHINCHILLA_GENE_NCBI_GENE_ID\t"+
        "HUMAN_ORTHOLOG_SYMBOL\tHUMAN_ORTHOLOG_RGD_ID\tHUMAN_ORTHOLOG_NCBI_GENE_ID\n";

    public void extractOrthologs() throws Exception {

        long time0 = System.currentTimeMillis();
        String outputFileName = getExtractDir()+"/"+getOrthologsFile();
        log.info("### started extraction to "+outputFileName);
        final PrintWriter writer = new PrintWriter(outputFileName);

        // prepare header common lines
        String commonLines = ORTHOLOGS_HEADER
                .replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.print(commonLines);


        // extract chinchilla-human orthologs
        final java.util.Map<Integer, OrthologRecord> map = new HashMap<Integer, OrthologRecord>();

        List<OrthologRecord> records = new ArrayList<>();
        // build map of chinchilla-human orthologs -- all entries of any SRC_RGD_ID
        int recNo;
        for( Ortholog ortholog: getDao().getOrthologs(SpeciesType.CHINCHILLA) ) {
            if( ortholog.getDestSpeciesTypeKey()!=SpeciesType.HUMAN )
                continue;
            recNo = ortholog.getSrcRgdId();
            OrthologRecord rec = map.get(recNo);
            if( rec==null ) {
                rec = new OrthologRecord(recNo);
                map.put(recNo, rec);
            }
            rec.orthologs.add(ortholog);
            records.add(rec);
        }

        records.parallelStream().forEach( rec -> {

            try {
                // get rat information from database
                rec.loadChinchillaInfo(getDao());

                // load supplementary homolog info from database
                rec.loadHomologInfo();

            } catch( Exception e) {
                throw new RuntimeException(e);
            }

            // write out all the parameters to the file
            rec.printRecord();
        });

        // sort homology records by symbol
        Object[] recordsSorted = map.values().toArray();
        Arrays.sort(recordsSorted);
        for( Object record: recordsSorted ) {
            writer.print(record.toString());
        }
        writer.close();

        log.info("### extracted "+recordsSorted.length+"  rows;  elapsed "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    public void setOrthologsFile(String orthologsFile) {
        this.orthologsFile = orthologsFile;
    }

    public String getOrthologsFile() {
        return orthologsFile;
    }

    public void setBedFile(String bedFile) {
        this.bedFile = bedFile;
    }

    public String getBedFile() {
        return bedFile;
    }

    public void setGenesFile(String genesFile) {
        this.genesFile = genesFile;
    }

    public String getGenesFile() {
        return genesFile;
    }

    public int getMapKey() {
        return mapKey;
    }

    public void setMapKey(int mapKey) {
        this.mapKey = mapKey;
    }

    class OrthologRecord implements Comparable<OrthologRecord> {
        public int chinchillaRgdId;
        public List<Ortholog> orthologs = new ArrayList<Ortholog>();

        public GeneInfo2 ratInfo;
        public List<GeneInfo2> humanHomologs = new ArrayList<GeneInfo2>();

        public String line; // line with all data columns as to be written to output file

        public OrthologRecord(int chinchillaRgdId) {
            this.chinchillaRgdId = chinchillaRgdId;
        }

        public void loadChinchillaInfo(FtpFileExtractsDAO dao) throws Exception {
            this.ratInfo = new GeneInfo2(chinchillaRgdId, dao);
        }

        public void loadHomologInfo() throws Exception {
            //(row.other_rgd_id, row.src, row.species_type_key)
            for( Ortholog o: orthologs ) {
                GeneInfo2 hh = new GeneInfo2(o.getDestRgdId(), getDao());
                if (hh.symbol.equalsIgnoreCase(ratInfo.symbol)) {
                    humanHomologs.add(0, hh);
                } else {
                    humanHomologs.add(hh);
                }
            }
        }

        void printRecord() {

            StringBuilder writer = new StringBuilder(120);
            // output chinchilla fields
            writer.append(checkNull(ratInfo.symbol))
                  .append('\t')
                  .append(ratInfo.rgdID)
                  .append('\t')
                  .append(checkNull(ratInfo.egID))
                  .append('\t');

            // build human fields
            String symbols, rgdIds, egIds;
            symbols = rgdIds = egIds = null;
            for( GeneInfo2 hh: humanHomologs ) {
                symbols = append(symbols, hh.symbol);
                rgdIds = append(rgdIds, Integer.toString(hh.rgdID));
                egIds = append(egIds, hh.egID);
            }

            // output human fields
            writer.append(checkNull(symbols))
                  .append('\t')
                  .append(checkNull(rgdIds))
                  .append('\t')
                  .append(checkNull(egIds))
                  .append('\n');

            line = writer.toString();
        }

        private String checkNull(String str) {
            return str==null ? "" : str.replace('\t', ' ');
        }

        String append(String oldVal, String val) {
            if( oldVal==null )
                return val;
            return oldVal+"|"+val;
        }

        public int compareTo(OrthologRecord o) {
            // we sort homology records by rgd gene symbol
            return ratInfo.symbol.compareToIgnoreCase(o.ratInfo.symbol);
        }

        @Override
         public String toString() {
            return line;
        }
    }

    class GeneInfo2 {
        int rgdID;
        String symbol;
        String egID;

        public GeneInfo2(int rgdID, FtpFileExtractsDAO dao) throws Exception {
            this.rgdID = rgdID;
            this.symbol = dao.getSymbolForMarker(rgdID);
            initEgID(dao);
        }

        void initEgID(FtpFileExtractsDAO dao) throws Exception {
            List<XdbId> xdbIds = dao.getXdbIds(rgdID, XdbId.XDB_KEY_NCBI_GENE);
            if( xdbIds.size() > 1 )
                System.out.println("multiple NCBI Gene references ("+xdbIds.size()+") found for RGD:"+rgdID+", symbol="+symbol);
            else if( xdbIds.isEmpty() )
                System.out.println("missing NCBI Gene reference for RGD:"+rgdID+", symbol="+symbol);

            if( !xdbIds.isEmpty() )
                egID = xdbIds.get(0).getAccId();
        }
    }
}
