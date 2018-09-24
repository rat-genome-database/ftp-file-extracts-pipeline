package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 6/30/14
 * Time: 12:35 PM
 * <p>generates files to be used by researcher Radoslavov; original queries provided by Jennifer:
 * <pre>

 Alleles_to_Genes.txt file columns:
 "ALLELE_RGDID","ALLELE_SYMBOL","ALLELE_NAME","ALLELE_DESCRIPTION","ALLELE_CREATED","ALLELE_LAST_MODIFIED",
 "PARENT_GENE_RGDID","PARENT_GENE_SYMBOL","MAP_NAME","GENE_CHR","GENE_START","GENE_STOP","GENE_STRAND","ALLELE_SYNONYMS"

 CREATED AND LAST MODIFIED DATES FOR active GENES, rat, mouse, human, excluding splices and alleles:
  #RGD_ID	GENE_SYMBOL	GENE_TYPE	SPECIES	CREATED_DATE	LAST_MODIFIED_DATE

QUERIES FOR RAT STRAIN TO PHENOMINER ONTOLOGIES:
strainToCmo
#STRAIN_RGD_ID	STRAIN_ONT_ID	STRAIN_SYMBOL	CLINICAL_MEASUREMENT_ONT_ID	CMO_TERM

strainToMmo
#STRAIN_RGD_ID	STRAIN_ONT_ID	STRAIN_SYMBOL	MEASUREMENT_METHOD_ONT_ID	MMO_TERM

strainToXco
#STRAIN_RGD_ID	STRAIN_ONT_ID	STRAIN_SYMBOL	EXP_COND_ONT_ID	XCO_TERM	EXP_COND_VALUE_MIN	EXP_COND_VALUE_MAX	EXP_COND_UNITS
 * </pre>
 */
public class RadoslavovExtractor extends BaseExtractor {

    private String version;
    private String outputDir;
    private String ratGenesFile;
    private String mouseGenesFile;
    private String humanGenesFile;

    static SimpleDateFormat _sdtDate = new SimpleDateFormat("yyyy-MM-dd");
    private String strainToCmoFile;
    private String strainToMmoFile;
    private String strainToXcoFile;
    private String allelesToGenesFile;
    private String splicesToGenesFile;

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        // make sure output dir does exist
        String dirBase = this.getExtractDir()+"/"+getOutputDir();
        new File(dirBase).mkdirs();
        dirBase += "/";

        if( speciesInfo.getSpeciesType()==SpeciesType.RAT ) {
            extractOntFile(getDao().strainToCmo(), dirBase+getStrainToCmoFile());
            extractOntFile(getDao().strainToMmo(), dirBase+getStrainToMmoFile());
            extractOntFile(getDao().strainToXco(), dirBase+getStrainToXcoFile());

            extractGenesFile(SpeciesType.RAT, dirBase+getRatGenesFile());

            extractOntFile(getDao().allelesToGenes(), dirBase+getAllelesToGenesFile());
            extractOntFile(getDao().splicesToGenes(), dirBase+getSplicesToGenesFile());
        }
        else if( speciesInfo.getSpeciesType()==SpeciesType.MOUSE ) {
            extractGenesFile(SpeciesType.MOUSE, dirBase+getMouseGenesFile());
        } else if( speciesInfo.getSpeciesType()==SpeciesType.HUMAN ) {
            extractGenesFile(SpeciesType.HUMAN, dirBase+getHumanGenesFile());
        }
    }

    public void extractOntFile(List<String[]> rows, String fileName) throws Exception {

        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        for( String[] row: rows ) {
            int colsWritten = 0;
            for( String col: row ) {
                if( colsWritten>0 )
                    writer.write('\t');
                if( col!=null )
                    writer.write(col);
                colsWritten++;
            }
            writer.newLine();
        }
        writer.close();

        System.out.println("written "+(rows.size()-1)+" rows for "+fileName);
    }

    public void extractGenesFile(int speciesTypeKey, String fileName) throws Exception {

        String species = SpeciesType.getCommonName(speciesTypeKey).toLowerCase();

        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write("RGD_ID\tGENE_SYMBOL\tGENE_TYPE\tSPECIES\tCREATED_DATE\tLAST_MODIFIED_DATE");
        writer.newLine();

        int rows = 0;
        for( Gene gene: getDao().getActiveGenes(speciesTypeKey) ) {
            RgdId id = getDao().getRgdId(gene.getRgdId());
            writer.write(id.getRgdId()+"");
            writer.write('\t');
            writer.write(gene.getSymbol());
            writer.write('\t');
            writer.write(gene.getType()!=null ? gene.getType() : "");
            writer.write('\t');
            writer.write(species);
            writer.write('\t');
            writer.write(writeDate(id.getCreatedDate()));
            writer.write('\t');
            writer.write(writeDate(id.getLastModifiedDate()));
            writer.newLine();
            rows++;
        }
        writer.close();

        System.out.println("written "+rows+" rows for "+fileName);
    }

    String writeDate(Date dt) {
        if( dt==null )
            return "";
        return _sdtDate.format(dt);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setRatGenesFile(String ratGenesFile) {
        this.ratGenesFile = ratGenesFile;
    }

    public String getRatGenesFile() {
        return ratGenesFile;
    }

    public void setMouseGenesFile(String mouseGenesFile) {
        this.mouseGenesFile = mouseGenesFile;
    }

    public String getMouseGenesFile() {
        return mouseGenesFile;
    }

    public void setHumanGenesFile(String humanGenesFile) {
        this.humanGenesFile = humanGenesFile;
    }

    public String getHumanGenesFile() {
        return humanGenesFile;
    }

    public void setStrainToCmoFile(String strainToCmoFile) {
        this.strainToCmoFile = strainToCmoFile;
    }

    public String getStrainToCmoFile() {
        return strainToCmoFile;
    }

    public void setStrainToMmoFile(String strainToMmoFile) {
        this.strainToMmoFile = strainToMmoFile;
    }

    public String getStrainToMmoFile() {
        return strainToMmoFile;
    }

    public void setStrainToXcoFile(String strainToXcoFile) {
        this.strainToXcoFile = strainToXcoFile;
    }

    public String getStrainToXcoFile() {
        return strainToXcoFile;
    }

    public void setAllelesToGenesFile(String allelesToGenesFile) {
        this.allelesToGenesFile = allelesToGenesFile;
    }

    public String getAllelesToGenesFile() {
        return allelesToGenesFile;
    }

    public void setSplicesToGenesFile(String splicesToGenesFile) {
        this.splicesToGenesFile = splicesToGenesFile;
    }

    public String getSplicesToGenesFile() {
        return splicesToGenesFile;
    }
}
