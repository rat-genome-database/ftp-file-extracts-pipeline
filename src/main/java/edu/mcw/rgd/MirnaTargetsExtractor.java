package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.MiRnaTargetDAO;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.MiRnaTarget;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 7/10/15
 * Time: 3:42 PM
 * <p>Extract data from MIRNA_TARGETS table into a set of files: divided by species and target type (confirmed or predicted)
 */
public class MirnaTargetsExtractor extends BaseExtractor {

    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: mirna-targets-version-1.0.4\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: information about #SPECIES# miRNA target genes extracted from RGD database\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by ';'\n"
    +"#\n"
    +"### June 13, 2015 initial version of the module\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"# (First 7 columns are in common between confirmed and predicted)\n"
    +"#\n"
    +"#1   TARGET_GENE_RGD_ID	      the RGD_ID of the miRNA target gene\n"
    +"#2   TARGET_GENE_SYMBOL         official symbol of miRNA target gene\n"
    +"#3   MIRNA_GENE_RGD_ID	      the RGD_ID of the miRNA gene\n"
    +"#4   MIRNA_GENE_SYMBOL          official symbol of miRNA gene\n"
    +"#5   MATURE_MIRNA               mature miRNA\n"
    +"#6   METHOD_NAME                method name\n"
    +"#7   RESULT_TYPE                result type\n";

     final String HEADER_CONFIRMED_TARGETS =
     "#8   DATA_TYPE                 data type\n"
    +"#9   SUPPORT_TYPE              support type\n"
    +"#10  PMID                      Pubmed Id\n"
    +"#\n"
    +"TARGET_GENE_RGD_ID\tTARGET_GENE_SYMBOL\tMIRNA_GENE_RGD_ID\tMIRNA_GENE_SYMBOL\tMATURE_MIRNA\tMETHOD_NAME\tRESULT_TYPE\t"
    +"DATA_TYPE\tSUPPORT_TYPE\tPMID\n";

    final String HEADER_PREDICTED_TARGETS =
    "#8   TARGET_TRANSCRIPT_ACC     target transcript accession\n"
   +"#9   TRANSCRIPT_BIOTYPE        transcript biotype\n"
   +"#10  ISOFORM                   isoform\n"
   +"#11  AMPLIFICATION             amplification\n"
   +"#12  UTR_START                 UTR Start\n"
   +"#13  UTR_END                   UTR End\n"
   +"#14  TARGET_SITE               target site\n"
   +"#15  SCORE                     score\n"
   +"#16  NORMALIZED_SCORE          normalized score\n"
   +"#17  ENERGY                    energy\n"
   +"#\n"
   +"TARGET_GENE_RGD_ID\tTARGET_GENE_SYMBOL\tMIRNA_GENE_RGD_ID\tMIRNA_GENE_SYMBOL\tMATURE_MIRNA\tMETHOD_NAME\tRESULT_TYPE\t"
   +"TRANSCRIPT_BIOTYPE\tTRANSCRIPT_BIOTYPE\tISOFORM\tAMPLIFICATION\tUTR_START\tUTR_END\tTARGET_SITE\t"
   +"SCORE\tNORMALIZED_SCORE\tENERGY\n";

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        runConfirmed(speciesInfo);
        runPredicted(speciesInfo);
    }

    void runConfirmed(SpeciesRecord si) throws Exception {

        MiRnaTargetDAO dao = new MiRnaTargetDAO();
        List<Gene> mirnaGenes = dao.getMiRnaGenes("confirmed", si.getSpeciesType());
        if( mirnaGenes.isEmpty() ) {
            // no data -- don't generate anything
            return;
        }

        String fileName = "MIRNA_TARGETS_CONFIRMED_"+si.getSpeciesName().toUpperCase()+".txt.gz";
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(getExtractDir()+'/'+fileName))));
        writer.write(HEADER_COMMON_LINES);
        writer.write(HEADER_CONFIRMED_TARGETS);

        for(Gene mirnaGene: mirnaGenes ) {
            for( MiRnaTarget mirna: dao.getTargets(mirnaGene.getRgdId(), "confirmed") ) {

                writeCommonFields(writer, mirnaGene, mirna);

                // data type
                writer.write(mirna.getDataType());
                writer.write('\t');
                // support type
                writer.write(mirna.getSupportType());
                writer.write('\t');
                // pmid
                writer.write(mirna.getPmid());
                writer.write('\n');
            }
        }

        writer.close();

        FtpFileExtractsManager.qcFileContent(getExtractDir()+'/'+fileName, "mirna", si.getSpeciesType());
    }

    void runPredicted(SpeciesRecord si) throws Exception {

        MiRnaTargetDAO dao = new MiRnaTargetDAO();
        List<Gene> mirnaGenes = dao.getMiRnaGenes("predicted", si.getSpeciesType());
        if( mirnaGenes.isEmpty() ) {
            // no data -- don't generate anything
            return;
        }

        String fileName = "MIRNA_TARGETS_PREDICTED_"+si.getSpeciesName().toUpperCase()+".txt.gz";
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(getExtractDir()+'/'+fileName))));
        writer.write(HEADER_COMMON_LINES);
        writer.write(HEADER_PREDICTED_TARGETS);

        for(Gene mirnaGene: mirnaGenes ) {
            for( MiRnaTarget mirna: dao.getTargets(mirnaGene.getRgdId(), "predicted") ) {

                writeCommonFields(writer, mirnaGene, mirna);

                // transcript accession
                writer.write(mirna.getTranscriptAcc());
                writer.write('\t');
                // transcript biotype
                writer.write(mirna.getTranscriptBioType());
                writer.write('\t');
                // isoform
                writer.write(mirna.getIsoform());
                writer.write('\t');
                // amplification
                writer.write(mirna.getAmplification());
                writer.write('\t');
                // utr start
                writer.write(mirna.getUtrStart()==null?"":Integer.toString(mirna.getUtrStart()));
                writer.write('\t');
                // utr end
                writer.write(mirna.getUtrEnd()==null?"":Integer.toString(mirna.getUtrEnd()));
                writer.write('\t');
                // target site
                writer.write(mirna.getTargetSite());
                writer.write('\t');
                // score
                writer.write(mirna.printScore());
                writer.write('\t');
                // normalized score
                writer.write(mirna.printNormalizedScore());
                writer.write('\t');
                // energy
                writer.write(mirna.printEnergy());
                writer.write('\n');
            }
        }

        writer.close();

        FtpFileExtractsManager.qcFileContent(getExtractDir()+'/'+fileName, "mirna", si.getSpeciesType());
    }

    void writeCommonFields(Writer writer, Gene mirnaGene, MiRnaTarget mirna) throws Exception {
        // target gene rgd id
        writer.write(Integer.toString(mirna.getGeneRgdId()));
        writer.write('\t');
        // target gene symbol
        writer.write(getGeneSymbol(mirna.getGeneRgdId()));
        writer.write('\t');
        // mirna gene rgd id
        writer.write(Integer.toString(mirnaGene.getRgdId()));
        writer.write('\t');
        // target gene symbol
        writer.write(mirnaGene.getSymbol());
        writer.write('\t');
        // mature mirna
        writer.write(mirna.getMiRnaSymbol());
        writer.write('\t');
        // method name
        writer.write(mirna.getMethodName());
        writer.write('\t');
        // result type
        writer.write(mirna.getResultType());
        writer.write('\t');
    }

    String getGeneSymbol(int geneRgdId) throws Exception {
        String geneSymbol = geneSymbols.get(geneRgdId);
        if( geneSymbol==null ) {
            geneSymbol = getDao().getSymbolForGene(geneRgdId);
            geneSymbols.put(geneRgdId, geneSymbol);
        }
        return geneSymbol;
    }
    static Map<Integer, String> geneSymbols = new HashMap<>(); // map of gene rgd id to gene symbol
}

