package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.Strain;
import edu.mcw.rgd.process.Utils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Dump data for active rat strains into disk files, both in xml and tab-separated format.
 */
public class StrainExtractor extends BaseExtractor {

    String version;
    String tsvFileName;
    String xmlFileName;

    /**
     * extract all strains to tab separated file and to xml file
     * @throws Exception
     */
    public void run(SpeciesRecord speciesRec) throws Exception {

        // strain extract is valid only for rat
        if( speciesRec.getSpeciesType() != SpeciesType.RAT ) {
            return;
        }

        System.out.println(getVersion());

        // create species specific output dir
        String outputDir = getSpeciesSpecificExtractDir(speciesRec);

        // create tsv and xml files
        String tsvFilePath = outputDir+'/'+tsvFileName;
        PrintWriter tsvWriter = new PrintWriter(tsvFilePath);
        tsvWriter.println(
        "# RGD-PIPELINE: ftp-file-extracts\n"
       +"# MODULE: strains   build 2019-06-24\n"
       +"# GENERATED-ON: #DATE#\n"
       +"# PURPOSE: information about active rat strains extracted from RGD database\n"
       +"# CONTACT: rgd.data@mcw.edu\n"
       +"# FORMAT: tab delimited text\n"
       +"# NOTES: multiple values in a single column are separated by ';'\n"
       +"RGD_ID\tSTRAIN_SYMBOL\tFULL_NAME\tORIGIN\tSOURCE\tSTRAIN_TYPE\tLAST_KNOWN_STATUS\tRESEARCH_USE"
        +"\tALLELES\tALLELE_RGD_IDS"
        +"\tRGSC_3.4_CHR\tRGSC_3.4_START_POS\tRGSC_3.4_STOP_POS\tRGSC_3.4_METHOD"
        +"\tRNOR_5.0_CHR\tRNOR_5.0_START_POS\tRNOR_5.0_STOP_POS\tRNOR_5.0_METHOD"
        +"\tRNOR_6.0_CHR\tRNOR_6.0_START_POS\tRNOR_6.0_STOP_POS\tRNOR_6.0_METHOD"
        );
        System.out.println("Processing file: "+tsvFilePath);

        String xmlFilePath = outputDir+'/'+xmlFileName;
        PrintWriter xmlWriter = new PrintWriter(xmlFilePath);
        xmlWriter.println("<?xml version='1.0'?>");
        xmlWriter.println("<Strains>");
        System.out.println("Processing file: "+xmlFilePath);

        // iterate all strains
        int counter = 0;
        for( Strain strain: getStrainList() ) {

            List<Gene> strainAlleles = getDao().getStrainsAlleles(strain.getRgdId());
            List<MapData> positions = getDao().getMapData(strain.getRgdId());
            List<MapData> positions3_4 = getPositionsForAssembly(positions, 60);
            List<MapData> positions5_0 = getPositionsForAssembly(positions, 70);
            List<MapData> positions6_0 = getPositionsForAssembly(positions, 360);

            // dump columns in tsv format
            tsvWriter.print(strain.getRgdId());
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(strain.getSymbol()));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(strain.getName()));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(strain.getOrigin()));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(strain.getSource()));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(strain.getStrainTypeName()));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(strain.getLastStatus()));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(strain.getResearchUse()));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(getAlleles(strainAlleles)));
            tsvWriter.print('\t');
            tsvWriter.print(checkWhiteSpace(getAlleleRgdIds(strainAlleles)));
            printTsvPositions(positions3_4, tsvWriter);
            printTsvPositions(positions5_0, tsvWriter);
            printTsvPositions(positions6_0, tsvWriter);
            tsvWriter.println();

            // dump columns in xml format
            xmlWriter.println("  <Strain>");

            writeXmlField(xmlWriter, checkNull(Integer.toString(strain.getRgdId())), "RGD_ID");
            writeXmlField(xmlWriter, checkNull(strain.getSymbol()), "STRAIN_SYMBOL");
            writeXmlField(xmlWriter, checkNull(strain.getName()), "FULL_NAME");
            writeXmlField(xmlWriter, checkNull(strain.getOrigin()), "ORIGIN");
            writeXmlField(xmlWriter, checkNull(strain.getSource()), "SOURCE");
            writeXmlField(xmlWriter, checkNull(strain.getStrainTypeName()), "STRAIN_TYPE");
            writeXmlField(xmlWriter, checkNull(strain.getLastStatus()), "AVAILABILITY");
            writeXmlField(xmlWriter, checkNull(strain.getResearchUse()), "RESEARCH_USE");
            writeXmlField(xmlWriter, checkNull(getAlleles(strainAlleles)), "ALLELES");
            writeXmlField(xmlWriter, checkNull(getAlleleRgdIds(strainAlleles)), "ALLELE_RGD_IDS");

            printXmlPositions(positions, xmlWriter);

            xmlWriter.println("  </Strain>");

            counter++;
        }

        // terminate root xml element
        xmlWriter.println("</Strains>");

        // close the files
        tsvWriter.close();
        xmlWriter.close();

        System.out.println(counter+" strains extracted");

        // copy the files to staging dir
        FtpFileExtractsManager.qcFileContent(tsvFilePath, "strains", speciesRec.getSpeciesType());
    }

    /// strains sorted by RGD ID
    List<Strain> getStrainList() throws Exception {

        List<Strain> strainsInRgd = getDao().getActiveStrains();

        Collections.sort(strainsInRgd, new Comparator<Strain>() {
            @Override
            public int compare(Strain o1, Strain o2) {
                return o1.getRgdId() - o2.getRgdId();
            }
        });

        return strainsInRgd;
    }

    public void printXmlPositions(List<MapData> positions, PrintWriter xmlWriter) {

        xmlWriter.println("    <POSITIONS>");
        for( MapData md: positions ) {
            String assembly = "";
            if( md.getMapKey()==60 )
                assembly="RGSC v.3.4";
            else if( md.getMapKey()==70 )
                assembly="Rnor_5.0";
            else if( md.getMapKey()==360 )
                assembly="Rnor_6.0";

            xmlWriter.println("      <POSITION ASSEMBLY=\""+assembly+"\" CHR=\""+md.getChromosome()
                    +"\" START_POS=\""+md.getStartPos()+"\" STOP_POS=\""+md.getStopPos()
                    +"\" METHOD=\""+md.getMapPositionMethod()+"\"/>");
        }
        xmlWriter.println("    </POSITIONS>");
    }

    public List<MapData> getPositionsForAssembly(List<MapData> positions, int mapKey) {

        List<MapData> results = new ArrayList<MapData>(positions.size());
        for( MapData md: positions ) {
            if( md.getMapKey()==mapKey )
                results.add(md);
        }
        return results;
    }

    public void printTsvPositions(List<MapData> positions, PrintWriter tsvWriter) throws Exception {
        if( positions.isEmpty() )
            tsvWriter.print("\t\t\t\t");
        else {
            tsvWriter.print('\t');
            tsvWriter.print(Utils.concatenate(";",positions,"getChromosome"));
            tsvWriter.print('\t');
            tsvWriter.print(Utils.concatenate(";",positions,"getStartPos"));
            tsvWriter.print('\t');
            tsvWriter.print(Utils.concatenate(";",positions,"getStopPos"));
            tsvWriter.print('\t');
            tsvWriter.print(Utils.concatenate(";",positions,"getMapPositionMethod"));
        }
    }

    public void writeXmlField(PrintWriter writer, String val, String field) {
        if( val==null || val.length()==0 ) {
            writer.append("    <").append(field).append("/>");
        } else {
            writer.append("    <").append(field).append('>')
                    .append(xmlEncode(val))
                    .append("</").append(field).append('>');
        }
        writer.println();
    }

    // very simple xml encoder
    String xmlEncode(String src) {
        return src.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String checkNull(String str) {
        return str==null ? "" : str;
    }

    private String checkWhiteSpace(String str) {
        return str==null ? "" : str.replaceAll("[\\s]+", " ");
    }

    private String getAlleles(List<Gene> alleles) throws Exception {
        return Utils.concatenate("|", alleles, "getSymbol");
    }

    private String getAlleleRgdIds(List<Gene> alleles) throws Exception {
        return Utils.concatenate("|", alleles, "getRgdId");
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTsvFileName() {
        return tsvFileName;
    }

    public void setTsvFileName(String tsvFileName) {
        this.tsvFileName = tsvFileName;
    }

    public String getXmlFileName() {
        return xmlFileName;
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }
}
