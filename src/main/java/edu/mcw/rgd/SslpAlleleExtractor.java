package edu.mcw.rgd;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Nov 29, 2010
 * Time: 12:29:30 PM
 */
public class SslpAlleleExtractor extends BaseExtractor {

    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: sslp-alleles-version-2.0.2\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about active #SPECIES# sslp alleles extracted from RGD database\n"
    +"# CONTACT: rgd.developers@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"#\n"
    +"### As of Dec 19 2011 v. 2.0.1: no data changes; improved internal QC.\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"#\n"
    +"#1   SSLP_RGD_ID	           RGD_ID of the sslp allele\n"
    +"#2   RGD_NAME                name of the sslp allele in RGD\n"
    +"#3   STRAIN_SSLP_SIZE        sslp size for given strain\n"
    +"SSLP_RGD_ID\tRGD_NAME";

    Logger log = Logger.getLogger(getClass());

    public void run(SpeciesRecord speciesRec) throws Exception {

        String outputFile = speciesRec.getSslpAllelesFileName();
        if( outputFile==null )
            return;
        outputFile = getExtractDir()+'/'+outputFile;

        // retrieve from database all sslps with allele sizes and strain names
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

        // iterate every sslp with alleles and output allele size at correct column
        for( SslpAlleles rec: sslpAlleles ) {
            writer.print(rec.sslpRgdId + "\t" + checkNull(rec.sslpName));

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
        FtpFileExtractsManager.qcFileContent(outputFile, "sslp_alleles", speciesRec.getSpeciesType());
    }

    private String checkNull(String str) {
        return str==null ? "" : str.replace('\t', ' ');
    }

}
