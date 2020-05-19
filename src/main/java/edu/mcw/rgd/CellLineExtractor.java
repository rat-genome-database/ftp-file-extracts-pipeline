package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.*;

/**
 * Created by mtutaj on May 19, 2020
 */
public class CellLineExtractor extends BaseExtractor {

    static boolean versionPrintedOut = false;

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        if( versionPrintedOut ) {
            return;
        }
        versionPrintedOut = true;
        System.out.println(getVersion());

        StringBuffer header = new StringBuffer();
        Collection<String> dataLines = generateDataLines(header);

        String tsvFilePath = getExtractDir()+"/"+"CELL_LINES.txt";

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        PrintWriter tsvWriter = new PrintWriter(tsvFilePath);
        tsvWriter.write(
            "# RGD-PIPELINE: ftp-file-extracts\n"
            +"# MODULE: cell lines   build 2020-05-19\n"
            +"# GENERATED-ON: "+dateFormat.format(new Date())+"\n"
            +"# CONTACT: rgd.data@mcw.edu\n"
            +"# FORMAT: tab delimited text\n"
            +"# NOTES:\n"
            +"#\tThis file provides cell line data in RGD.\n"
            +"#\tThe cell line data is either curated in RGD, or imported from Cellosaurus resource.\n"
            +"#\tThe file is in the tab-delimited format.\n"
            +"#\tMultiple values in a single column are separated by ';'\n"
            +"# COUNT: "+ dataLines.size()+" cell lines\n"
            +header.toString()
        );

        for( String line: dataLines ) {
            tsvWriter.write(line);
        }
        tsvWriter.close();

        System.out.println("  written cell lines: "+dataLines.size());
    }

    Collection<String> generateDataLines(StringBuffer header) throws Exception {

        Set<String> sortedLines = new TreeSet<>();

        List<CellLine> cellLines = getDao().getActiveCellLines();
        Map<Integer, String> cellosaurus2RgdIdMap = getCellosaurus2RgdIdMap();

        header.append("RGD ID\t");
        header.append("Symbol\t");
        header.append("Name\t");
        header.append("Type\t");
        header.append("Species\t");
        header.append("Cellosaurus ID\t");
        header.append("Description\t");
        header.append("Source\t");
        header.append("SO_ACC_ID\t");
        header.append("Genomic Alteration\t");
        header.append("Notes\t");
        header.append("Origin\t");
        header.append("Research Use\t");
        header.append("Availability\t");
        header.append("Gender\t");
        header.append("Characteristics\t");
        header.append("Phenotype\t");
        header.append("Germline Competent\t");
        header.append("Groups\t");
        header.append("Caution\n");

        for (CellLine c: cellLines) {

            String speciesType = "";
            if( c.getSpeciesTypeKey()>0 ) {
                speciesType = SpeciesType.getGenebankCommonName(c.getSpeciesTypeKey());
            }
            String cellosaurusId = Utils.NVL(cellosaurus2RgdIdMap.get(c.getRgdId()), "");

            String line =
                c.getRgdId()+"\t"+
                normalizeString(c.getSymbol())+"\t"+
                normalizeString(c.getName())+"\t"+
                normalizeString(c.getObjectType())+"\t"+
                speciesType+"\t"+
                cellosaurusId+"\t"+
                normalizeString(c.getDescription())+"\t"+
                normalizeString(c.getSource())+"\t"+
                c.getSoAccId()+"\t"+
                normalizeString(c.getGenomicAlteration())+"\t"+
                normalizeString(c.getNotes())+"\t"+
                normalizeString(c.getOrigin())+"\t"+
                normalizeString(c.getResearchUse())+"\t"+
                normalizeString(c.getAvailability())+"\t"+
                normalizeString(c.getGender())+"\t"+
                normalizeString(c.getCharacteristics())+"\t"+
                normalizeString(c.getPhenotype())+"\t"+
                normalizeString(c.getGermlineCompetent())+"\t"+
                normalizeString(c.getGroups())+"\t"+
                normalizeString(c.getCaution())+
                "\n";

            sortedLines.add(line);
        }

        return sortedLines;
    }

    Map<Integer, String> getCellosaurus2RgdIdMap() throws Exception {

        Map<Integer, String> result = new HashMap<>();
        List<XdbId> xdbIds = getDao().getActiveXdbIds(XdbId.XDB_KEY_CELLOSAURUS, RgdId.OBJECT_KEY_CELL_LINES);
        for( XdbId id: xdbIds ) {

            String oldCellosaurusId = result.put(id.getRgdId(), id.getAccId());
            if( oldCellosaurusId!=null && !Utils.stringsAreEqual(oldCellosaurusId, id.getAccId()) ) {
                System.out.println("conflict: multiple cellosaurus ids for RGD:"+id.getRgdId());
            }
        }
        return result;
    }

    /** no string can contain *new line* or *tab* characters, because they will break the TSV file
     */
    public String normalizeString(String str) {

        if( str==null ) {
            return "";
        }
        return str.replaceAll("[\\s]+", " ");
    }
}
