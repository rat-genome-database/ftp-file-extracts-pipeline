package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.datamodel.SSLP;
import edu.mcw.rgd.process.Utils;

import java.io.PrintWriter;
import java.util.*;

/**
 * @author mtutaj
 * @since Nov 29, 2010
 */
public class SslpAlleleExtractor extends BaseExtractor {

    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: marker-alleles  build 2022-12-02\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# marker alleles extracted from RGD database\n"
    +"# CONTACT: rgd.developers@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"#\n"
    +"### As of Dec 19 2011: no data changes; improved internal QC.\n"
    +"### As of Dec 02 2022: added columns with rn7 positions and forward/reverse primer sequences.\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"#\n"
    +"#1   MARKER_RGD_ID	   RGD_ID of the marker allele\n"
    +"#2   RGD_NAME            name of the marker allele in RGD\n"
    +"#3   RN7_CHR             chromosome on mRatBN7.2 (rn7) assembly, if available\n"
    +"#4   RN7_START_POS       start pos on mRatBN7.2 (rn7) assembly, if available\n"
    +"#5   RN7_STOP_POS        stop pos on mRatBN7.2 (rn7) assembly, if available\n"
    +"#6   FORWARD_SEQ         PCR forward primer sequence, if available\n"
    +"#7   REVERSE_SEQ         PCR reverse primer sequence, if available\n"
    +"#8   STRAIN_MARKER_SIZE  marker size for given strain\n"
    +"MARKER_RGD_ID\tRGD_NAME\tRN7_CHR\tRN7_START_POS\tRN7_STOP_POS\tFORWARD_SEQ\tREVERSE_SEQ";

    public static int RN7_MAP_KEY = 372;

    public void run(SpeciesRecord speciesRec) throws Exception {

        String outputFileName = speciesRec.getMarkerAllelesFileName();
        if( outputFileName==null )
            return;
        String outputFile = getSpeciesSpecificExtractDir(speciesRec)+'/'+outputFileName;

        // retrieve from database all markers with allele sizes and strain names
        Set<String> strainNames = new TreeSet<String>();
        List<SslpAlleles> sslpAlleles = getDao().getSslpAlleles(speciesRec.getSpeciesType(), strainNames);

        // create output file name
        PrintWriter writer = new PrintWriter(outputFile);
        String commonLines = HEADER_COMMON_LINES
                .replace("#SPECIES#", speciesRec.getSpeciesName().toLowerCase())
                .replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.print(commonLines);

        // sort strain names and output the names
        for( String strainName: strainNames ) {
            writer.print("\t" + checkNull(strainName));
        }
        writer.println();

        // iterate every marker with alleles and output allele size at correct column
        for( SslpAlleles rec: sslpAlleles ) {

            writer.print(rec.markerRgdId + "\t" + checkNull(rec.markerName));

            List<MapData> mds = getDao().getMapData(rec.markerRgdId, RN7_MAP_KEY);
            writer.print(formatPos(mds));

            // write out PCR sequences
            SSLP marker = getDao().getMarker(rec.markerRgdId);
            writer.print('\t');
            writer.print(checkNull(marker.getForwardSeq()));
            writer.print('\t');
            writer.print(checkNull(marker.getReverseSeq()));
            //if( !Utils.isStringEmpty(marker.getTemplateSeq()) ) {
            //    System.out.println("aha");
            //}

            // iterate all strain names - columns and output any allele size matching the strain name
            for( String strainName: strainNames ) {
                writer.print('\t');

                Integer sslpSize = rec.strainNamesToAlleleSizes.get(strainName);
                if( sslpSize!=null )
                    writer.print(sslpSize);
            }
            writer.println();
        }
        writer.close();

        // copy the output file to the staging area
        FtpFileExtractsManager.qcFileContent(outputFile, "marker_alleles", speciesRec.getSpeciesType());
    }

    String formatPos(List<MapData> mds) throws Exception {

        // print chromosomes
        return "\t"
        + checkNull(Utils.concatenate(";", mds, "getChromosome"))

        // print start pos
        + "\t"
        + checkNull(Utils.concatenate(";", mds, "getStartPos"))

        // print stop pos
        + "\t"
        + checkNull(Utils.concatenate(";", mds, "getStopPos"));
    }

    private String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

}
