package edu.mcw.rgd;

import edu.mcw.rgd.process.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AgrHtpDataset {
    public AgrHtpDataset.Metadata metaData = new AgrHtpDataset.Metadata();
    public List<DataObj> data = new ArrayList<>();

    SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    class Metadata {
        public final HashMap dataProvider;
        public final String dateProduced;
        public final String release;

        public Metadata() {
            synchronized(DafExport.class) {
                dataProvider = getDataProviderForMetaData();
                release = "RGD Htp Extractor for Data Sets, AGR schema 1.0.1.3, build Aug 25, 2020";

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

    public void addDataObj(String geoId, String title, String summary, String pubmedId, String dateAssigned) throws ParseException {

        // skip SUPERSERIES
        final String SUPERSERIES_SUMMARY = "This SuperSeries is composed of the SubSeries listed below.";
        if( Utils.stringsAreEqualIgnoreCase(summary, SUPERSERIES_SUMMARY) ) {
            return;
        }

        DataObj obj = new DataObj();
        obj.title = title;
        obj.summary = summary;

        obj.categoryTags= new ArrayList<>();
        obj.categoryTags.add("unclassified");

        // date assigned is in format 'Jan 23 2006'
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d yyyy");
        Date dt = formatter.parse(dateAssigned);
        obj.dateAssigned = sdf_agr.format(dt);

        HashMap datasetId = new HashMap();
        datasetId.put("primaryId", "GEO:"+geoId);
        obj.datasetId = datasetId;

        if( pubmedId!=null ) {

            String pmid = "PMID:"+pubmedId;
            Publication p = new Publication();

            if( false ) { // optional 'crossReference'
                p.crossReference = new HashMap<String, Object>();
                p.crossReference.put("id", pmid);
                List<String> pages = new ArrayList<>();
                pages.add("reference");
                p.crossReference.put("pages", pages);
            }
            p.publicationId = pmid;

            List publications = new ArrayList();
            publications.add(p);
            obj.publications = publications;
        }

        this.data.add(obj);
    }

    public void sort() {
        Collections.sort(data, new Comparator<DataObj>() {
            @Override
            public int compare(DataObj o1, DataObj o2) {
                String geoAcc1 = o1.datasetId.get("primaryId").toString();
                String geoAcc2 = o2.datasetId.get("primaryId").toString();
                int r = geoAcc1.length() - geoAcc2.length();
                if( r!=0 ) {
                    return r;
                }
                r = geoAcc1.compareTo(geoAcc2);
                return r;
            }
        });
    }

    class DataObj {
        // required fields
        public HashMap datasetId;
        public String title;
        public String dateAssigned;

        // optional fields
        public String summary;
        public List<String> categoryTags;
        public List<Publication> publications;
    }

    class Publication {
        public Map<String,Object> crossReference;
        public String publicationId;
    }
}
