package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 10/22/12
 * Time: 1:39 PM
 * When a file is generated, we count number of rows having no value for every column;
 * We report columns having unusual number of null values.
 * Reporting is suppressed for columns named '(UNUSED)'
 */
public class FileContentQC {

    // if there are more than 'NO_VALUE_THRESHOLD' rows for a column, it is marked as failed QC
    public static final int NO_VALUE_THRESHOLD = 5;

    public String validate(String file, String module, int speciesTypeKey) throws IOException {

        String result = "";

        String line;
        String[] colHeaders = null;
        int colCounts[] = null; // counts of columns with a value

        BufferedReader reader;
        if( file.endsWith(".gz") ) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
        } else {
            reader = new BufferedReader(new FileReader(file));
        }

        while( (line=reader.readLine())!=null ) {

            // skip comment lines
            if( line.startsWith("#") )
                continue;

            // read columns
            if( colHeaders==null ) {
                colHeaders = line.split("[\t]", -1);
                colCounts = new int[colHeaders.length];
                continue;
            }

            // check if any column has no value
            String[] cols = line.split("[\t]", -1);
            for( int i=0; i<cols.length && i<colHeaders.length; i++ ) {
                if( !cols[i].trim().isEmpty() )
                    colCounts[i]++;
            }
        }
        reader.close();

        // generate the report
        if( colHeaders!=null )
        for( int i=0; i<colHeaders.length; i++ ) {

            if( colCounts[i] < NO_VALUE_THRESHOLD ) {

                // suppress reporting if column name is '(UNUSED)'
                if( colCounts[i]<=0 && colHeaders[i].equals("(UNUSED)") ) {
                    continue;
                }

                // generate usage report for this column
                if( result.isEmpty() )
                    result = module +" "+ SpeciesType.getCommonName(speciesTypeKey)+" file "+file+"\n";
                result += " column ["+colHeaders[i]+"] has "+colCounts[i]+" rows with a value\n";
            }
        }

        return result;
    }
}
