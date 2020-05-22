package edu.mcw.rgd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.dao.AbstractDAO;
import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgrHtpExtractor extends BaseExtractor {

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

        // data samples
        AgrHtpDataSample dataSamplesInJson = new AgrHtpDataSample();
        for( Dataset ds: datasets.values() ) {

            List<DataSample> samples = loadDataSamples(ds.geoId);
            for( DataSample s: samples ) {
                dataSamplesInJson.addDataObj(s.geoId, s.sampleId, s.sampleTitle, s.sampleAge, s.gender);
            }
        }
        dumpDataSamplesToJson(dataSamplesInJson);


        dumpDataSetsToJson(datasets);
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
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

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
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

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

    List<DataSample> loadDataSamples(String geoId) throws Exception {

        List<DataSample> dataSamples = new ArrayList<>();

        String sql = "SELECT sample_accession_id,sample_title,sample_age,sample_gender FROM rna_seq"
                +" WHERE geo_accession_id=?";

        Connection conn = DataSourceFactory.getInstance().getDataSource().getConnection();
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

            dataSamples.add(rec);
        }
        conn.close();

        return dataSamples;
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
    }
}
