package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mtutaj
 * @since Oct 11, 2010
 */
public class FtpFileExtractsManager {

    public static final String MULTIVAL_SEPARATOR = ";"; // multi-value field separator

    String extractDir; // directory where the files will be generated
    FtpFileExtractsDAO dao = new FtpFileExtractsDAO();
    //Log log = LogFactory.getLog("core");
    private String version;
    private Map<String, SpeciesRecord> speciesInfo;

    static public void main(String[] args) throws Exception {
        if( args.length==0 ) {
            usage();
        }

        try {
            main2(args);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-2);
        }
    }

    static public void main2(String[] args) throws Exception {

        long time0 = System.currentTimeMillis();

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        FtpFileExtractsManager manager = (FtpFileExtractsManager) (bf.getBean("manager"));
        System.out.println(manager.getVersion());

        String beanId = null;
        int speciesTypeKey = SpeciesType.ALL;
        boolean generateObsoleteIds = false;
        String annotDirOverride = null;
        boolean agr = false; // generate for AGR

        for( String arg: args ) {
            switch( arg.toLowerCase() ) {
                // extractors
                case "-genes":
                    beanId = "geneExtractor";
                    break;

                case "-strains":
                    beanId = "strainExtractor";
                    break;

                case "-qtls":
                    beanId = "qtlExtractor";
                    break;

                case "-gp2protein":
                    beanId = "gp2ProteinExtractor";
                    break;

                case "-markers":
                case "-sslps": // formerly
                    beanId = "markerExtractor";
                    break;

                case "-marker_alleles":
                case "-sslp_alleles": // formerly
                    beanId = "markerAlleleExtractor";
                    break;

                case "-orthologs":
                    beanId = "orthologExtractor";
                    break;

                case "-orthologs2":
                    beanId = "orthologExtractor2";
                    break;

                case "-annotations":
                    beanId = "annotExtractor";
                    break;

                case "-db_snps":
                    beanId = "dbSnpExtractor";
                    break;

                case "-gaf_agr_annotations":
                    agr = true;
                    // Note! Do *not* add 'break;' here -- it must fall through
                case "-gaf_annotations":
                    beanId = "annotGafExtractor";
                    break;

                case "-uniprot_annotations":
                    beanId = "annotUniProtExtractor";
                    break;

                case "-daf_annotations":
                    beanId = "annotDafExtractor";
                    break;

                case "-variants":
                    beanId = "variantExtractor";
                    break;

                case "-radoslavov":
                    beanId = "radoslavovExtractor";
                    break;

                case "-chinchilla":
                    beanId = "chinchillaExtractor";
                    break;

                case "-assembly_comparison":
                    beanId = "assemblyComparisonExtractor";
                    break;

                case "-mirna_targets":
                    beanId = "miRnaTargetExtractor";
                    break;

                case "-array_ids":
                    beanId = "arrayIdExtractor";
                    break;

                case "-sequences":
                    beanId = "seqExtractor";
                    break;

                case "-interactions":
                    beanId = "intExtractor";
                    break;

                // other
                case "-obsolete":
                    generateObsoleteIds = true;
                    break;

                default:
                    if( arg.startsWith("-species=") ) {
                        speciesTypeKey = SpeciesType.parse(arg.substring(9));
                    }
                    else if( arg.startsWith("-annotDir=") ) {
                        annotDirOverride = arg.substring(10);
                    }
                    break;
            }
        }

        // generate a file with obsolete rgd ids for genes
        if( generateObsoleteIds ) {
            GeneExtractor ge = (GeneExtractor) bf.getBean("geneExtractor");
            ge.getCmdLineProperties().put("generate_obsolete_ids", "true");
            ge.setExtractDir(manager.getExtractDir());
            ge.setDao(manager.dao);
            ge.generateObsoleteIds();
            return;
        }

        if( beanId==null ) {
            usage();
            return;
        }

        if( speciesTypeKey==SpeciesType.UNKNOWN ) {
            throw new Exception("Unsupported species type");
        }

        manager.run(speciesTypeKey, bf, beanId, agr, annotDirOverride);

        System.out.println("=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    void run(int speciesTypeKey, DefaultListableBeanFactory bf, String beanId, boolean agr, String annotDirOverride) {

        // for every species, create the bean anew to avoid potential conflicts
        List<Integer> speciesTypeKeys;
        if( speciesTypeKey==SpeciesType.ALL ) {
            speciesTypeKeys = new ArrayList<>(SpeciesType.getSpeciesTypeKeys());
        } else {
            speciesTypeKeys = new ArrayList<>();
            speciesTypeKeys.add(speciesTypeKey);
        }
        Collections.shuffle(speciesTypeKeys);

        speciesTypeKeys.parallelStream().forEach( key -> {
            if( key>0 ) {
                try {
                    BaseExtractor extractor = (BaseExtractor) bf.getBean(beanId);
                    extractor.setDao(dao);
                    extractor.setExtractDir(getExtractDir());

                    // set optional parameter overrides
                    if( annotDirOverride!=null && extractor instanceof AnnotBaseExtractor ) {
                        ((AnnotBaseExtractor)extractor).setAnnotDir(annotDirOverride);
                    }

                    // agr
                    if( agr && extractor instanceof AnnotGafExtractor ) {
                        ((AnnotGafExtractor)extractor).setGenerateForAgr(true);
                    }

                    extractor.go(getSpeciesInfo().get(SpeciesType.getCommonName(key).toLowerCase()));
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    static public void usage() {

        String USAGE = "java -Dspring.config=$APPHOME/../properties/default_db.xml -Dlog4j.configuration=$APPHOME/properties/log4j.properties"+
            " -jar ./lib/ftpFileExtracts.jar [MODULE] [OPTIONS]\n"+
        "[MODULE] one of: -annotations | -array_ids | -assembly_comparison | -chinchilla | -daf_annotations | -db_snps"+
            " | -gaf_agr_annotations | -gaf_annotations | -genes | -gp2protein | -interactions | -markers | -marker_alleles"+
            " | -mirna_targets | -obsolete | -orthologs | -radoslavov | -qtls | -sequences | -strains | -uniprot_annotations\n"+
        "[OPTIONS] optional species name: -species=rat|mouse|human|...|all (default) (all:rat,mouse,human,...)\n"+
        "   optional annotation directory override: -annotDir=annotDirOverride;\n"+
        "     overrides value of 'annotDir' from beans 'annotExtractor' and 'annotGafExtractor'";

        System.out.println(USAGE);
        System.exit(-1);
    }

    public FtpFileExtractsManager() {
        System.out.println(dao.getConnectionInfo());
    }
    
    static public void qcFileContent(String fileName, String module, int speciesTypeKey) throws IOException {

        // generate qc
        FileContentQC qcContentChecker = new FileContentQC();
        String result = qcContentChecker.validate(fileName, module, speciesTypeKey);
        if( !result.isEmpty() ) {
            Logger logNullColumns = Logger.getLogger("nullColumns");
            logNullColumns.info(result);
        }
    }

    public String getExtractDir() {
        return extractDir;
    }

    public void setExtractDir(String extractDir) {
        this.extractDir = extractDir;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setSpeciesInfo(Map<String, SpeciesRecord> speciesInfo) {
        this.speciesInfo = speciesInfo;
    }

    public Map<String, SpeciesRecord> getSpeciesInfo() {
        return speciesInfo;
    }
}

