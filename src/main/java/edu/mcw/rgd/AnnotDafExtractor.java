package edu.mcw.rgd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.Omim;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.DafAnnotation;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * RGD manual disease annotations fitted to the DAF specification.
 * The annotated objects being exported are genes, qtls and strains.
 */
public class AnnotDafExtractor extends AnnotBaseExtractor {

    Logger log = Logger.getLogger(AnnotDafExtractor.class);
    Logger logDaf = Logger.getLogger("daf");

    CounterPool counters;

    ObjectMapper json;
    DafExport dafExport;

    private String fileJsonPrefix;
    private String fileJsonSuffix;

    // a good place to initialize variables for the species being processed
    boolean onInit() {

        // generate only for rat mouse and human
        if( getSpeciesTypeKey()!=SpeciesType.RAT &&
            getSpeciesTypeKey()!=SpeciesType.MOUSE &&
            getSpeciesTypeKey()!=SpeciesType.HUMAN ) {
            return false;
        }

        counters = new CounterPool();

        dafExport = new DafExport();

        // setup a JSON object array to collect all DafAnnotation objects
        json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return true;
    }

    String getOutputFileNamePrefix(int speciesTypeKey) {
        String taxId = Integer.toString(SpeciesType.getTaxonomicId(getSpeciesTypeKey()));
        return getFileJsonPrefix() + taxId + getFileJsonSuffix();
    }

    String getOutputFileNameSuffix(String ontId, int objectKey) {
        return "";
    }

    @Override
    String getHeaderCommonLines() {
        return "";
    }

    String writeLine(AnnotRecord rec) {

        // process only genes and strains
        boolean isGene = rec.objectType.equals("gene");
        boolean isStrain = rec.objectType.equals("strain");
        if( !(isGene || isStrain) ) {
            return null;
        }

        // only process RGD manual disease annotations and OMIM IEA annotations
        if( !(rec.annot.getDataSrc().equals("RGD") || rec.annot.getDataSrc().equals("OMIM")) ) {
            return null;
        }
        if( !rec.termAccId.startsWith("DOID:") ) {
            return null;
        }

        // for human genes, HGNC id; for mouse, MGD id; for other species, RGD id
        String objectID;
        if( getSpeciesTypeKey()==SpeciesType.HUMAN ) {
            if (Utils.isStringEmpty(rec.hgncId)) {
                return null; // skip genes without HGNC id
            }
            objectID = rec.hgncId;
        }
        else if( getSpeciesTypeKey()==SpeciesType.MOUSE ) {
            if( Utils.isStringEmpty(rec.mgdId) ) {
                return null; // skip genes without MGD id
            }
            objectID = rec.mgdId;
        } else {
            objectID = "RGD:"+rec.annot.getAnnotatedObjectRgdId();
        }

        // create association type
        String assocType;
        if( isGene ) {
            // for genes evidence code must be a manual evidence code
            switch (rec.annot.getEvidence()) {
                case "IEP":
                    assocType = "is_marker_for";
                    break;
                case "IAGP":
                case "IMP":
                case "IDA":
                case "IGI":
                    assocType = "is_implicated_in";
                    break;
                //case "IEA": assocType = "is_implicated_in"; break; // for OMIM annotations
                default:
                    return null; // not a manual evidence code
            }
        } else {
            // for strains: skip annotations with IEA, IEP, QTM or TAS evidence codes
            switch (rec.annot.getEvidence()) {
                case "IEA":
                case "IEP":
                case "QTM":
                case "TAS":
                    return null;
                default:
                    assocType = "is_model_of";
                    break;
            }
        }

        // exclude DO+ custom terms (that were added by RGD and are not present in DO ontology)
        if( rec.termAccId.startsWith("DOID:90") && rec.termAccId.length()==12 ) {

            // see if this term could be mapped to an OMIM PS id
            String parentTermAcc = null;
            try {
                parentTermAcc = getDao().getOmimPSTermAccForChildTerm(rec.termAccId, counters);
            } catch( Exception e ) {
                throw new RuntimeException(e);
            }
            if( parentTermAcc==null ) {
                return null;
            }

            if( parentTermAcc.startsWith("DOID:90") && parentTermAcc.length()==12 ) {
                counters.increment("OMIM:PS conversion FAILED: " + rec.termAccId + " [" + rec.annot.getTerm() + "]) has DO+ parent " + parentTermAcc);
                return null;
            } else {
                counters.increment("OMIM:PS conversion OK: " + rec.termAccId + " [" + rec.annot.getTerm() + "]) replaced with " + parentTermAcc);
                rec.termAccId = parentTermAcc;
                counters.increment("omimPSConversions");
            }
        }

        counters.increment("recordsExported");

        // remove 'RGD:xxx' from reference list
        String pmids = getPmids(rec.references);

        log.debug(rec.annot.getObjectSymbol()+"|"+assocType+"|"+rec.annot.getEvidence()+"|"+pmids+"|"+rec.createdDate);

        // gaf files use this format: 'taxon:xxxxx',
        // while daf files uses this format: 'NCBITaxon:xxxxx'
        String taxon = rec.taxon.replace("taxon:", "NCBITaxon:");

        DafAnnotation daf = new DafAnnotation();
        daf.setTaxon(taxon);
        daf.setDbObjectType(rec.objectType);
        daf.setDb("RGD");
        daf.setDbObjectID(objectID);
        daf.setDbObjectSymbol(rec.annot.getObjectSymbol());
        daf.setAssociationType(assocType);
        daf.setQualifier(checkNull(rec.annot.getQualifier()).equals("no_association") ? "NOT" : null);
        daf.setDoId(rec.termAccId);
        daf.setWithInfo(rec.annot.getWithInfo());
        daf.setEvidenceCode(rec.annot.getEvidence());
        daf.setDbReference(pmids);
        daf.setCreatedDate(rec.createdDate);
        daf.setDataProviders(getDataProviders(rec));

        // handle gene alleles: inferredGeneAssociation non null only for gene alleles
        if( isGene ) {
            daf.setInferredGeneAssociation(getGeneRgdIdsForAllele(rec.annot.getAnnotatedObjectRgdId()));
        }

        DafExport.DafData dafData = dafExport.addData(daf, rec.annot.getRefRgdId());
        counters.increment(dafData.objectRelation.objectType+"RecordsExported");

        return null;
    }

    List<HashMap> getDataProviders(AnnotRecord rec) {

        List<HashMap> result = new ArrayList<HashMap>();

        if( rec.annot.getDataSrc().equals("OMIM") ) {

            // generate OMIM entry
            HashMap entry = new HashMap();
            entry.put("type", "curated");
            HashMap crossRef = new HashMap();
            entry.put("crossReference", crossRef);

            crossRef.put("id", getGeneOmimId(rec.annot.getAnnotatedObjectRgdId(), rec.termAccId));
            List<String> pages = new ArrayList<>();
            pages.add("gene");
            crossRef.put("pages", pages);

            result.add(entry);
        }

        // now link to ontology annotation table
        HashMap entry = new HashMap();
        entry.put("type", result.isEmpty() ? "curated" : "loaded");
        HashMap crossRef = new HashMap();
        entry.put("crossReference", crossRef);

        crossRef.put("id", rec.termAccId);
        List<String> pages = new ArrayList<>();
        pages.add("disease/"+SpeciesType.getCommonName(getSpeciesTypeKey()).toLowerCase());
        crossRef.put("pages", pages);

        result.add(entry);

        return result;
    }

    String getGeneOmimId(int geneRgdId, String phenotypeOmimId) {

        try {
            List<XdbId> omimIds = getDao().getXdbIds(geneRgdId, XdbId.XDB_KEY_OMIM);

            // remove phenotype OMIM ids
            if( omimIds.size()>1 ) {
                //logDaf.info("  MULTIS: remove phenotype OMIM ids for "+phenotypeOmimId);
                Iterator<XdbId> it = omimIds.iterator();
                while (it.hasNext()) {
                    XdbId id = it.next();
                    Omim omim = getDao().getOmimByNr(id.getAccId());
                    if( omim==null ) {
                        logDaf.info("NULL OMIM table entry for OMIM:"+id.getAccId());
                    }
                    else if (omim.getMimType().equals("phenotype") || omim.getMimType().equals("moved/removed")) {
                        it.remove();
                    }
                }
            }

            if( omimIds.size()==0 ) {
                logDaf.info("NO GENE OMIM for "+phenotypeOmimId+ ", RGD:"+geneRgdId);
                return "OMIM";
            }

            String omimId = "OMIM:"+omimIds.get(0).getAccId();

            if( omimIds.size()==1 ) {
                //logDaf.info("SINGLE GENE OMIM "+omimIds.get(0).getAccId()+" for "+phenotypeOmimId);
                return omimId;
            }

            logDaf.info("MULTIPLE GENE OMIMs for "+phenotypeOmimId + ", RGD:"+geneRgdId+" {"+Utils.concatenate(",", omimIds, "getAccId")+"}");
            // just pick an OMIM id by random
            return omimId;

        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    String getGeneRgdIdsForAllele(int rgdId) {
        try {
            List<Gene> parentGenes = getDao().getGenesForAllele(rgdId);
            if( parentGenes==null || parentGenes.isEmpty() ) {
                return null;
            }
            String parentRgdIds = "RGD:"+parentGenes.get(0).getRgdId();
            for( int i=1; i<parentGenes.size(); i++ ) {
                parentRgdIds += ",RGD:" + parentGenes.get(i).getRgdId();
            }
            return parentRgdIds;
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    String getPmids(String references) {

        Set<String> pmids = new TreeSet<>();
        String[] refs = references.split("[\\|\\,]");
        for( String ref: refs ) {
            if( ref.startsWith("PMID:") ) {
                pmids.add(ref);
            }
        }
        return Utils.concatenate(pmids, "|");
    }


    void onDone() {
        log.info("Records exported: "+counters.get("recordsExported"));
        log.info("   for genes    : "+counters.get("geneRecordsExported"));
        log.info("   for strains  : "+counters.get("strainRecordsExported"));
        log.info("   for alleles  : "+counters.get("alleleRecordsExported"));
        log.info("DO+ records exported thanks to OMIM:PS conversions: "+counters.get("omimPSConversions"));

        Enumeration<String> counterNames = counters.getCounterNames();
        while( counterNames.hasMoreElements() ) {
            String counterName = counterNames.nextElement();
            if( counterName.startsWith("OMIM:PS") ) {
                int value = counters.get(counterName);
                log.info(counterName + (value>1?("   ["+value+" hits]"):"   [1 hit]"));
            }
        }

        // sort data, alphabetically by object symbols
        dafExport.sort();

        // dump DafAnnotation records to a file in JSON format
        try {
            String jsonFileName = getAnnotDir()+ "/" + getOutputFileNamePrefix(getSpeciesTypeKey());
            BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(jsonFileName));

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(dafExport));

            jsonWriter.close();
        } catch(IOException ignore) {
        }
        log.info("");
    }

    private String annotDir;

    public String getAnnotDir() {
        return annotDir;
    }

    public void setAnnotDir(String annotDir) {
        this.annotDir = annotDir;
    }

    boolean processOnlyGenes() {
        return false;
    }

    boolean loadUniProtIds() {
        return false;
    }

    public void setFileJsonPrefix(String fileJsonPrefix) {
        this.fileJsonPrefix = fileJsonPrefix;
    }

    public String getFileJsonPrefix() {
        return fileJsonPrefix;
    }

    public void setFileJsonSuffix(String fileJsonSuffix) {
        this.fileJsonSuffix = fileJsonSuffix;
    }

    public String getFileJsonSuffix() {
        return fileJsonSuffix;
    }
}