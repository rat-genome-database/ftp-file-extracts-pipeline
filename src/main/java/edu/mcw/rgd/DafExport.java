package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.EvidenceCode;
import edu.mcw.rgd.datamodel.ontology.DafAnnotation;

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
                release = "RGD Daf Extractor, AGR schema 1.0.1.4, build  Aug 27, 2021";

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

    public DafData addData(DafAnnotation a, int refRgdId) {

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
            if( q.equals("not") || q.equals("no_association") ) {
                data.negation = "not";
            }
        }

        if( a.getWithInfo()!=null ) {
            data.with = new ArrayList<>();
            for( String with: a.getWithInfo().split("[\\|\\,\\ ]+") ) {
                data.with.add(with);
            }
        }

        // handle experimental conditions -- for strains
        // handle annotation with a single condition only
        if( a.getDbObjectType().equals("strain") && data.with!=null && data.with.size()==1 ) {
            AgrExperimentalConditionMapper.Info info = AgrExperimentalConditionMapper.getInstance().getInfo(data.with.get(0));
            if( info!=null ) {

                // only a subset of qualifiers is allowed
                String condRelType = null;
                if( a.getQualifier()==null ) {
                    condRelType = "has_condition";
                } else if( a.getQualifier().contains("induced") || a.getQualifier().contains("induces") ) {
                    condRelType = "induces";
                } else if( a.getQualifier().contains("treatment") || a.getQualifier().contains("ameliorates") ) {
                    condRelType = "ameliorates";
                } else if( a.getQualifier().contains("exacerbates") ) {
                    condRelType = "exacerbates";
                }

                if( condRelType!=null ) {

                    DafConditionRelation condRel = new DafConditionRelation();
                    condRel.conditionRelationType = condRelType;

                    HashMap h = new HashMap();
                    h.put("conditionClassId", info.zecoAcc);
                    h.put("conditionId", info.xcoAcc);
                    h.put("conditionStatement", info.conditionStatement);
                    condRel.conditions = new ArrayList();
                    condRel.conditions.add(h);

                    data.conditionRelations = new ArrayList<>();
                    data.conditionRelations.add(condRel);
                }
            }
        }

        if( data.evidence.publication.publicationId==null ) {
            System.out.println("annot skipped because publicationRef is empty");
            return null;
        } else {
            this.data.add(data);
        }

        return data;
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
                int r = o1.objectName.compareToIgnoreCase(o2.objectName);
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
