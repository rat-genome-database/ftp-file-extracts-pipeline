package edu.mcw.rgd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Reference;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.process.Utils;

import java.io.*;
import java.text.ParseException;
import java.util.*;

public class AgrRefExtractor extends BaseExtractor {

    static String[] allianceCategories = new String[]{"Research Article","Review Article","Thesis","Book","Other","Preprint",
            "Conference Publication","Personal Communication","Direct Data Submission","Internal Process Reference", "Unknown","Retraction"};

    Map<String,String> rgdToAllianceCategories = new HashMap<>();

    public AgrRefExtractor() {

        rgdToAllianceCategories.put("", "Unknown");
        rgdToAllianceCategories.put("ABSTRACT", "Other");
        rgdToAllianceCategories.put("BOOK REVIEW", "Other");
        rgdToAllianceCategories.put("BOOK", "Book");
        rgdToAllianceCategories.put("WEBSITE", "Internal Process Reference");
        rgdToAllianceCategories.put("PERSONAL COMMUNICATION", "Personal Communication");
        rgdToAllianceCategories.put("JOURNAL ARTICLE", "Research Article");
        rgdToAllianceCategories.put("DIRECT DATA TRANSFER", "Internal Process Reference");
    }

    static boolean versionPrintedOut = false;

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        if( versionPrintedOut ) {
            return;
        }
        versionPrintedOut = true;
        System.out.println(getVersion());

        Map<Integer,String> pmidMap = getDao().loadPmidMap();
        List<Reference> refsInRgd = getDao().getActiveReferences();


        processReferences(refsInRgd, pmidMap);
        processRefExchange(refsInRgd, pmidMap);

    }

    void processReferences(List<Reference> refsInRgd, Map<Integer,String> pmidMap) throws Exception {
        int refsWithPmids = 0;
        int totalRefs = 0;

        AgrRefs refs = new AgrRefs();

        for( Reference r: refsInRgd ) {
            totalRefs++;

            String pmid = pmidMap.get(r.getRgdId());
            String primaryId;
            if( pmid==null ) {
                primaryId = "RGD:" + r.getRgdId();
            } else {
                primaryId = "PMID:"+pmid;
                refsWithPmids++;
            }

            RgdId id = getDao().getRgdId(r.getRgdId());

            String allianceCategory = rgdToAllianceCategories.get(r.getReferenceType());

            refs.addDataObj(primaryId, r, id.getLastModifiedDate(), allianceCategory, getDao().getAuthors(r.getKey()));
        }
        refs.sort();
        dumpReferencesToJson(refs);

        System.out.println("total refs: "+totalRefs);
        System.out.println("refs with PMID: "+refsWithPmids);
    }

    void dumpReferencesToJson(AgrRefs agrRefs) throws ParseException, IOException {

        // setup a JSON object array to collect all DafAnnotation objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // dump records to a file in JSON format
        String jsonFileNameTmp = "data/agr/REFERENCE_RGD.json.tmp";
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(jsonFileNameTmp), "UTF8");
            BufferedWriter jsonWriter = new BufferedWriter(out);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(agrRefs));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        // replace field 'ZBASTRACTZ' with 'abstract'
        String line;
        BufferedReader in = Utils.openReader(jsonFileNameTmp);
        String jsonFileName = "data/agr/REFERENCE_RGD.json";

        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(jsonFileName), "UTF8");
        BufferedWriter jsonWriter = new BufferedWriter(out);
        while( (line=in.readLine())!=null ) {
            jsonWriter.write(line.replace("\"ZABSTRACTZ\" : ", "\"abstract\" : "));
            jsonWriter.write("\n");
        }
        jsonWriter.close();
        in.close();

        // remove tmp file
        new File(jsonFileNameTmp).delete();
    }


    void processRefExchange( List<Reference> refsInRgd, Map<Integer,String> pmidMap ) throws Exception {

        // REF-EXCHANGE (small set of meta data that is exchanged from a MOD to ABC to establish and update PubMed corpus)
        AgrRefExchange refExchange = new AgrRefExchange();

        int entries = 0;
        for( Reference r: refsInRgd ) {

            String pmid = pmidMap.get(r.getRgdId());

            if( pmid!=null ) {
                RgdId id = getDao().getRgdId(r.getRgdId());

                String allianceCategory = rgdToAllianceCategories.get(r.getReferenceType());

                refExchange.addDataObj(pmid, r, id.getLastModifiedDate(), allianceCategory);

                entries++;
            }
        }
        refExchange.sort();
        dumpRefExchangeToJson(refExchange);
        System.out.println("total ref-exchange entries: "+entries);
    }

    void dumpRefExchangeToJson(AgrRefExchange agrRefs) throws ParseException, IOException {

        // setup a JSON object array to collect all DafAnnotation objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // dump records to a file in JSON format
        String jsonFileName = "data/agr/REF-EXCHANGE_RGD.json";
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(jsonFileName), "UTF8");
            BufferedWriter jsonWriter = new BufferedWriter(out);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(agrRefs));

            jsonWriter.close();
        } catch(IOException ignore) {
        }
    }
}
