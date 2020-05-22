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
                release = "RGD Htp Extractor for Data Samples, AGR schema 1.0.1.1, build  May 22, 2020";

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

    public void addDataObj(String expId, String sampleId, String sampleTitle, String sampleAge, String sex) {

        DataSampleObj obj = new DataSampleObj();

        HashMap datasetId = new HashMap();
        datasetId.put("primaryId", "GEO:"+expId);
        obj.datasetId = datasetId;

        HashMap sampleIdMap = new HashMap();
        sampleIdMap.put("primaryId", "GEO:"+sampleId);
        obj.sampleId = sampleIdMap;

        obj.sampleTitle = sampleTitle;

        obj.sex = normalizeSex(sex);

        if( sampleAge!=null ) {
            HashMap ageMap = new HashMap();
            ageMap.put("age", sampleAge);
            obj.sampleAge = ageMap;
        }
        this.data.add(obj);
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
                result = "female";
                break;
            case "pooled":
            case "both":
            case "male or female":
            case "male and female":
                result = "pooled";
                break;
            case "none":
            case "not known":
            case "unsexed":
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
                String dataset1 = o1.datasetId.get("primaryId").toString();
                String dataset2 = o2.datasetId.get("primaryId").toString();
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
        public HashMap datasetId;
        public HashMap sampleId;

        // optional fields
        public String sampleTitle;
        public String taxonId = "NCBITaxon:10116";
        public HashMap sampleAge;
        public String sex;
    }

}
