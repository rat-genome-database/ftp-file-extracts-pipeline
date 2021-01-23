package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Author;
import edu.mcw.rgd.datamodel.Reference;
import edu.mcw.rgd.process.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AgrRefs {
    public AgrRefs.Metadata metaData = new AgrRefs.Metadata();
    public List<DataObj> data = new ArrayList<>();

    SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    SimpleDateFormat sdf2_agr = new SimpleDateFormat("yyyy MMM dd");

    class Metadata {
        public final HashMap dataProvider;
        public final String dateProduced;
        public final String release;

        public Metadata() {
            synchronized(DafExport.class) {
                dataProvider = getDataProviderForMetaData();
                release = "RGD Reference Extractor, AGR schema 1.0.1.4, build Jan 22, 2021";

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

    public void addDataObj(String primaryId, Reference r, Date lastModDate, String allianceCategory, List<Author> authors) throws ParseException {

        DataObj obj = new DataObj();
        obj.primaryId = primaryId;
        obj.title = Utils.NVL(r.getTitle(), "");
        if( r.getPubDate()!=null ) {
            obj.datePublished = sdf2_agr.format(r.getPubDate());
        } else {
            obj.datePublished = "";
        }
        obj.dateLastModified = sdf_agr.format(lastModDate);
        obj.volume = r.getVolume();
        obj.pages = r.getPages();
        obj.ZABSTRACTZ = r.getRefAbstract();
        obj.citation = r.getCitation();
        obj.allianceCategory = allianceCategory;
        obj.publisher = r.getPublisher();
        obj.issueName = r.getIssue();

        HashMap modRefType = new HashMap();
        modRefType.put("referenceType", r.getReferenceType());
        modRefType.put("source", "RGD");
        obj.MODReferenceTypes.add(modRefType);

        int i=1;
        for( Author a: authors ) {
            HashMap author = new HashMap();
            author.put("name", a.getAuthorForCitation());
            author.put("referenceId", "RGD:A"+a.getKey());

            author.put("firstName", a.getFirstName());
            author.put("lastName", a.getLastName());
            author.put("authorRank", new Integer(i));

            obj.authors.add(author);
            i++;
        }

        HashMap crossReference = new HashMap();
        crossReference.put("id", "RGD:"+r.getRgdId());
        List pages = new ArrayList();
        pages.add("reference");
        crossReference.put("pages", pages);
        obj.crossReferences.add(crossReference);

        this.data.add(obj);
    }

    public void sort() {
        Collections.sort(data, new Comparator<AgrRefs.DataObj>() {
            @Override
            public int compare(AgrRefs.DataObj o1, AgrRefs.DataObj o2) {
                return o1.primaryId.compareTo(o2.primaryId);
            }
        });
    }

    class DataObj {
        // required fields
        public String primaryId;
        public String title;
        public String datePublished; // f.e. '1931 Mar 30'
        public String citation;
        public String allianceCategory;

        // optional
        public String dateLastModified;
        public String volume;
        public String pages;
        public String ZABSTRACTZ; // trouble: cannot have a variable named 'abstract' in java
        public String publisher;
        public String issueName;
        public List MODReferenceTypes = new ArrayList();
        public List authors = new ArrayList();
        public List crossReferences = new ArrayList();
    }

}
