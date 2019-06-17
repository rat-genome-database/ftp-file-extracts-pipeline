package edu.mcw.rgd;

import java.io.File;

/**
 * @author mtutaj
 * @since Nov 29, 2010
 */
public class QtlExtractor  extends BaseExtractor {

    public void run(SpeciesRecord speciesRec) throws Exception {

        String outputFile = speciesRec.getQtlFileName();
        if( outputFile==null )
            return;

        System.out.println(getVersion());

        // create species specific output dir
        String outputDir = getExtractDir()+'/'+speciesRec.getSpeciesName().toUpperCase();
        new File(outputDir).mkdirs();

        QtlReporter qtlReporter = new QtlReporter();
        qtlReporter.setDao(getDao());
        qtlReporter.runReporter(speciesRec, outputDir);
    }
}
