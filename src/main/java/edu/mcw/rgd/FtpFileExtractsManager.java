package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author mtutaj
 * @since Oct 11, 2010
 */
public class FtpFileExtractsManager {

    public static final String MULTIVAL_SEPARATOR = ";"; // multi-value field separator

    static Logger log = LogManager.getLogger("status");

    String extractDir; // directory where the files will be generated
    FtpFileExtractsDAO dao = new FtpFileExtractsDAO();
    private String version;
    private Map<String, SpeciesRecord> speciesInfo;

    static public void main(String[] args) throws Exception {
        if( args.length==0 ) {
            usage();
        }

        try {
            main2(args);
        } catch(Exception e) {
            Utils.printStackTrace(e, log);
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
        boolean singleThread = false;

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

                case "-cell_lines":
                    beanId = "cellLineExtractor";
                    break;

                case "-agr_htp":
                    beanId = "agrHtpExtractor";
                    break;

                case "-agr_ref":
                    beanId = "agrRefExtractor";
                    break;

                // other
                case "-obsolete":
                    generateObsoleteIds = true;
                    break;

                case "-single_thread":
                    singleThread = true;
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

        // generates three files with obsolete rgd ids for genes, strains and alleles
        if( generateObsoleteIds ) {
            ObsoleteIdExtractor oe = (ObsoleteIdExtractor) bf.getBean("obsoleteIdExtractor");
            oe.setExtractDir(manager.getExtractDir());
            oe.run();
            return;
        }

        if( beanId==null ) {
            usage();
            return;
        }

        if( speciesTypeKey==SpeciesType.UNKNOWN ) {
            throw new Exception("Unsupported species type");
        }

        manager.run(speciesTypeKey, bf, beanId, agr, annotDirOverride, singleThread);
        System.out.println("=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    void run(int speciesTypeKey, DefaultListableBeanFactory bf, String beanId, boolean agr, String annotDirOverride, boolean singleThread) {

        // for every species, create the bean anew to avoid potential conflicts
        List<Integer> speciesTypeKeys;
        if( speciesTypeKey==SpeciesType.ALL ) {
            speciesTypeKeys = new ArrayList<>(SpeciesType.getSpeciesTypeKeys());
        } else {
            speciesTypeKeys = new ArrayList<>();
            speciesTypeKeys.add(speciesTypeKey);
        }
        Collections.shuffle(speciesTypeKeys);

        Stream<Integer> stream;
        if( singleThread ) {
            stream = speciesTypeKeys.stream();
        } else {
            stream = speciesTypeKeys.parallelStream();
        }
        stream.forEach( key -> {
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

        String USAGE = "java -Dspring.config=$APPHOME/../properties/default_db.xml -Dlog4j.configurationFile=$APPHOME/properties/log4j2.xml"+
            " -jar ./lib/ftpFileExtracts.jar [MODULE] [OPTIONS]\n"+
        "[MODULE] one of: -annotations | -array_ids | -assembly_comparison | -chinchilla | -daf_annotations | -db_snps"+
            " | -gaf_agr_annotations | -gaf_annotations | -genes | -gp2protein | -interactions | -markers | -marker_alleles"+
            " | -mirna_targets | -obsolete | -orthologs | -radoslavov | -qtls | -sequences | -strains | -uniprot_annotations\n"+
        "[OPTIONS] (purely optional, not mandatory):\n"+
        "-species=rat|mouse|human|...|all (default) (all:rat,mouse,human,...)\n"+
        "-single_thread    if specified, the module is run in a single thread; if not specified, (default), module is run in multiple threads\n"+
        "-annotDir=annotDirOverride    if provided, specifies the annotation directory override\n"+
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
            Logger logNullColumns = LogManager.getLogger("nullColumns");
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

