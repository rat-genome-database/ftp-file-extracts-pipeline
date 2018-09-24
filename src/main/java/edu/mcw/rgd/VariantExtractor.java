package edu.mcw.rgd;

import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.dao.impl.SampleDAO;
import edu.mcw.rgd.dao.impl.StrainDAO;
import edu.mcw.rgd.dao.impl.VariantDAO;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 10/14/14
 * Time: 3:21 PM
 * <p>
 * extract all non-synonymous variants for all rat strains into files
 */
public class VariantExtractor extends BaseExtractor {
    private String version;
    private String outputDir;
    private String outputSubDir;

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        System.out.println(getVersion());

        if( speciesInfo.getSpeciesType()!=SpeciesType.RAT ) {
            System.out.println("  only rat extract supported!");
            return;
        }

        long timeStart = System.currentTimeMillis();

        SampleDAO sampleDAO = new SampleDAO();
        sampleDAO.setDataSource(DataSourceFactory.getInstance().getCarpeNovoDataSource());
        String[] patientIds = {"180","500","600"};
        for( Sample sample: sampleDAO.getSamples(patientIds) ) {
            run(sample);
        }

        long timeEnd = System.currentTimeMillis();
        System.out.println(" variant generation complete: elapsed "+Utils.formatElapsedTime(timeStart,  timeEnd));
    }

    void run(Sample sample) throws Exception {

        // create output file
        BufferedWriter writer = new BufferedWriter(new FileWriter(createOutputFile(sample)));

        // write header
        String header = ""
        +"# RGD-PIPELINE: ftp-file-extracts\n"
        +"# MODULE: variants-ver-1.0.1\n"
        +"# GENERATED-ON: "+SpeciesRecord.getTodayDate()+"\n"
        +"# PURPOSE: non-synonomous variants for rat strains, extracted from RGD database\n"
        +"# CONTACT: rgd.data@mcw.edu\n"
        +"# FORMAT: tab delimited text\n"
        +"#\n"
        +"# SAMPLE: "+sample.getAnalysisName()+"\n"
        +"# SAMPLE-INFO: "+sample.getDescription()+"\n"
        +"# SEQUENCED-BY: "+sample.getSequencedBy()+"\n"
        +"# SEQUENCER: "+sample.getSequencer()+"\n"
        +"# SECONDARY-ANALYSIS: "+sample.getSecondaryAnalysisSoftware()+"\n"
        +"# GRANT-NUMBER: "+sample.getGrantNumber()+"\n"
        +"# WHERE-BRED: "+sample.getWhereBred()+"\n"
        +"# STRAIN-RGD-ID: "+sample.getStrainRgdId()+"\n"
        +"#\n"
        +"# COLUMN INFORMATION:\n"
        +"#\n"
        +"#1  VID	      unique variant id\n"
        +"#2  VAR_TYPE    variant type:'snv','ins','del'\n"
        +"#3  CHROMOSOME  chromosome\n"
        +"#4  POSITION    position on chromosome\n"
        +"#5  REF_NUC     reference nucleotide\n"
        +"#6  VAR_NUC     variant nucleotide\n"
        +"#7  REF_AA      reference amino acid\n"
        +"#8  VAR_AA      variant amino acid\n"
        +"#9  PREDICTION  polyphen prediction\n"
        +"#10 TRANSCRIPT  transcript id\n"
        +"#11 POS_NUC     nucleotide position of variant relative to 1st nucleotide of CDS\n"
        +"#12 POS_AA      aminoacid position of variant relative to protein 1st aminoacid\n"
        +"";
        writer.write(header);

        VariantSearchBean vsb = new VariantSearchBean(sample.getMapKey());
        vsb.sampleIds = new ArrayList<Integer>();
        vsb.sampleIds.add(sample.getId());
        vsb.setAAChange(null, "true");
        VariantDAO variantDAO = new VariantDAO();
        variantDAO.setDataSource(DataSourceFactory.getInstance().getCarpeNovoDataSource());
        String[] chromosomes = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "X", "Y",};
        for( String chr: chromosomes ) {
            vsb.setChromosome(chr);
            List<VariantResult> results = variantDAO.getVariantResults(vsb);
            for( VariantResult vr: results ) {
                if( vr.getTranscriptResults()==null || vr.getTranscriptResults().isEmpty() ) {
                    System.out.println("no transcript results");
                    continue;
                }
                for( TranscriptResult tr: vr.getTranscriptResults() ) {
                    Variant v = vr.getVariant();
                    writer.write(v.getId()+"\t");
                    writer.write(Utils.NVL(v.getVariantType(),"snv")+"\t");
                    writer.write(v.getChromosome()+"\t");
                    writer.write(v.getStartPos()+"\t");
                    writer.write(v.getReferenceNucleotide()+"\t");
                    writer.write(v.getVariantNucleotide()+"\t");
                    writer.write(tr.getAminoAcidVariant().getReferenceAminoAcid()+"\t");
                    writer.write(tr.getAminoAcidVariant().getVariantAminoAcid()+"\t");
                    writer.write(Utils.defaultString(tr.getAminoAcidVariant().getPolyPhenStatus())+"\t");
                    writer.write(tr.getAminoAcidVariant().getTranscriptSymbol()+"\t");
                    writer.write(tr.getAminoAcidVariant().getDnaPosition()+"\t");
                    writer.write(tr.getAminoAcidVariant().getAaPosition()+"\n");
                }
            }
        }
        writer.close();
    }

    File createOutputFile(Sample sample) throws Exception {

        // directory is based on sample's strain name
        Strain strain = new StrainDAO().getStrain(sample.getStrainRgdId());
        String dirName = getExtractDir()+"/"+getOutputSubDir()+"/"+strain.getSymbol().replace('/','_');
        new File(dirName).mkdirs();

        // file name is based on sample name + assembly symbol
        String fileName = sample.getAnalysisName().replace('/','_').replace(' ','_')
                .replace('(','_').replace(')','_').replace("__","_");
        if( sample.getMapKey()==60 )
            fileName += "RGSC3.4";
        else if( sample.getMapKey()==70 )
            fileName += "Rnor5.0";
        else if( sample.getMapKey()==360 )
            fileName += "Rnor6.0";
        String filePath = dirName+"/"+fileName+".txt";
        return new File(filePath);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputSubDir(String outputSubDir) {
        this.outputSubDir = outputSubDir;
    }

    public String getOutputSubDir() {
        return outputSubDir;
    }
}
