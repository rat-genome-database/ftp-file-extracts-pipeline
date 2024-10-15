package edu.mcw.rgd;

import java.text.SimpleDateFormat;
import java.util.*;

public class AgrHtpDataSample {

    public AgrHtpDataSample.Metadata metaData = new AgrHtpDataSample.Metadata();
    public List<DataSampleObj> data = new ArrayList<>();

    SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    class Metadata {
        public final HashMap dataProvider;
        public final String dateProduced;
        public final String release;

        public Metadata() {
            synchronized(DafExport.class) {
                dataProvider = getDataProviderForMetaData();
                release = "RGD Htp Extractor for Data Samples, build  Oct 15, 2024";

                SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                dateProduced = sdf_agr.format(new Date());
            }
        }
    }

    HashMap getDataProviderForMetaData() {

        HashMap crossReference = new HashMap();
        crossReference.put("id", "RGD");
        List<String> pages = new ArrayList<>();
        pages.add("homepage");
        crossReference.put("pages", pages);

        HashMap dataProvider = new HashMap();
        dataProvider.put("type", "curated");
        dataProvider.put("crossReference", crossReference);

        return dataProvider;
    }

    public void addDataObj(String expId, String sampleId, String sampleTitle, String sampleAge, String sex,
                           String tissueUberonId, List<String> tissueUberonSlimIds, String tissue, String assayType,
                           Map<String, HashMap> ageStages) {

        DataSampleObj obj = new DataSampleObj();

        List<String> datasetId = new ArrayList<>();
        datasetId.add("GEO:"+expId);
        obj.datasetIds = datasetId;

        HashMap sampleIdMap = new HashMap();
        sampleIdMap.put("primaryId", "GEO:"+sampleId);
        obj.sampleId = sampleIdMap;

        obj.sampleTitle = sampleTitle;

        obj.sex = normalizeSex(sex);

        if( sampleAge!=null ) {
            HashMap stage = ageStages.get(sampleAge);
            // export 'sampleAge' only if it has valid mapping to a stage -- per communication with Jennifer in Feb 2021
            if( stage!=null ) {
                HashMap ageMap = new HashMap();
                ageMap.put("age", sampleAge);
                ageMap.put("stage", stage);
                obj.sampleAge = ageMap;
            }
        }

        obj.sampleLocations = getSampleLocation(tissueUberonId, tissueUberonSlimIds, tissue);

        obj.assayType = assayType;

        this.data.add(obj);
    }

    List getSampleLocation(String tissueUberonId, List<String> tissueUberonSlimIds, String tissue) {

        List result = new ArrayList();

        HashMap loc = new HashMap();

        loc.put("anatomicalStructureTermId", tissueUberonId==null ? "UBERON:0001062" : tissueUberonId);
        loc.put("whereExpressedStatement", tissue);

        List slims = new ArrayList();
        for( String slimId: tissueUberonSlimIds ) {
            HashMap id = new HashMap();
            id.put("uberonTerm", slimId);
            slims.add(id);
        }
        loc.put("anatomicalStructureUberonSlimTermIds", slims);

        result.add(loc);
        return result;
    }

    String normalizeSex(String sex) {
        if( sex==null ) {
            return null;
        }

        String result = null;
        String str = sex.trim().toLowerCase();
        switch(str) {
            case "m":
            case "male":
            case "males":
            case "weanling male":
            case "sex:male":
                result = "male";
                break;
            case "f":
            case "female":
            case "females":
            case "famale": // typo in the data
                result = "female";
                break;
            case "pooled":
            case "both":
            case "both genders":
            case "male/female":
            case "male or female":
            case "male and female":
                result = "pooled";
                break;
            case "na":
            case "none":
            case "not determined":
            case "not known":
            case "not provided":
            case "unsexed":
            case "unknown":
                result = "unknown";
                break;
            default:
                if( str.startsWith("male,") ) {
                    result = "male";
                }
                else if( str.startsWith("female") ) {
                    result = "female";
                }
                else if( str.startsWith("pooled") ) {
                    result = "pooled";
                }
        }

        if( result==null ) {
            System.out.println("cannot map sex: "+sex);
        }
        return result;
    }

    public void sort() {

        Collections.sort(data, new Comparator<DataSampleObj>() {
            @Override
            public int compare(DataSampleObj o1, DataSampleObj o2) {
                String dataset1 = o1.datasetIds.get(0);
                String dataset2 = o2.datasetIds.get(0);
                int r = dataset1.length() - dataset2.length();
                if( r!=0 ) {
                    return r;
                }
                r = dataset1.compareTo(dataset2);
                if( r!=0 ) {
                    return r;
                }

                String sampleId1 = o1.sampleId.get("primaryId").toString();
                String sampleId2 = o2.sampleId.get("primaryId").toString();
                r = sampleId1.length() - sampleId2.length();
                if( r!=0 ) {
                    return r;
                }
                r = sampleId1.compareTo(sampleId2);
                return r;
            }
        });
    }

    class DataSampleObj {
        // required fields
        public List<String> datasetIds;
        public HashMap sampleId;
        public String assayType;
        public String sampleType = "OBI:0000423"; // extract

        // optional fields
        public String sampleTitle;
        public String taxonId = "NCBITaxon:10116";
        public HashMap sampleAge; // age and stage
        public String sex;
        public List sampleLocations;
    }

}
