package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Reference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AgrRefExchange {
    public AgrRefExchange.Metadata metaData = new AgrRefExchange.Metadata();
    public List<DataObj> data = new ArrayList<>();

    SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    class Metadata {
        public final HashMap dataProvider;
        public final String dateProduced;
        public final String release;

        public Metadata() {
            synchronized(DafExport.class) {
                dataProvider = getDataProviderForMetaData();
                release = "RGD Reference Exchange Extractor, AGR schema 1.0.1.4, build Jan 22, 2021";

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

    public void addDataObj(String pubMedId, Reference r, Date lastModDate, String allianceCategory) throws ParseException {

        DataObj obj = new DataObj();
        obj.pubMedId = pubMedId;
        obj.dateLastModified = sdf_agr.format(lastModDate);
        obj.allianceCategory = allianceCategory;
        obj.modId = "RGD:"+r.getRgdId();

        HashMap modRefType = new HashMap();
        modRefType.put("referenceType", r.getReferenceType());
        modRefType.put("source", "RGD");
        obj.MODReferenceTypes.add(modRefType);

        this.data.add(obj);
    }

    public void sort() {
        Collections.sort(data, new Comparator<AgrRefExchange.DataObj>() {
            @Override
            public int compare(AgrRefExchange.DataObj o1, AgrRefExchange.DataObj o2) {
                return o1.pubMedId.compareTo(o2.pubMedId);
            }
        });
    }

    class DataObj {
        // required fields
        public String pubMedId;
        public String allianceCategory;

        // optional
        public String modId;
        public String dateLastModified;
        public List MODReferenceTypes = new ArrayList();
        //public List tags = new ArrayList();
    }

}
