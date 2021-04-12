package edu.mcw.rgd;

import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.dao.impl.SampleDAO;
import edu.mcw.rgd.dao.impl.StrainDAO;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mtutaj
 * @since 10/14/14
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

        AtomicInteger dataRowsWritten = new AtomicInteger(0);
        AtomicInteger samplesWithData = new AtomicInteger(0);
        AtomicInteger samplesWithNoData = new AtomicInteger(0);
        sampleDAO.getSamples(patientIds).parallelStream().forEach( sample -> {

            try {
                int dataRows = run(sample);
                if (dataRows > 0) {
                    dataRowsWritten.addAndGet(dataRows);
                    samplesWithData.incrementAndGet();
                } else {
                    samplesWithNoData.incrementAndGet();
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

        long timeEnd = System.currentTimeMillis();
        System.out.println(" variant generation complete: elapsed "+Utils.formatElapsedTime(timeStart,  timeEnd));
        System.out.println(" samples with data: "+samplesWithData);
        System.out.println(" samples without data "+samplesWithNoData);
        System.out.println(" total data rows written "+Utils.formatThousands(dataRowsWritten));
    }

    int run(Sample sample) throws Exception {

        // write header
        String header = ""
        +"# RGD-PIPELINE: ftp-file-extracts\n"
        +"# MODULE: variants-ver-2021.04.12\n"
        +"# GENERATED-ON: "+SpeciesRecord.getTodayDate()+"\n"
        +"# PURPOSE: non-synonomous variants for rat strains, extracted from RGD database\n"
        +"# CONTACT: rgd.data@mcw.edu\n"
        +"# FORMAT: tab delimited text\n"
        +"#\n"
        +"# SAMPLE: "+Utils.defaultString(sample.getAnalysisName())+"\n"
        +"# SAMPLE-INFO: "+Utils.defaultString(sample.getDescription())+"\n"
        +"# SEQUENCED-BY: "+Utils.defaultString(sample.getSequencedBy())+"\n"
        +"# SEQUENCER: "+Utils.defaultString(sample.getSequencer())+"\n"
        +"# SECONDARY-ANALYSIS: "+Utils.defaultString(sample.getSecondaryAnalysisSoftware())+"\n"
        +"# GRANT-NUMBER: "+Utils.defaultString(sample.getGrantNumber())+"\n"
        +"# WHERE-BRED: "+Utils.defaultString(sample.getWhereBred())+"\n"
        +"# STRAIN-RGD-ID: "+sample.getStrainRgdId()+"\n"
        +"#\n"
        +"# COLUMN INFORMATION:\n"
        +"#\n"
        +"#1  VID	      unique variant rgd id\n"
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

        int dataRows = 0;
        StringBuffer buf = new StringBuffer(header);

        ResultSet rs = this.getNonSynonymousVariants(sample.getId(), sample.getMapKey());
        while( rs.next() ) {
            dataRows++;

            String rgdId = rs.getString("rgd_id");
            String varType = rs.getString("variant_type");
            String chr = rs.getString("chromosome");
            String startPos = rs.getString("start_pos");
            String refNuc = rs.getString("ref_nuc");
            String varNuc = rs.getString("var_nuc");
            String refAa = rs.getString("ref_aa");
            String varAa = rs.getString("var_aa");
            String prediction = Utils.defaultString(rs.getString("prediction"));
            String dnaPos = rs.getString("dna_pos");
            String aaPos = rs.getString("aa_pos");
            String accId = rs.getString("acc_id");

            buf.append(rgdId).append("\t")
                .append(varType).append("\t")
                .append(chr).append("\t")
                .append(startPos).append("\t")
                .append(refNuc).append("\t")
                .append(varNuc).append("\t")
                .append(refAa).append("\t")
                .append(varAa).append("\t")
                .append(prediction).append("\t")
                .append(accId).append("\t")
                .append(dnaPos).append("\t")
                .append(aaPos).append("\n");
        }
        rs.getStatement().getConnection().close();

        // create output file if there are any rows
        String dataFileName = createOutputFile(sample);
        if( dataRows>0 ) {
            BufferedWriter out = Utils.openWriter(dataFileName);
            out.write(buf.toString());
            out.close();
        } else {
            new File(dataFileName).deleteOnExit();
        }

        //System.out.println("  sample "+sample.getId()+": "+sample.getAnalysisName()+";   rows="+dataRows);
        return dataRows;
    }

    String createOutputFile(Sample sample) throws Exception {

        // directory is based on sample's strain name
        Strain strain = new StrainDAO().getStrain(sample.getStrainRgdId());
        String fileNameS = ;
        fileNameS = strain.getSymbol()
                .replace("<i>","").replace("</i>","")
                .replace("<I>","").replace("</I>","")
                .replace("<sup>","").replace("</sup>","");

        String separator = System.getProperty("file.separator");
        if( separator.equals("/") ) {
            // unix
            fileNameS = fileNameS.replace('/', '_');
        } else {
            // Windows file system has many more restrictions on punctuation characters used in file names
            fileNameS = fileNameS
                    .replace("<","").replace(">","")
                    .replace(":","").replace("\\","")
                    .replace('/', '_');
        }

        String dirName = getExtractDir()+"/"+getOutputSubDir()+"/"+fileNameS;
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
        String filePath = dirName+"/"+fileName+".txt.gz";
        return filePath;
    }

    ResultSet getNonSynonymousVariants(int sampleId, int mapKey) throws Exception {

        String sql = "SELECT v.rgd_id,v.variant_type,vmd.chromosome,vmd.start_pos,v.ref_nuc,v.var_nuc, vt.ref_aa,vt.var_aa, p.prediction, vt.full_ref_nuc_pos dna_pos,vt.full_ref_aa_pos aa_pos, t.acc_id \n" +
                "FROM variant v  \n" +
                "join variant_map_data vmd on (vmd.rgd_id=v.rgd_id)  \n" +
                "join variant_sample_detail vsd on (vsd.rgd_id=v.rgd_id)\n" +
                "join variant_transcript vt on v.rgd_id=vt.variant_rgd_id \n" +
                "join transcripts t on (vt.transcript_rgd_id=t.transcript_rgd_id)\n" +
                " left outer join polyphen  p on (v.rgd_id =p.variant_rgd_id) \n" +
                "WHERE vsd.sample_id=? AND vmd.map_key=? AND ref_aa is not null AND var_aa is not null AND ref_aa<>var_aa\n" +
                " ORDER BY chromosome,start_pos";
        Connection conn = DataSourceFactory.getInstance().getCarpeNovoDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, sampleId);
        ps.setInt(2, mapKey);
        return ps.executeQuery();
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
