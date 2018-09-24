package edu.mcw.rgd;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Nov 29, 2010
 * Time: 12:29:53 PM
 */
public class QtlExtractor  extends BaseExtractor {

    public void run(SpeciesRecord speciesRec) throws Exception {

        String outputFile = speciesRec.getQtlFileName();
        if( outputFile==null )
            return;

        System.out.println(getVersion());

        QtlReporter qtlReporter = new QtlReporter();
        qtlReporter.setDao(getDao());
        qtlReporter.runReporter(speciesRec, getExtractDir());
    }
}
