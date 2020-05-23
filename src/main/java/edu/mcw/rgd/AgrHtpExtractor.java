package edu.mcw.rgd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedWriter;
import java.io.IOException;
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

        // data samples
        AgrHtpDataSample dataSamplesInJson = new AgrHtpDataSample();
        for( Dataset ds: datasets.values() ) {

            List<DataSample> samples = loadDataSamples(ds.geoId);
            for( DataSample s: samples ) {
                List<String> uberonSlimTermIds = getUberonSlimTermIds(s.tissueUberonId);
                dataSamplesInJson.addDataObj(s.geoId, s.sampleId, s.sampleTitle, s.sampleAge, s.gender, s.tissueUberonId, uberonSlimTermIds, s.tissue);
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

        String sql = "SELECT sample_accession_id,sample_title,sample_age,sample_gender,sample_tissue,rgd_tissue_term_acc FROM rna_seq"
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
            rec.tissue = rs.getString(5);
            rec.tissueUberonId = rs.getString(6);

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
    }
}
