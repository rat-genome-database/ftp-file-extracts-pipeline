package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;

import java.io.PrintWriter;
import java.util.*;

public class ArrayIdExtractor extends BaseExtractor {

    private static final String HEADER =
        "# RGD-PIPELINE: ftp-file-extracts\n"
        +"# MODULE: array-ids-version-1.0.1\n"
        +"# GENERATED-ON: #DATE#\n"
        +"# PURPOSE: microarray probe IDs to gene RGD ID mapping data\n"
        +"# CONTACT: rgd.data@mcw.edu\n"
        +"# FORMAT: tab delimited text\n"
        +"#\n"
        +"GENE_RGD_ID\tGENE_SYMBOL\tNCBI_GENE_ID\tENSEMBL_GENE_ID\tARRAY_ID\tPROBE_SET_ID\n";

    private String version;
    private String outputDir;
    private String fileNamePrefix;

    /**
     * extract gene array ids from Ensembl to tab separated file
     * @throws Exception
     */
    public void run(SpeciesRecord speciesRec) throws Exception {

        System.out.println(getVersion());

        List<Alias> aliases = getDao().getActiveArrayIdAliasesFromEnsembl(RgdId.OBJECT_KEY_GENES, speciesRec.getSpeciesType());
        System.out.println("  array id aliases from Ensembl for "+speciesRec.getSpeciesName()+" available: "+aliases.size());
        if( aliases.isEmpty() ) {
            return;
        }

        // load gene symbols
        java.util.Map<Integer, String> geneSymbols = new HashMap<>();
        for( Gene gene: getDao().getActiveGenes(speciesRec.getSpeciesType()) ) {
            geneSymbols.put(gene.getRgdId(), gene.getSymbol());
        }
        System.out.println("  gene symbols loaded: "+geneSymbols.size());

        // create csv file and write the header
        String filePath = getOutputDir()+"/"+getFileNamePrefix()+speciesRec.getSpeciesName().toUpperCase()+".txt";
        PrintWriter writer = new PrintWriter(filePath);
        String header = HEADER.replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.print(header);
        System.out.println("  processing file: "+filePath);

        int dataLineCount = 0;
        for( Alias alias: aliases ) {

            Collection<String> geneIds = getXdbIds(alias.getRgdId(), XdbId.XDB_KEY_NCBI_GENE);
            Collection<String> ensemblIds = getXdbIds(alias.getRgdId(), XdbId.XDB_KEY_ENSEMBL_GENES);

            if( geneIds.size()>1 ) {
                System.out.println("*** WARN: multiple gene ids for "+alias.getRgdId());
            }

            // get array id: 'array_id_affy_huex_1_0_st_v2_ensembl' --> 'affy_huex_1_0_st_v2'
            String arrayId = alias.getTypeName().substring(9, alias.getTypeName().length()-8);

            for( String geneId: geneIds ) {
                for( String ensemblId: ensemblIds ) {
                    writer.print(alias.getRgdId()+"\t");
                    writer.print(geneSymbols.get(alias.getRgdId())+"\t");
                    writer.print(geneId+"\t");
                    writer.print(ensemblId+"\t");
                    writer.print(arrayId+"\t");
                    writer.print(alias.getValue()+"\n");
                    dataLineCount++;
                }
            }
        }

        writer.close();

        System.out.println("  data lines written: "+dataLineCount);
    }

    java.util.Map<Integer, List<XdbId>> xdbIdMap = new HashMap<>();

    Collection<String> getXdbIds(int rgdId, int xdbKey) throws Exception {
        List<XdbId> xdbIds = getXdbIds(rgdId);
        Set<String> accIds = new TreeSet<>();
        for( XdbId id: xdbIds ) {
            if( id.getXdbKey()==xdbKey ) {
                accIds.add(id.getAccId());
            }
        }
        if( accIds.isEmpty() ) {
            accIds.add("");
        }
        return accIds;
    }

    List<XdbId> getXdbIds(int geneRgdId) throws Exception {
        List<XdbId> xdbIds = xdbIdMap.get(geneRgdId);
        if( xdbIds==null ) {
            List<Integer> xdbKeys = new ArrayList<>();
            xdbKeys.add(XdbId.XDB_KEY_NCBI_GENE);
            xdbKeys.add(XdbId.XDB_KEY_ENSEMBL_GENES);

            xdbIds = getDao().getXdbIdsByRgdId(xdbKeys, geneRgdId);
            xdbIdMap.put(geneRgdId, xdbIds);
        }
        return xdbIds;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setFileNamePrefix(String fileNamePrefix) {
        this.fileNamePrefix = fileNamePrefix;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }
}
