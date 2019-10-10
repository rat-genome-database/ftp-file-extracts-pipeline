package edu.mcw.rgd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.ontology.DafAnnotation;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * RGD manual disease annotations fitted to the DAF specification.
 * The annotated objects being exported are genes, qtls and strains.
 */
public class AnnotDafExtractor extends AnnotBaseExtractor {

    Logger log = Logger.getLogger(AnnotDafExtractor.class);

    int recordsExported;
    int omimPSConversions;
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

        recordsExported = 0;
        omimPSConversions = 0;

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

        // evidence code must be a manual evidence code
        // create association type
        String assocType;
        switch( rec.annot.getEvidence() ) {
            case "IAGP": assocType = "is_implicated_in"; break;
            case "IMP": assocType = "is_implicated_in"; break;
            case "IEP": assocType = "is_marker_for"; break;
            case "IDA": assocType = "is_implicated_in"; break;
            case "IGI": assocType = "is_implicated_in"; break;
            //case "IEA": assocType = "is_implicated_in"; break; // for OMIM annotations
            default:
                return null; // not a manual evidence code
        }

        // exclude DO+ custom terms (that were added by RGD and are not present in DO ontology)
        if( rec.termAccId.startsWith("DOID:90") && rec.termAccId.length()==12 ) {

            // see if this term could be mapped to an OMIM PS id
            String parentTermAcc = null;
            try {
                parentTermAcc = getDao().getOmimPSTermAccForChildTerm(rec.termAccId);
            } catch( Exception e ) {
                throw new RuntimeException(e);
            }
            if( parentTermAcc==null ) {
                return null;
            }

            if( parentTermAcc.startsWith("DOID:90") && parentTermAcc.length()==12 ) {
                System.out.println("  OMIM:PS conversion FAILED: " + rec.termAccId + " [" + rec.annot.getTerm() + "]) has DO+ parent " + parentTermAcc);
                return null;
            } else {
                System.out.println("  OMIM:PS conversion OK: " + rec.termAccId + " [" + rec.annot.getTerm() + "]) replaced with " + parentTermAcc);
                rec.termAccId = parentTermAcc;
                omimPSConversions++;
            }
        }

        recordsExported++;

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

        // currently AGR does not support OMIM as data source, so we must use 'RGD'
        HashMap<String,String> dataProviders = new HashMap<>();
        if( rec.annot.getDataSrc().equals("OMIM") ) {
            dataProviders.put("OMIM", "curated");
            dataProviders.put("RGD", "loaded");
        } else {
            dataProviders.put("RGD", "curated");
        }
        daf.setDataProviders(dataProviders);

        // handle gene alleles: inferredGeneAssociation non null only for gene alleles
        daf.setInferredGeneAssociation(getGeneRgdIdsForAllele(rec.annot.getAnnotatedObjectRgdId()));

        dafExport.addData(daf, rec.annot.getRefRgdId());

        return null;
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
        log.info("Records exported: "+recordsExported);
        log.info("DO+ records exported thanks to OMIM:PS conversions: "+omimPSConversions);

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
        return true;
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