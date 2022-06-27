package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.EvidenceCode;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.DafAnnotation;
import edu.mcw.rgd.process.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by mtutaj on 4/12/2017.
 */
public class DafExport {

    public DafMetadata metaData = new DafMetadata();
    public List<DafData> data = new ArrayList<>();

    SimpleDateFormat sdt = new SimpleDateFormat("yyyyMMdd");
    SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    class DafMetadata {
        public final HashMap dataProvider;
        public final String dateProduced;
        public final String release;

        public DafMetadata() {
            synchronized(DafExport.class) {
                dataProvider = getDataProviderForMetaData();
                release = "RGD Daf Extractor, AGR schema 1.0.1.4, build  Feb 17, 2022";

                SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                dateProduced = sdf_agr.format(new Date());
            }
        }
    }

    class DafData {
        // required fields
        public String objectId;
        public DafObjectRelation objectRelation = new DafObjectRelation();
        public String DOid;
        public List<HashMap> dataProvider;
        public DafEvidence evidence = new DafEvidence();
        public String dateAssigned;

        public String objectName;
        public String negation; // optional, previously 'qualifier'
        public List<String> with;
        public List<DafConditionRelation> conditionRelations;
    }

    class DafObjectRelation {
        public String objectType;
        public String associationType;
        public List<String> inferredGeneAssociation = null;
    }

    class DafEvidence {
        public ArrayList<String> evidenceCodes = new ArrayList<>();
        public DafPublication publication = new DafPublication();
    }

    class DafPublication {
        public Map<String,Object> crossReference;
        public String publicationId;
    }

    class DafConditionRelation {
        public String conditionRelationType;
        public List conditions;
    }

    public DafData addData(DafAnnotation a, int refRgdId, FtpFileExtractsDAO dao) {

        DafData data = new DafData();
        data.objectId = a.getDbObjectID();

        data.objectRelation.associationType = a.getAssociationType();
        // handle gene alleles
        if( a.getInferredGeneAssociation()!=null ) {
            data.objectRelation.objectType = "allele";
            String[] parentGeneIds = a.getInferredGeneAssociation().split("[,]");
            data.objectRelation.inferredGeneAssociation = Arrays.asList(parentGeneIds);
        } else {
            data.objectRelation.objectType = a.getDbObjectType();
        }

        data.DOid = a.getDoId();
        data.dataProvider = a.getDataProviders();

        String ecoId = getEcoId(a.getEvidenceCode());
        if( ecoId==null ) {
            System.out.println("WARN no ECO_ID for evidence code "+a.getEvidenceCode());
            return null;
        }
        data.evidence.evidenceCodes.add(ecoId);

        handlePublication(a.getDbReference(), refRgdId, data.evidence.publication);

        data.dateAssigned = getDateAssigned(a.getCreatedDate());

        data.objectName = a.getDbObjectSymbol();

        if( a.getQualifier()!=null ) {
            String q = a.getQualifier().toLowerCase();
            if( q.equals("not") || q.equals("no_association") || q.contains("control") ) {
                data.negation = "not";
            }
        }

        try {
            if (!handleWithInfo(a, data, dao)) {
                return null;
            }

            // TMP request: suppress export of WITH fields
            data.with = null;

        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        if( data.evidence.publication.publicationId==null ) {
            System.out.println("annot skipped because publicationRef is empty");
            return null;
        } else {
            synchronized (this.data) {
                this.data.add(data);
            }
        }

        return data;
    }

    boolean handleWithInfo(DafAnnotation a, DafData data, FtpFileExtractsDAO dao) throws Exception {

        if( a.getWithInfo()==null ) {
            return true;
        }

        // only a subset of qualifiers is allowed
        String condRelType = null;
        if( a.getQualifier() == null ) {
            condRelType = "has_condition";
        } else if( a.getQualifier().contains("induced") || a.getQualifier().contains("induces") ) {
            condRelType = "induces";
        } else if( a.getQualifier().contains("treatment") || a.getQualifier().contains("ameliorates") ) {
            condRelType = "ameliorates";
        } else if( a.getQualifier().contains("exacerbates") ) {
            condRelType = "exacerbates";
        } else {
            System.out.println("UNMAPPED QUALIFIER: "+a.getQualifier());
            condRelType = "has_condition";
        }

        // remove all whitespace from WITH field to simplify parsing
        String withInfo = a.getWithInfo().replaceAll("\\s", "");
        List conditionRelations = new ArrayList();

        // if the separator is '|', create separate conditionRelation object
        // if the separator is ',', combine conditions
        boolean or;

        // out[0]: token;  out[1]: separator before token
        String[] out = new String[2];
        String str = withInfo;
        for( ;; ) {
            str = getNextToken(str, out);
            if( out[0]==null ) {
                break;
            }

            String withValue = out[0];
            if( out[1]!=null && out[1].equals(",") ) {
                or = false;
            } else {
                or = true;
            }

            withValue = transformRgdId(withValue, dao);
            if( withValue==null ) {
                return false;
            }

            if( withValue.startsWith("XCO:") ) {
                AgrExperimentalConditionMapper.Info info = AgrExperimentalConditionMapper.getInstance().getInfo(withValue);
                if (info == null) {
                    System.out.println("UNEXPECTED WITH VALUE: " + withValue);
                    return false;
                }


                HashMap h = new HashMap();
                h.put("conditionClassId", info.zecoAcc);
                if (info.xcoAcc != null && info.xcoAcc.startsWith("CHEBI:")) {
                    h.put("chemicalOntologyId", info.xcoAcc);
                } else {
                    h.put("conditionId", info.xcoAcc);
                }
                h.put("conditionStatement", info.conditionStatement);

                if (or) {
                    DafConditionRelation condRel = new DafConditionRelation();
                    condRel.conditionRelationType = condRelType;

                    condRel.conditions = new ArrayList();
                    condRel.conditions.add(h);

                    conditionRelations.add(condRel);
                } else {
                    // 'and' operator: update last condition
                    DafConditionRelation condRel = (DafConditionRelation) conditionRelations.get(conditionRelations.size() - 1);
                    condRel.conditions.add(h);
                }
            } else {
                // NOTE: per Alliance request, we suppress export of any WITH fields
                //
                // non-XCO with value
                //if( data.with==null ) {
                //    data.with = new ArrayList<>();
                //}
                //data.with.add(withValue);
            }
        }

        if( !conditionRelations.isEmpty() ) {
            data.conditionRelations = conditionRelations;

            if( conditionRelations.size()>2 ) {
                System.out.println("MULTI CONDRELS "+data.objectId+" "+data.DOid);
            }
        }
        return true;
    }

    // convert human rgd ids to HGNC ids, and mouse rgd ids to MGI ids
    String transformRgdId(String with, FtpFileExtractsDAO dao) throws Exception {

        if (with.startsWith("RGD:")) {
            Integer rgdId = Integer.parseInt(with.substring(4));
            RgdId id = dao.getRgdId(rgdId);
            if (id == null) {
                System.out.println("ERROR: invalid RGD ID " + with + "; skipping annotation");
                return null;
            }

            if (id.getSpeciesTypeKey() == SpeciesType.HUMAN) {
                List<XdbId> xdbIds = dao.getXdbIds(rgdId, XdbId.XDB_KEY_HGNC);
                if (xdbIds.isEmpty()) {
                    System.out.println("ERROR: cannot map " + with + " to human HGNC ID");
                    return null;
                }
                if (xdbIds.size() > 1) {
                    System.out.println("WARNING: multiple HGNC ids for " + with);
                }
                String hgncId = xdbIds.get(0).getAccId();
                return hgncId;
            } else if (id.getSpeciesTypeKey() == SpeciesType.MOUSE) {
                List<XdbId> xdbIds = dao.getXdbIds(rgdId, XdbId.XDB_KEY_MGD);
                if (xdbIds.isEmpty()) {
                    System.out.println("ERROR: cannot map " + with + " to mouse MGI ID");
                    return null;
                }
                if (xdbIds.size() > 1) {
                    System.out.println("WARNING: multiple MGI ids for " + with);
                }
                String mgiId = xdbIds.get(0).getAccId();
                return mgiId;
            } else if (id.getSpeciesTypeKey() == SpeciesType.RAT) {
                return with;
            } else {
                System.out.println("ERROR: RGD id for species other than rat,mouse,human in WITH field");
                return null;
            }
        }

        return with;
    }

    // str: string to be parsed
    // out: out[0]-extracted term; out[1]-separator before term
    // return rest of string 'str' after extracting the token
    String getNextToken(String str, String[] out) {

        if( str==null ) {
            out[0] = null;
            out[1] = null;
            return null;
        }

        int startPos = 0;
        if( str.startsWith("|") ) {
            out[1] = "|";
            startPos = 1;
        } else if( str.startsWith(",") ) {
            out[1] = ",";
            startPos = 1;
        } else {
            out[1] = null;
        }

        int endPos = str.length();

        int barPos = str.indexOf('|', startPos);
        if( barPos>=0 && barPos < endPos ) {
            endPos = barPos;
        }
        int commaPos = str.indexOf(',', startPos);
        if( commaPos>=0 && commaPos < endPos ) {
            endPos = commaPos;
        }

        out[0] = str.substring(startPos, endPos);

        if( endPos < str.length() ) {
            return str.substring(endPos);
        } else {
            return null;
        }
    }


    // as of Aug 2021, EvidenceCode.getEcoId() is not thread safe and it was causing problems
    //  therefore we synchronise calls to it explicitly
    synchronized String getEcoId(String evidenceCode) {

        try {
            return EvidenceCode.getEcoId(evidenceCode);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * sort data list by ['objectName','DOid']
     */
    public void sort() {
        Collections.sort(data, new Comparator<DafData>() {
            @Override
            public int compare(DafData o1, DafData o2) {
                int r = Utils.stringsCompareToIgnoreCase(o1.objectName, o2.objectName);
                if( r!=0 ) {
                    return r;
                }
                return o1.DOid.compareTo(o2.DOid);
            }
        });
    }

    void handlePublication(String dbRefStr, int refRgdId, DafPublication dafPublication) {

        // look for a PMID
        String[] dbRefs = dbRefStr.split("[\\|]");
        for( String ref : dbRefs ) {
            if (ref.startsWith("PMID:")) {
                if( dafPublication.publicationId==null ) {
                    dafPublication.publicationId = ref;

                    if( refRgdId>0 ) {
                        dafPublication.crossReference = new HashMap<>();
                        dafPublication.crossReference.put("id", "RGD:" + refRgdId);
                        List<String> pages = new ArrayList<>();
                        pages.add("reference");
                        dafPublication.crossReference.put("pages", pages);
                    }
                    return;
                } else {
                    System.out.println("*** WARN *** multiple PMIDs for this reference");
                }
            }
        }

        // no PMID available -- set reference to REF_RGD_ID
        for( String ref : dbRefs ) {
            if( ref.isEmpty() && refRgdId>0 ) {
                dafPublication.publicationId = "RGD:" + refRgdId;

                dafPublication.crossReference = new HashMap<>();
                dafPublication.crossReference.put("id", "RGD:" + refRgdId);
                List<String> pages = new ArrayList<>();
                pages.add("reference");
                dafPublication.crossReference.put("pages", pages);
            } else {
                System.out.println("*** WARN *** unexpected reference type: "+ref);
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

    synchronized String getDateAssigned(String dt) {
        String result;
        try {
            Date dateAssigned = sdt.parse(dt);
            result = sdf_agr.format(dateAssigned);
        } catch( ParseException e ) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
