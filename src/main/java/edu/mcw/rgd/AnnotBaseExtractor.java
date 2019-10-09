package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.datamodel.ontologyx.TermSynonym;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author mtutaj
 * @since 5/30/12
 * Base class for extracting annotations
 */
abstract public class AnnotBaseExtractor extends BaseExtractor {

    abstract public String getAnnotDir();
    abstract public void setAnnotDir(String annotDir);
    abstract String getOutputFileNamePrefix(int speciesTypeKey);
    abstract String getOutputFileNameSuffix(String ontId, int objectKey);
    abstract String getHeaderCommonLines();
    abstract void writeLine(AnnotRecord rec, PrintWriter writer);
    abstract boolean processOnlyGenes();
    abstract boolean loadUniProtIds();

    // do not deconsolidate annotations
    // i.e. if annotation has several PMIDs, do not split it into multiple annotations, one PMID per annotation
    boolean suppressDeconsolidation = true; // (deconsolidation not implemented yet for CHEBI or CLINVAR annotations)

    private int speciesTypeKey;
    private String version;
    private Set<Integer> refRgdIdsForGoPipelines;
    private Logger logAnnot = Logger.getLogger("annot");

    // map of REF_RGD_IDs to PMID acc ids: used to significantly reduce database overhead
    // (issue one sql query vs millions of queries previously)
    private Map<Integer, String> pmidMap;

    private SimpleDateFormat sdt = new SimpleDateFormat("yyyyMMdd");

    public String getPmid(int refRgdId) {
        return pmidMap.get(refRgdId);
    }

    void loadPmidMap() throws Exception {
        synchronized(AnnotBaseExtractor.class) {
            if( pmidMap==null ) {
                pmidMap = getDao().loadPmidMap();
            }
        }
    }


    boolean onInit() {return true; }
    boolean acceptAnnotation(Annotation a) {
        return true;
    }
    void onDone() {}

    public int getSpeciesTypeKey() {
        return speciesTypeKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void run(SpeciesRecord si) throws Exception {

        loadPmidMap();

        speciesTypeKey = si.getSpeciesType();

        if( !onInit() ) {
            return;
        }

        logAnnot.info(getVersion());
        logAnnot.info("Processing annotations for "+si.getSpeciesName());

        // ensure annot dir does exist
        File annotDir = new File(getAnnotDir());
        if( !annotDir.exists() ) {
            annotDir.mkdirs();
        }

        // prefix for output file name dependent on species
        final int speciesType = speciesTypeKey;
        final String outputFileNamePrefix = getAnnotDir()+"/"+getOutputFileNamePrefix(speciesType);
        // the suffix will be ontology id -- generated automatically from the data

        // prepare header common lines
        final String commonLines = getHeaderCommonLines()
                .replace("#SPECIES#", si.getSpeciesName())
                .replace("#DATE#", SpeciesRecord.getTodayDate());

        // process active genes for given species
        CounterPool counters = new CounterPool();

        List<Annotation> annots = processOnlyGenes()
                ? getDao().getAnnotationsBySpecies(speciesType, RgdId.OBJECT_KEY_GENES)
                : getDao().getAnnotationsBySpecies(speciesType);

        Collection<Annotation> annots2 = suppressDeconsolidation ? annots : deconsolidateAnnotations(annots);

        annots2.parallelStream().forEach( a -> {

            if( !acceptAnnotation(a) ) {
                return;
            }

            AnnotRecord rec = new AnnotRecord();
            rec.annot = a;
            rec.taxon = "taxon:"+ SpeciesType.getTaxonomicId(speciesType);

            try {
                qc(rec, counters);

                // write out all the parameters to the file
                PrintWriter writer = getWriter(outputFileNamePrefix, commonLines, rec.ontId, rec.ontName);
                writeLine(rec, writer);

            } catch(Exception e) {
                logAnnot.info("getWriter("+outputFileNamePrefix+","+commonLines+","+rec.ontId+","+rec.ontName);
                throw new RuntimeException(e);
            }
        });

        // close the file writers
        closeFileWriters();

        logAnnot.info(counters.dumpAlphabetically());

        // show the number of orphaned annotations
        int count = counters.get("orphaned_annots");
        if( count>0 )
            logAnnot.info("count of orphaned annotations for "+si.getSpeciesName()+": "+count);

        onDone();
    }

    /**
     * NOTE: this method was almost literally copied from goc_annotation pipeline
     *
     * in RGD, we store pipeline annotations in consolidated form,
     * f.e. XREF_SOURCE: MGI:MGI:1100157|MGI:MGI:3714678|PMID:17476307|PMID:9398843
     *      NOTES:       MGI:MGI:2156556|MGI:MGI:2176173|MGI:MGI:2177226  (MGI:MGI:1100157|PMID:9398843), (MGI:MGI:3714678|PMID:17476307)
     * but GO spec says, REFERENCES column 6 can contain at most one PMID
     * so we must deconsolidate RGD annotations
     * what means we must split them into multiple, f.e.
     *    XREF_SOURCE1:  MGI:MGI:1100157|PMID:9398843
     *    XREF_SOURCE2:  MGI:MGI:3714678|PMID:17476307
     */
    Collection<Annotation> deconsolidateAnnotations(Collection<Annotation> annotations) throws Exception {

        int deconsolidatedAnnotsIncoming = 0;
        int deconsolidatedAnnotsCreated = 0;

        Set<Annotation> result = new TreeSet<>(new Comparator<Annotation>() {
            @Override
            public int compare(Annotation o1, Annotation o2) {
                int r = o1.getAnnotatedObjectRgdId() - o2.getAnnotatedObjectRgdId();
                if( r!=0 ) {
                    return r;
                }
                return o1.getTermAcc().compareTo(o2.getTermAcc());
            }
        });

        for( Annotation a: annotations ) {

            String xrefSrc = Utils.defaultString(a.getXrefSource());
            int posPmid1 = xrefSrc.indexOf("PMID:");
            int posPmid2 = xrefSrc.lastIndexOf("PMID:");
            if( !(posPmid1>=0 && posPmid2>posPmid1) ) {
                // only one PMID, annotation is already GO spec compliant
                result.add(a);
                continue;
            }
            deconsolidatedAnnotsIncoming++;

            int parPos = a.getNotes().indexOf("(");
            if( parPos<0 ) {
                logAnnot.warn("CANNOT DECONSOLIDATE ANNOTATION! SKIPPING IT: notes info missing: "+a.dump("|"));
                continue;
            }
            String notesOrig = a.getNotes().substring(0, parPos).trim();

            // multi PMID annotation: deconsolidate it
            String[] xrefs = xrefSrc.split("[\\|\\,]");
            for( ;; ){
                // extract PMID from xrefSrc
                String pmid = null;
                for( int i=0; i<xrefs.length; i++ ) {
                    if( xrefs[i].startsWith("PMID:") ) {
                        pmid = xrefs[i];
                        xrefs[i] = "";
                        break;
                    }
                }
                if( pmid==null ) {
                    break;
                }

                // find corresponding PMID info in NOTES field
                int pmidPos = a.getNotes().indexOf(pmid);
                if( pmidPos<0 ) {
                    logAnnot.warn("CANNOT DECONSOLIDATE ANNOTATION! SKIPPING IT: notes info missing PMID: "+a.dump("|"));
                    continue;
                }
                int parStartPos = a.getNotes().lastIndexOf("(", pmidPos);
                int parEndPos = a.getNotes().indexOf(")", pmidPos);
                if( parStartPos<0 || parEndPos<parStartPos ) {
                    logAnnot.warn("CANNOT DECONSOLIDATE ANNOTATION! SKIPPING IT: notes info malformed PMID: "+a.dump("|"));
                    continue;
                }
                String xrefInfo = a.getNotes().substring(parStartPos+1, parEndPos);

                Annotation ann = (Annotation)a.clone();
                ann.setXrefSource(xrefInfo);
                ann.setNotes(notesOrig);
                result.add(ann);
                deconsolidatedAnnotsCreated++;
            }
        }

        logAnnot.info(deconsolidatedAnnotsIncoming+" incoming annotations deconsolidated into "+deconsolidatedAnnotsCreated+" annotations");

        return result;
    }

    public Set<Integer> getRefRgdIdsForGoPipelines() {
        return refRgdIdsForGoPipelines;
    }

    public void setRefRgdIdsForGoPipelines(Set<Integer> refRgdIdsForGoPipelines) {
        this.refRgdIdsForGoPipelines = refRgdIdsForGoPipelines;
    }

    public void qc(AnnotRecord rec, CounterPool counters) throws Exception {

        // old disease or behavioral ontology term; use TERM and TERM_ACC directly from the annotation
        rec.termAccId = rec.annot.getTermAcc();
        rec.termName = rec.annot.getTerm();

        // filter out annotations for Not4Curation GO terms
        if( !getDao().isForCuration(rec.termAccId) ) {
            if( rec.termAccId.startsWith("GO:") ) {
                logAnnot.warn(" term "+rec.termAccId+" ["+rec.termName+"] is Not4Curation! annotation export skipped" );
                rec.ontId = null;
                return;
            }
            logAnnot.warn(" term "+rec.termAccId+" ["+rec.termName+"] is Not4Curation!" );
        }

        int objectKey = rec.annot.getRgdObjectKey();
        Term term = getDao().getTermByAccId(rec.annot.getTermAcc());
        if( term==null ) {
            rec.ontId = "orphaned";
            rec.ontName = "Orphaned annotations - terms no longer found in ontologies";
            counters.increment("orphaned_annots");
        }
        else {
            rec.ontId = getOutputFileNameSuffix(term.getOntologyId(), objectKey);
            rec.ontName = getDao().getOntology(term.getOntologyId()).getName();
        }

        switch( objectKey ) {
             case RgdId.OBJECT_KEY_GENES: rec.objectType = "gene"; break;
             case RgdId.OBJECT_KEY_QTLS: rec.objectType = "qtl"; break;
             case RgdId.OBJECT_KEY_STRAINS: rec.objectType = "strain"; break;
             case RgdId.OBJECT_KEY_VARIANTS: rec.objectType = "variant"; break;
             default: rec.objectType = "";
                 logAnnot.warn("unknown object type "+objectKey+" for annot key="+rec.annot.getKey());
                 rec.ontId = null; // to skip this term from processing
                 break;
        }

        int refRgdId = rec.annot.getRefRgdId()!=null && rec.annot.getRefRgdId()>0 ? rec.annot.getRefRgdId() : 0;
        if( refRgdId>0 ) {
            //(|DB:Reference) 	RGD:47763|PMID:2676709
            rec.references = "RGD:" + refRgdId;
            String pmid = getPmid(refRgdId);
            if( pmid!=null ){
                rec.references += "|PMID:" + pmid;
            }
        } else {
            // ref_rgd_id is null -- use non-null XREF_SOURCE as dbReference
            if( rec.annot.getXrefSource() != null )
                rec.references = rec.annot.getXrefSource();
        }

        rec.withInfo = rec.annot.getWithInfo(); //(or) From
        if ( rec.withInfo == null) {
            rec.withInfo = "";
        } else {
            // check for Pub Med id in this field, if it exists tack it on to the dbReference field
            if ( rec.withInfo.contains("PMID:") ) {
                if( rec.references.length()>0 )
                    rec.references += '|';
                rec.references += rec.withInfo;
                // print "Added With Field " + With + " to DBreference " + DBReference + "\n";
            }

            // Certain evidence codes cannot have with fields
            if ( rec.annot.getEvidence().equals("IDA") || rec.annot.getEvidence().equals("NAS") || rec.annot.getEvidence().equals("ND") || rec.annot.getEvidence().equals("TAS") ) {
                rec.withInfo = "";
            }
            else {
                rec.withInfo = rec.withInfo.trim();
            }
        }

        // GO consortium rule:
        // "protein binding" annotation -- GO:0005515 -- must have evidence 'IPI'
        //                   and non-null WITH field
        if( rec.termAccId.equals("GO:0005515") && (!rec.annot.getEvidence().equals("IPI") || rec.withInfo.length()==0 )) {
            // "protein binding" rule violation -- skip this row
            rec.ontId = null; // to skip this term from processing
        }

        // filter out ND annotations that violate GO rule GO_AR:0000011
        handleNDAnnotations(rec, counters);

        // determine correct created-date
        rec.createdDate = determineCreatedDate(rec.annot, counters);

        // extract some term synonyms for RDO and CHEBI
        handleTermSynonyms(rec);

        // extract curation notes if only if they are different from xref source
        String notes = Utils.defaultString(rec.annot.getNotes()).replaceAll("\\s"," ").trim();
        String xrefSource = Utils.defaultString(rec.annot.getXrefSource()).trim();
        if( !notes.isEmpty() || !notes.equals(xrefSource) )
            rec.curationNotes = notes;

        if( loadUniProtIds() ) {
            rec.uniprotIds = new HashSet<>();
            for( XdbId xdbId: getDao().getXdbIds(rec.annot.getAnnotatedObjectRgdId(), XdbId.XDB_KEY_UNIPROT) ) {
                rec.uniprotIds.add(xdbId.getAccId());
            }
        }

        if( objectKey==RgdId.OBJECT_KEY_GENES ) {
            handleMouseAndHumanPrimaryIds(rec);
        }
    }

    // GO consortium rule GO_AR:0000011:
    //   (http://www.geneontology.org/GO.annotation_qc.shtml#GO_AR:0000011)
    // The No Data (ND) evidence code should be used for annotations to the root nodes only
    // and should be accompanied with GO_REF:0000015 or an internal reference.
    void handleNDAnnotations(AnnotRecord rec, CounterPool counters) {
        // CASE 1: evidence.code = 'ND' AND term.acc NOT IN ( 'GO:0005575', 'GO:0003674', 'GO:0008150' )
        if( rec.annot.getEvidence().equals("ND") && rec.termAccId.startsWith("GO:")
                && !(rec.termAccId.equals("GO:0005575") || rec.termAccId.equals("GO:0003674") || rec.termAccId.equals("GO:0008150")) ) {

            counters.increment("Skipped GO ND annots to non-root terms");
            rec.ontId = null;
            return;
        }

        // CASE 2: evidence.code != 'ND' AND term.acc IN ( 'GO:0005575', 'GO:0003674', 'GO:0008150' )
        if( !rec.annot.getEvidence().equals("ND")
                && (rec.termAccId.equals("GO:0005575") || rec.termAccId.equals("GO:0003674") || rec.termAccId.equals("GO:0008150")) ) {

            counters.increment("Skipped GO non-ND annots to root terms");
            rec.ontId = null;
            return;
        }

        // CASE 3: evidence.code = 'ND' AND term.acc IN ( 'GO:0005575', 'GO:0003674', 'GO:0008150' )
        //   and xref_db NOT IN( 'GO_REF:0000015', 'RGD:1598407')
        if( rec.annot.getEvidence().equals("ND")
                && (rec.termAccId.equals("GO:0005575") || rec.termAccId.equals("GO:0003674") || rec.termAccId.equals("GO:0008150")) ) {

            if( !rec.references.equals("GO_REF:0000015") && !rec.references.equals("RGD:1598407") ) {

                counters.increment("GO ND annot reference set to GO_REF:0000015 for "+rec.references);
                rec.references = "GO_REF:0000015";
            }
        }
    }

    void handleTermSynonyms(AnnotRecord rec) throws Exception {

        // term synonyms are processed only for CHEBI and RDO
        if( !rec.termAccId.startsWith("RDO") && !rec.termAccId.startsWith("CHEBI") ) {
            return;
        }

        List<TermSynonym> synonyms = getDao().getTermSynonyms(rec.termAccId);

        // for RDO term acc, retrieve corresponding MESH or OMIM id
        if( rec.termAccId.startsWith("RDO") ) {
            rec.meshOrOmimId = getTermSynonym(synonyms, "primary_id");
            if( rec.meshOrOmimId!=null && rec.meshOrOmimId.startsWith("OMIM:") ) {
                rec.meshOrOmimId = "OMIM:" + Integer.parseInt(rec.meshOrOmimId.substring(5).trim(), 10);
            }
        }
        // extract MESH ids for CHEBI
        else if( rec.termAccId.startsWith("CHEBI") ) {
            rec.meshOrOmimId = getTermSynonym(synonyms, "xref_mesh");
        }
    }

    String getTermSynonym(List<TermSynonym> synonyms, String synonymType) {
        for(TermSynonym tsyn: synonyms ) {
            if( Utils.stringsAreEqual(tsyn.getType(), synonymType) ) {
                return Utils.defaultString(tsyn.getName()).trim();
            }
        }
        return null;
    }

    //  calls to sdt.format must be synchronized
    synchronized String formatDate(java.util.Date dt) {
        // date formatting is not synchronized :-(
        return dt != null ? sdt.format(dt) : "";
    }

    // GO consortium rule: IEA annotations cannot be more than one year old
    //   since creation date of an annotation *never* changes, as of May 2016, per ticket RGDD-1194
    //   then all GO 'IEA' annotations must use LAST_MODIFIED_DATE instead of CREATED_DATE during export
    //   (note that LAST_MODIFIED_DATE for an annotation is updated during every pipeline run)
    String determineCreatedDate(Annotation annot, CounterPool counters) {

        Date createdDate;
        if( annot.getTermAcc().startsWith("GO:") && annot.getEvidence().equals("IEA") ) {
            if( getRefRgdIdsForGoPipelines().contains(annot.getRefRgdId()) ) {
                // this update (to prevent the GOC error that occurs when an IEA annotation is more than a year old) was previously
                // run manually on a theoretically yearly basis.  It is much better to have it run automatically as part of the pipeline
                // so it isn't forgotten.  The annotations in question were originally assigned as ISS annotations based on
                // the similarity in sequence between member of the putative rat Olr (olfactory receptor) family and similar genes
                // in other species but the annotations were basically assigned manually but en masse rather than being individually
                // reviewed, so the decision was later made to change them to IEA annotations.  Since they were not assigned by
                // a pipeline, however, they would not be updated on a regular basis as the GOC assumes would be done for IEA
                // annotations.  Hence the need to update the "created date" each year.  Just to give some history.
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.DATE, -90); // created date should be 3 months back from the current date
                createdDate = calendar.getTime();
            } else {
                createdDate = annot.getLastModifiedDate();
            }
            counters.increment("GO_annots_with_IEA_evidence");
        } else {
            createdDate = annot.getCreatedDate();
        }
        return formatDate(createdDate);
    }

    void handleMouseAndHumanPrimaryIds(AnnotRecord rec) throws Exception {
        if (getSpeciesTypeKey() == SpeciesType.HUMAN) {
            List<XdbId> xdbIds = getDao().getXdbIds(rec.annot.getAnnotatedObjectRgdId(), XdbId.XDB_KEY_HGNC);
            if( !xdbIds.isEmpty() ) {
                rec.hgncId = xdbIds.get(0).getAccId();
                if( xdbIds.size()>1 ) {
                    String multiId = "MULTI HGNC Ids for RGD:"+rec.annot.getAnnotatedObjectRgdId()+": "+Utils.concatenate(",", xdbIds, "getAccId");
                    if( _multiIds.add(multiId) ) {
                        logAnnot.info(multiId);
                    }
                }
            }
        }
        if (getSpeciesTypeKey() == SpeciesType.MOUSE) {
            List<XdbId> xdbIds = getDao().getXdbIds(rec.annot.getAnnotatedObjectRgdId(), XdbId.XDB_KEY_MGD);
            if( !xdbIds.isEmpty() ) {
                rec.mgdId = xdbIds.get(0).getAccId();
                if( xdbIds.size()>1 ) {
                    String multiId = "MULTI MGD Ids for RGD:"+rec.annot.getAnnotatedObjectRgdId()+": "+Utils.concatenate(",", xdbIds, "getAccId");
                    if( _multiIds.add(multiId) ) {
                        logAnnot.info(multiId);
                    }
                }
            }
        }
    }


    static Set<String> _multiIds = new ConcurrentSkipListSet<>();

    String checkNull(String str) {
        return str==null ? "" : str.trim();
    }


    // map of ontology id to its file writer
    HashMap<String, PrintWriter> writers = new HashMap<>();

    private synchronized PrintWriter getWriter(String filePrefix, String header, String ontId, String ontName) throws IOException {

        PrintWriter writer = writers.get(ontId);
        if (writer == null) {
            // create a new writer and write header
            writer = new PrintWriter(filePrefix + ontId);
            writers.put(ontId, writer);
            writer.write(header.replace("#ONT#", ontName));
        }
        return writer;
    }

    // close all writers
    public void closeFileWriters() {
        for( PrintWriter writer: writers.values() ) {
            writer.close();
        }
    }

    class AnnotRecord {
        public Annotation annot;
        public String ontId; // like 'go','do','mp','pw' -- if null, the record is to be skipped
        public String ontName; // ontology name
        public String meshOrOmimId; // optional MESH or OMIM id
        public String hgncId; // optional HGNC id -- only for human genes -- in AGR approved format: 'HGNC:xxx'
        public String mgdId; // optional MGD id -- only for mouse genes -- in AGR approved format: 'MGI:xxx'
        public String curationNotes;
        public String taxon;

        public String objectType;
        public String termAccId;
        public String termName;
        public String references;
        public String withInfo;
        public String createdDate;
        public Collection<String> uniprotIds;
    }

}
