package edu.mcw.rgd;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mtutaj
 * @since Nov 29, 2010
 */
public class SslpAlleleExtractor extends BaseExtractor {

    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: marker-alleles  build 2019-06-24\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# marker alleles extracted from RGD database\n"
    +"# CONTACT: rgd.developers@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"#\n"
    +"### As of Dec 19 2011 v. 2.0.1: no data changes; improved internal QC.\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"#\n"
    +"#1   MARKER_RGD_ID	       RGD_ID of the marker allele\n"
    +"#2   RGD_NAME                name of the marker allele in RGD\n"
    +"#3   STRAIN_MARKER_SIZE      marker size for given strain\n"
    +"MARKER_RGD_ID\tRGD_NAME";

    Logger log = Logger.getLogger(getClass());

    public void run(SpeciesRecord speciesRec) throws Exception {

        String outputFileName = speciesRec.getMarkerAllelesFileName();
        if( outputFileName==null )
            return;
        String outputFile = getSpeciesSpecificExtractDir(speciesRec)+'/'+outputFileName;

        // retrieve from database all markers with allele sizes and strain names
        Set<String> strainNamesSet = new HashSet<String>();
        List<SslpAlleles> sslpAlleles = getDao().getSslpAlleles(speciesRec.getSpeciesType(), strainNamesSet);

        // create output file name
        PrintWriter writer = new PrintWriter(outputFile);
        String commonLines = HEADER_COMMON_LINES
                .replace("#SPECIES#", speciesRec.getSpeciesName().toLowerCase())
                .replace("#DATE#", SpeciesRecord.getTodayDate());
        writer.print(commonLines);

        // sort strain names and output the names
        String[] strainNames = strainNamesSet.toArray(new String[strainNamesSet.size()]);
        Arrays.sort(strainNames);
        for( String strainName: strainNames ) {
            writer.print("\t" + checkNull(strainName));
        }
        writer.println();

        // iterate every marker with alleles and output allele size at correct column
        for( SslpAlleles rec: sslpAlleles ) {
            writer.print(rec.markerRgdId + "\t" + checkNull(rec.markerName));

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

    private String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

}
