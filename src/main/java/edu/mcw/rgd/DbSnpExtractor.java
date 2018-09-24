package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.pipelines.PipelineManager;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordPreprocessor;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Nov 29, 2010
 * Time: 12:04:40 PM
 */
public class DbSnpExtractor extends BaseExtractor {

    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: dbsnp-version-1.0.2\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# genes (assembly build #ASSEMBLY#) associated with #SPECIES# db snps (#BUILD#) extracted from RGD database\n"
    +"# CONTACT: rgd.developers@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by '|'\n"
    +"#\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"#\n"
    +"#1   DB_SNP_ID	    RS id of SNP in DB_SNP database\n"
    +"#2   CHROMOSOME       chromosome\n"
    +"#3   POSITION         position on chromosome\n"
    +"#4   ALLELE           allele nucleotide(s). All possible alleles are listed in alphabetical order; named indels are listed by name, e.g. (D1RAT392)\n"
    +"#5   GENE_SYMBOL      symbol of gene in RGD\n"
    +"#6   GENE_RGD_ID      rgd id of gene\n"
    +"#7   GENE_NCBI_ID     NCBI GeneID\n"
    +"#\n"
    +"DB_SNP_ID\tCHROMOSOME\tPOSITION\tALLELE\tGENE_SYMBOL\tGENE_RGD_ID\tGENE_NCBI_ID";

    final String HEADER_LINE_RAT =
    "";

    final String HEADER_LINE_HUMAN =
    "";

    final String HEADER_LINE_MOUSE =
    "";

    Logger log = Logger.getLogger(getClass());

    public void run(SpeciesRecord speciesInfo) throws Exception {

        final int speciesType = speciesInfo.getSpeciesType();
        this.loadConfig(speciesType);
        String species = speciesInfo.getSpeciesName();

        String outputFile = getConfig().get("outputFileName");
        outputFile = getExtractDir()+'/'+outputFile;


        final PrintWriter writer = new PrintWriter(new GZIPOutputStream(new FileOutputStream(outputFile)));

        // prepare header common lines
        String commonLines = HEADER_COMMON_LINES
                .replace("#SPECIES#", species)
                .replace("#ASSEMBLY#", getConfig().get("assembly"))
                .replace("#BUILD#", getConfig().get("dbSnpBuild"))
                .replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.print(commonLines);
        writer.println(speciesType== SpeciesType.RAT ? HEADER_LINE_RAT :
                speciesType==SpeciesType.HUMAN ? HEADER_LINE_HUMAN :
                HEADER_LINE_MOUSE);

        // create pipeline managing framework
        PipelineManager manager = new PipelineManager();

        // setup pipeline parser "DB" - 1 thread -- max 10000 records in output queue
        manager.addPipelineWorkgroup(new RecordPreprocessor() {
            // parser: break source into a stream of record-s
            public void process() throws Exception {

                String query = ""+
                    "select s.snp_name,s.chromosome,s.position,allele,gene_symbols_lc "+
                    "from db_snp s,gene_loci l "+
                    "where s.source=? and s.map_key=? "+
                    "and l.map_key=s.map_key and l.chromosome=s.chromosome and l.pos=s.position "+
                    "and l.genic_status='genic' " +
                    "ORDER BY s.map_key,s.source,s.chromosome,s.position,l.gene_symbols_lc,s.allele";

                int recNo = 0;
                int mapKey = Integer.parseInt(getConfig().get("mapKey"));
                String source = getConfig().get("dbSnpBuild");
                Connection conn = null;
                try {
                    conn =  getDao().getConnection();
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, source);
                    ps.setInt(2, mapKey);

                    ResultSet rs = ps.executeQuery();
                    DbSnpRecord rec = null;

                    // combine alleles for same rs and gene symbol
                    while(rs.next()) {

                        String rsId = rs.getString(1);
                        String geneSymbolLc = rs.getString(5);

                        if( rec!=null && Utils.stringsAreEqual(rsId, rec.rsId) && Utils.stringsAreEqual(geneSymbolLc, rec.geneSymbolLc) ) {
                            // same rs_id and gene symbol -- combine alleles
                            String allele = rs.getString(4);
                            if( !rec.allele.contains(allele) )
                                rec.allele += "/" + allele;
                        }
                        else {
                            // new rec
                            if( rec!=null )
                                getSession().putRecordToFirstQueue(rec);
                            rec = new DbSnpRecord();
                            rec.setRecNo(++recNo);
                            rec.rsId = rsId;
                            rec.chr = rs.getString(2);
                            rec.pos = rs.getInt(3);
                            rec.allele = rs.getString(4);
                            rec.geneSymbolLc = geneSymbolLc;
                        }
                    }
                    if( rec!=null )
                        getSession().putRecordToFirstQueue(rec);
                }
                finally {
                    try {
                        conn.close();
                    }catch (Exception ignored) {
                    }
                }
            }
        }, "DB", 1, 10000);


        // setup pipeline "QC" - several parallel threads -- max 10000 records in output queue
        manager.addPipelineWorkgroup(new RecordProcessor() {

            Set<String> noMatchMsgSet = new HashSet<String>();
            Set<String> multiMatchMsgSet = new HashSet<String>();

            // gather data from database
            public void process(PipelineRecord r) throws Exception {
                DbSnpRecord rec = (DbSnpRecord) r;

                if( rec.getRecNo()%100==0 )
                    log.debug("QC recno="+rec.getRecNo());

                //load genes by symbol
                List<Gene> genes = getDao().getActiveGenes(speciesType, rec.geneSymbolLc);
                if( genes.isEmpty() ) {
                    String msg = "PROBLEM: no gene matches symbol " + rec.geneSymbolLc;
                    if( noMatchMsgSet.add(msg) ) {
                        log.debug(msg);
                        getSession().incrementCounter("NO_GENE_MATCHES_SYMBOL", 1);
                    }
                    return;
                }
                if( genes.size()>1 ) {
                    String msg = "PROBLEM: multiple genes match symbol "+rec.geneSymbolLc;
                    if( multiMatchMsgSet.add(msg) ) {
                        log.debug(msg);
                        getSession().incrementCounter("MULTIPLE_GENE_MATCH_SYMBOL", 1);
                    }
                    return;
                }
                rec.gene = genes.get(0);

                // load NCBI gene ids for gene
                rec.ncbiGeneIds = getNcbiGeneIds(rec.gene.getRgdId());
            }
        }, "QC", getQcThreadCount(), 0);

        // setup data loading pipeline "DL" - 1 thread; writing records to output file
        manager.addPipelineWorkgroup(new RecordProcessor() {
            // write record to a line in output file
            public void process(PipelineRecord r) throws Exception {
                DbSnpRecord rec = (DbSnpRecord) r;

                // write out all the parameters to the file
                writeLine(rec, writer);
            }
        }, "DL", 1, 0);

        // run pipelines
        manager.run();

        // close the output file
        writer.close();

        manager.dumpCounters();

        // copy the output file to the staging area
        FtpFileExtractsManager.qcFileContent(outputFile, "db_snp", speciesType);
    }

    void writeLine(DbSnpRecord rec, PrintWriter writer) throws Exception {

        // DB_SNP_ID
        writer.print(rec.rsId);
        writer.print('\t');
        // chromosome
        writer.print(rec.chr);
        writer.print('\t');
        // pos
        writer.print(rec.pos);
        writer.print('\t');
        // allele
        writer.print(rec.allele);
        writer.print('\t');

        if( rec.gene!=null ) {
            // gene symbol
            writer.print(Utils.defaultString(rec.gene.getSymbol()));
            writer.print('\t');
            // gene rgd id
            writer.print(rec.gene.getRgdId());
            writer.print('\t');
            // ncbi gene id
            writer.print(rec.ncbiGeneIds);
        }
        else {
            writer.print('\t');
            writer.print('\t');
        }

        // terminate the line
        writer.println();
    }

    String getNcbiGeneIds(int rgdId) throws Exception {

        String ncbiGeneIds = _cacheNcbiGeneIds.get(rgdId);
        if( ncbiGeneIds==null ) {
            ncbiGeneIds = "";
            for( XdbId xdbId: getDao().getXdbIds(rgdId, XdbId.XDB_KEY_NCBI_GENE) ) {
                if( !ncbiGeneIds.isEmpty() )
                    ncbiGeneIds += "|";
                ncbiGeneIds += xdbId.getAccId();
            }
            _cacheNcbiGeneIds.put(rgdId, ncbiGeneIds);
        }
        return ncbiGeneIds;
    }
    java.util.Map<Integer, String> _cacheNcbiGeneIds = new HashMap<>();

    class DbSnpRecord extends PipelineRecord {

        public String rsId;
        public String chr;
        public int pos;
        public String allele;
        public String geneSymbolLc;
        public Gene gene;
        public String ncbiGeneIds;
    }
}
