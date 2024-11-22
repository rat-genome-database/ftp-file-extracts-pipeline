package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * @author mtutaj
 * @since Nov 29, 2010
 * Base class with functionality common for all extractors
 */
public abstract class BaseExtractor {

    private FtpFileExtractsDAO dao;
    private String extractDir; // temporary directory where the files will be generated
    private int qcThreadCount;
    private Map ratConfig;
    private Map mouseConfig;
    private Map humanConfig;
    private String version;
    private Map<String, String> config;
    private Map<String, String> cmdLineProperties = new HashMap<>();

    // runs the file extraction for given species
    abstract public void run(SpeciesRecord speciesInfo) throws Exception;

    public void go(SpeciesRecord speciesInfo) throws Exception {

        if( speciesInfo!=null ) {
            // run the extractor
            run(speciesInfo);
        }
    }

    public void loadConfig(int speciesTypeKey) {

        System.out.println(getVersion());

        this.config =
                speciesTypeKey== SpeciesType.RAT ? ratConfig
                : speciesTypeKey==SpeciesType.MOUSE ? mouseConfig
                : speciesTypeKey==SpeciesType.HUMAN ? humanConfig
                : null;
    }

    public void writeDataLines(Writer out, Map<Integer,String> lineMap) throws IOException {

        // the original data map is unsorted -- let's sort it by rgd id
        Map<Integer, String> sortedLineMap = new TreeMap<>();
        for( Map.Entry<Integer,String> entry: lineMap.entrySet() ) {
            sortedLineMap.put(entry.getKey(), entry.getValue());
        }
        for( Map.Entry<Integer,String> entry: sortedLineMap.entrySet() ) {
            out.write(entry.getValue());
        }
    }

    public String getSpeciesSpecificExtractDir(SpeciesRecord si) {
        String outputDir = getExtractDir()+'/'+si.getSpeciesShortName().toUpperCase();
        new File(outputDir).mkdirs(); // ensure the species specific directory does exist
        return outputDir;
    }

    public List<GeneExtractRecord> loadGeneRecords(int speciesTypeKey) throws Exception {
        List<Gene> genesInRgd = getDao().getActiveGenes(speciesTypeKey);
        List<GeneExtractRecord> result = new ArrayList<>(genesInRgd.size());

        for( Gene gene: genesInRgd ) {
            GeneExtractRecord rec = new GeneExtractRecord();
            rec.setGeneKey(gene.getKey());
            rec.setRgdId(gene.getRgdId());
            rec.setGeneSymbol(gene.getSymbol());
            rec.setGeneFullName(gene.getName());
            rec.setGeneDesc(Utils.getGeneDescription(gene));
            rec.setRefSeqStatus(gene.getRefSeqStatus());

            // NCBI cannot handle gene types of type 'protein_coding'
            // replace 'protein_coding' into 'protein-coding'
            String geneType = gene.getType();
            if( geneType!=null && geneType.equals("protein_coding") ) {
                geneType = "protein-coding";
            }
            rec.setGeneType(geneType);

            result.add(rec);
        }
        return result;
    }


    public FtpFileExtractsDAO getDao() {
        return dao;
    }

    public void setDao(FtpFileExtractsDAO dao) {
        this.dao = dao;
    }

    public String getExtractDir() {
        return extractDir;
    }

    public void setExtractDir(String extractDir) {
        this.extractDir = extractDir;
    }

    public void setQcThreadCount(int qcThreadCount) {
        this.qcThreadCount = qcThreadCount;
    }

    public int getQcThreadCount() {
        return qcThreadCount;
    }

    public void setRatConfig(Map ratConfig) {
        this.ratConfig = ratConfig;
    }

    public Map getRatConfig() {
        return ratConfig;
    }

    public void setMouseConfig(Map mouseConfig) {
        this.mouseConfig = mouseConfig;
    }

    public Map getMouseConfig() {
        return mouseConfig;
    }

    public void setHumanConfig(Map humanConfig) {
        this.humanConfig = humanConfig;
    }

    public Map getHumanConfig() {
        return humanConfig;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public Map<String, String> getCmdLineProperties() {
        return cmdLineProperties;
    }

    public void setCmdLineProperties(Map<String, String> cmdLineProperties) {
        this.cmdLineProperties = cmdLineProperties;
    }
}
