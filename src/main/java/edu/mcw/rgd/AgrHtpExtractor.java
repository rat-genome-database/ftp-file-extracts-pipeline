package edu.mcw.rgd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.dao.impl.OntologyXDAO;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.util.*;

public class AgrHtpExtractor extends BaseExtractor {

    static String[] uberonSlimTermIdArray = new String[]{"UBERON:0001009","UBERON:0005409","UBERON:0000949","UBERON:0001008","UBERON:0002330","UBERON:0002193","UBERON:0002416",
            "UBERON:0002423","UBERON:0002204","UBERON:0001016","UBERON:0000990","UBERON:0001004","UBERON:0001032","UBERON:0005726","UBERON:0007037","UBERON:0002105","UBERON:0002104",
            "UBERON:0000924","UBERON:0000925","UBERON:0000926","UBERON:0003104","UBERON:0001013","UBERON:0000026","UBERON:0016887","UBERON:6005023","UBERON:0002539","Other"};

    Set<String> uberonSlimTermIdSet;

    public AgrHtpExtractor() {

        uberonSlimTermIdSet = new HashSet<>();
        Collections.addAll(uberonSlimTermIdSet, uberonSlimTermIdArray);
    }

    static boolean versionPrintedOut = false;

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        if( versionPrintedOut ) {
            return;
        }
        versionPrintedOut = true;
        System.out.println(getVersion());

        // datasets
        Map<String, Dataset> datasets = loadDataSets();

        // mappings of 'sample_age' to stages
        Map<String, HashMap> ageStages = loadStages();

        Set<String> platformsWithoutMMO = new TreeSet<>();

        // data samples
        AgrHtpDataSample dataSamplesInJson = new AgrHtpDataSample();
        for( Dataset ds: datasets.values() ) {

            List<DataSample> samples = loadDataSamples(ds.geoId, platformsWithoutMMO);
            for( DataSample s: samples ) {
                List<String> uberonSlimTermIds = getUberonSlimTermIds(s.tissueUberonId);
                dataSamplesInJson.addDataObj(s.geoId, s.sampleId, s.sampleTitle, s.sampleAge, s.gender, s.tissueUberonId, uberonSlimTermIds, s.tissue, s.assayType, ageStages);
            }
        }
        dumpDataSamplesToJson(dataSamplesInJson);

        dumpDataSetsToJson(datasets);

        // dump platforms without MMO
        if( platformsWithoutMMO.size()>0 ) {
            System.out.println("platforms without MMO:");
            System.out.println("===");
            for (String platform: platformsWithoutMMO ) {
                System.out.println(platform);
            }
            System.out.println("===");
        }
    }

    void dumpDataSamplesToJson(AgrHtpDataSample dataSamplesInJson) throws ParseException {

        // setup a JSON object array to collect all DafAnnotation objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // sort data
        dataSamplesInJson.sort();

        // dump DafAnnotation records to a file in JSON format
        try {
            String jsonFileName = "data/agr/HTPDATASAMPLES_RGD.json";
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(jsonFileName), "UTF8");
            BufferedWriter jsonWriter = new BufferedWriter(out);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(dataSamplesInJson));

            jsonWriter.close();
        } catch(IOException ignore) {
        }
    }

    void dumpDataSetsToJson(Map<String, Dataset> datasets) throws ParseException {

        AgrHtpDataset jsonDatasets = normalizeDatasets(datasets);

        // setup a JSON object array to collect all DafAnnotation objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // sort data
        jsonDatasets.sort();

        // dump DafAnnotation records to a file in JSON format
        try {
            String jsonFileName = "data/agr/HTPDATASET_RGD.json";
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(jsonFileName), "UTF8");
            BufferedWriter jsonWriter = new BufferedWriter(out);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(jsonDatasets));

            jsonWriter.close();
        } catch(IOException ignore) {
        }
    }

    AgrHtpDataset normalizeDatasets(Map<String,Dataset> datasets) throws ParseException {

        AgrHtpDataset result = new AgrHtpDataset();

        for( Dataset ds: datasets.values() ) {
            result.addDataObj(ds.geoId, ds.studyTitle, ds.summary, ds.pubmedId, ds.submissionDate);
        }

        return result;
    }

    Map<String, Dataset> loadDataSets() throws Exception {

        Map<String,Dataset> datasets = new HashMap<>();

        String sql = "SELECT DISTINCT geo_accession_id, study_title, submission_date, pubmed_id, DBMS_LOB.SUBSTR(summary, 4000) summary FROM rna_seq"
                +" WHERE sample_organism='Rattus norvegicus'";

        Connection conn = DataSourceFactory.getInstance().getDataSource().getConnection();
        Statement ps = conn.createStatement();
        ResultSet rs = ps.executeQuery(sql);
        while( rs.next() ) {
            Dataset rec = new Dataset();
            rec.geoId = rs.getString(1);
            rec.studyTitle = rs.getString(2);
            rec.submissionDate = rs.getString(3);
            rec.pubmedId = rs.getString(4);
            rec.summary = rs.getString(5);

            datasets.put(rec.geoId, rec);
        }
        conn.close();

        return datasets;
    }

    Map<String, HashMap> loadStages() throws Exception {

        Map<String, HashMap> stages = new HashMap<>();

        String sql = "SELECT sample_age,stage,stage_slim FROM RNA_SEQ_STAGES";

        // 'stage' structure corresponding to 'sample_age'
        //
        //"stage" : {
        //    "stageName": "embryonic stage 16",
        //    "stageUberonSlimTerm": {
        //        "uberonTerm": "UBERON:0000068"
        //    }
        //}

        Connection conn = DataSourceFactory.getInstance().getDataSource().getConnection();
        Statement ps = conn.createStatement();
        ResultSet rs = ps.executeQuery(sql);
        while( rs.next() ) {

            String sampleAge = rs.getString("sample_age");
            String stageName = rs.getString("stage");
            String stageSlim = rs.getString("stage_slim");

            HashMap stage = new HashMap();
            stage.put("stageName", stageName);
            HashMap stageUberonSlimTerm = new HashMap();
            stageUberonSlimTerm.put("uberonTerm", stageSlim);
            stage.put("stageUberonSlimTerm", stageUberonSlimTerm);

            stages.put(sampleAge, stage);
        }
        conn.close();

        return stages;
    }

    List<DataSample> loadDataSamples(String geoId, Set<String> platformsWithoutMMO) throws Exception {

        List<DataSample> dataSamples = new ArrayList<>();

        String sql = "SELECT sample_accession_id,sample_title,sample_age,sample_gender,sample_tissue,rgd_tissue_term_acc,platform_name FROM rna_seq"
                +" WHERE geo_accession_id=? AND sample_organism='Rattus norvegicus'";

        Connection conn = DataSourceFactory.getInstance().getDataSource().getConnection();

        String sql2 = "SELECT assay_type_acc FROM rna_seq_assays WHERE platform_name=?";
        PreparedStatement ps2 = conn.prepareStatement(sql2);

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, geoId);
        ResultSet rs = ps.executeQuery();
        while( rs.next() ) {
            DataSample rec = new DataSample();
            rec.geoId = geoId;
            rec.sampleId = rs.getString(1);
            rec.sampleTitle = rs.getString(2);
            rec.sampleAge = rs.getString(3);
            rec.gender = rs.getString(4);
            rec.tissue = rs.getString(5);
            rec.tissueUberonId = rs.getString(6);

            String platformName = rs.getString(7);
            ps2.setString(1, platformName);
            ResultSet rs2 = ps2.executeQuery();
            String assayTypeAcc = "MMO:0000000";
            if( rs2.next() ) {
                assayTypeAcc = rs2.getString(1);
            } else {
                platformsWithoutMMO.add(platformName);
            }
            rs2.close();
            rec.assayType = assayTypeAcc;

            dataSamples.add(rec);
        }
        conn.close();

        return dataSamples;
    }

    Map<String, List<String>> cacheOfUberonSlimTermIds = new HashMap<>();

    List<String> getUberonSlimTermIds(String uberonTermId) throws Exception {

        if( uberonTermId==null ) {
            List<String> result = new ArrayList<>();
            result.add("Other");
            return result;
        }

        if( uberonSlimTermIdSet.contains(uberonTermId) ) {
            List<String> result = new ArrayList<>();
            result.add(uberonTermId);
            return result;
        }

        List<String> uberonSlimTermIds = cacheOfUberonSlimTermIds.get(uberonTermId);
        if( uberonSlimTermIds == null ) {
            OntologyXDAO odao = new OntologyXDAO();
            uberonSlimTermIds = new ArrayList<>();

            List<String> parentTermIds = odao.getAllActiveTermAncestorAccIds(uberonTermId);
            for (String termId : parentTermIds) {
                if (uberonSlimTermIdSet.contains(termId)) {
                    uberonSlimTermIds.add(termId);
                }
            }
            cacheOfUberonSlimTermIds.put(uberonTermId, uberonSlimTermIds);
        }

        return uberonSlimTermIds;
    }

    class Dataset {
        public String geoId;
        public String studyTitle;
        public String submissionDate;
        public String summary;
        public String pubmedId;
    }

    class DataSample {
        public String geoId;
        public String sampleId;
        public String sampleTitle;
        public String sampleAge;
        public String gender;
        public String tissue;
        public String tissueUberonId;
        public String assayType;
    }
}
