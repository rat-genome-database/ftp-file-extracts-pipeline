package edu.mcw.rgd;

import edu.mcw.rgd.dao.AbstractDAO;
import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.dao.spring.GeneQuery;
import edu.mcw.rgd.dao.spring.IntStringMapQuery;
import edu.mcw.rgd.dao.spring.StringListQuery;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.datamodel.ontologyx.Ontology;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.datamodel.ontologyx.TermSynonym;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.springframework.jdbc.core.*;

import java.sql.*;
import java.util.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mtutaj
 * @since Oct 11, 2010
 */
public class FtpFileExtractsDAO extends AbstractDAO {

    private AliasDAO aliasDAO = new AliasDAO();
    private AnnotationDAO annotationDAO = new AnnotationDAO();
    private AssociationDAO associationDAO = new AssociationDAO();
    private CellLineDAO cellLineDAO = new CellLineDAO();
    private GeneDAO geneDAO = associationDAO.getGeneDAO();
    private GenomicElementDAO genomicElementDAO = new GenomicElementDAO();
    private MapDAO mapDAO = new MapDAO();
    private NomenclatureDAO nomenDAO = new NomenclatureDAO();
    private NotesDAO notesDAO = new NotesDAO();
    private OmimDAO omimDAO = new OmimDAO();
    private OntologyXDAO ontologyDAO = new OntologyXDAO();
    private QTLDAO qtlDAO = associationDAO.getQtlDAO();
    private ReferenceDAO refDAO = associationDAO.getReferenceDAO();
    private RGDManagementDAO rgdIdDAO = new RGDManagementDAO();
    private SSLPDAO markerDAO = associationDAO.getSslpDAO();
    private TranscriptDAO tdao = new TranscriptDAO();
    private XdbIdDAO xdbIdDAO = new XdbIdDAO();

    /**
     * return list of all active genes for given species
     *
     * @param speciesType
     * @return
     * @throws Exception
     */
    List<Gene> getActiveGenes(int speciesType) throws Exception {
        return geneDAO.getActiveGenes(speciesType);
    }

    /**
     * return map positions for current rgd id
     *
     * @param rgdId gene rgd id
     * @return list of map positions
     * @throws Exception
     */
    List<MapData> getMapData(int rgdId) throws Exception {
        return mapDAO.getMapData(rgdId);
    }

    /**
     * return map positions for current rgd id and map key
     *
     * @param rgdId  rgd id
     * @param mapKey map key
     * @return list of map positions
     * @throws Exception
     */
    List<MapData> getMapData(int rgdId, int mapKey) throws Exception {
        return mapDAO.getMapData(rgdId, mapKey);
    }

    /**
     * given list of all map positions for given marker, return the chromosome this SSLP is located; chromosome
     * on the current primary reference assembly takes preference; also return
     * fishband from cytogenetic map, if available
     *
     * @param mdList             list of MapData objects
     * @param primaryRefAssembly map key of primary ref assembly
     * @return array containing chromosome and fishband, if available
     * @throws Exception
     */
    public String[] getChromosomeAndFishBand(List<MapData> mdList, int primaryRefAssembly) throws Exception {

        // get list of all genomic positions
        String chr = null;
        // if there is a valid chromosome for reference assembly, use it!
        for (MapData md : mdList) {
            if (md.getMapKey() == primaryRefAssembly && md.getChromosome() != null) {
                if (md.getChromosome().trim().length() > 0) {
                    chr = md.getChromosome().trim();
                    break;
                }
            }
        }

        // second iteration: extract fish band or any valid chromosome
        String fishBand = null;
        for (MapData md : mdList) {
            if (chr == null && md.getChromosome() != null) {
                if (md.getChromosome().trim().length() > 0) {
                    chr = md.getChromosome().trim();
                }
            }

            if (fishBand == null && md.getFishBand() != null) {
                if (md.getFishBand().trim().length() > 0) {
                    fishBand = md.getFishBand().trim();
                }
            }
        }

        return new String[]{chr, fishBand};
    }

    public Map<Integer,String> loadPmidMap() throws Exception {
        List<IntStringMapQuery.MapPair> pmidList = refDAO.getPubmedIdsAndRefRgdIds();
        Map<Integer,String> pmidMap = new HashMap<>(pmidList.size());
        for (IntStringMapQuery.MapPair pair : pmidList) {
            String pmid = pmidMap.put(pair.keyValue, pair.stringValue);
            if( pmid != null ) {
                System.out.println("multiple PMIDs for REF_RGD_ID:"+pair.keyValue+", PMID:"+pmid);
            }
        }
        return pmidMap;
    }

    public List<Reference> getActiveReferences() throws Exception {
        return refDAO.getActiveReferences();
    }

    public List<Author> getAuthors(int refKey) throws Exception {
        return refDAO.getAuthors(refKey);
    }

    /**
     * return comma separated string of reference rgds in format "123;456;678"
     *
     * @param rgdId gene rgd id
     * @return string with list of reference rgd ids
     * @throws Exception
     */
    String getCuratedRefs(int rgdId) throws Exception {
        List<Integer> refs = refDAO.getReferenceRgdIdsForObject(rgdId);
        return Utils.concatenate(refs, ";");
    }

    /**
     * return delimiter separated string of reference curated pubmed ids in format "123;456;678"
     *
     * @param rgdId gene rgd id
     * @return string with list of  ids
     * @throws Exception
     */
    String getCuratedPubmedIds(int rgdId) throws Exception {

        List<XdbId> pubMedIds = xdbIdDAO.getCuratedPubmedIds(rgdId);
        return Utils.concatenate(";", pubMedIds, "getAccId");
    }

    /**
     * return comma separated string of reference uncurated pubmed ids in format "123,456,678"
     *
     * @param rgdId gene rgd id
     * @return string with list of  ids
     * @throws Exception
     */
    String getUncuratedPubmedIds(int rgdId) throws Exception {
        // PUBMED ids that do not have curated references in RGD
        List<XdbId> pubMedIds = xdbIdDAO.getUncuratedPubmedIds(rgdId);
        return Utils.concatenate(";", pubMedIds, "getAccId");
    }

    /**
     * return curated pubmed ids
     *
     * @param objectKey      object key for which we return the curated pubmed ids
     * @param speciesTypeKey species type key
     * @return map of objects: rgd id mapped to set of accession ids
     * @throws Exception
     */
    Map<Integer, Set<String>> getCuratedPubmedIds(int objectKey, int speciesTypeKey) throws Exception {
        return xdbIdDAO.getCuratedPubmedIds(objectKey, speciesTypeKey);
    }

    /**
     * return all external database links for given rgd and xdb key
     *
     * @param rgdId  object rgd id
     * @param xdbKey xdb key
     * @return list of XdbId objects
     * @throws Exception
     */
    List<XdbId> getXdbIds(int rgdId, int xdbKey) throws Exception {
        return xdbIdDAO.getXdbIdsByRgdId(xdbKey, rgdId);
    }

    /**
     * get all external database links for given rgd
     *
     * @param rgdId object rgd id
     * @return list of XdbId objects
     * @throws Exception
     */
    List<XdbId> getXdbIds(int rgdId) throws Exception {
        XdbId filter = new XdbId();
        filter.setRgdId(rgdId);
        return xdbIdDAO.getXdbIds(filter);
    }

    /**
     * return external ids for given xdb key and rgd-id
     *
     * @param xdbKeys - list of external database keys (like 2 for PubMed)
     * @param rgdId   - rgd-id
     * @return list of external ids
     */
    public List<XdbId> getXdbIdsByRgdId(List xdbKeys, int rgdId) throws Exception {
        return xdbIdDAO.getXdbIdsByRgdId(xdbKeys, rgdId);
    }

    public List<XdbId> getActiveXdbIds(int xdbKey, int objectKey) throws Exception {
        return xdbIdDAO.getActiveXdbIds(xdbKey, objectKey);
    }

    /**
     * get list of gene splices
     *
     * @param geneKey gene key
     * @return list of all gene splices for given gene
     * @throws Exception
     */
    List<Gene> getSplices(int geneKey) throws Exception {
        return geneDAO.getVariantsForGene(geneKey, "splice");
    }

    public List<Gene> getGenesForAllele(int alleleRgdId) throws Exception {
        Gene gene = geneDAO.getGene(alleleRgdId);
        if (gene.getType() == null || !gene.getType().equals("allele")) {
            return null;
        }
        return geneDAO.getGeneFromVariant(alleleRgdId);
    }

    /**
     * get list of gene sslps
     *
     * @param geneKey gene key
     * @return list of all sslps for given gene
     * @throws Exception
     */
    List<SSLP> getMarkers(int geneKey) throws Exception {
        return markerDAO.getSSLPsForGene(geneKey);
    }

    /**
     * Returns all active genes for given species and gene symbol. Results do not contain splices or alleles
     *
     * @param speciesKey species type key
     * @param symbol     gene symbol
     * @return list of all active genes
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<Gene> getActiveGenes(int speciesKey, String symbol) throws Exception {

        String cacheKey = speciesKey + "|" + symbol;
        List<Gene> genes = _cacheActiveGenes.get(cacheKey);
        if (genes != null)
            return genes;
        genes = geneDAO.getActiveGenes(speciesKey, symbol);
        _cacheActiveGenes.put(cacheKey, genes);
        return genes;
    }

    private Map<String, List<Gene>> _cacheActiveGenes = new HashMap<>();

    /**
     * get a gene object given marker key
     *
     * @param markerKey marker key
     * @return gene object given marker key or null if no association
     * @throws Exception when wrong things happen in spring framework
     */
    public Gene getGeneByMarkerKey(int markerKey) throws Exception {
        String sql = "select g.*,0 species_type_key from GENES g where GENE_KEY IN (SELECT GENE_KEY FROM RGD_GENE_SSLP WHERE SSLP_KEY=?)";
        List<Gene> genes = GeneQuery.execute(geneDAO, sql, markerKey);
        return genes == null || genes.isEmpty() ? null : genes.get(0);
    }

    /**
     * get list of active markers for given species
     *
     * @param speciesType species type
     * @return list of active SSLPs for given species
     * @throws Exception when wrong things happen in spring framework
     */
    public List<SSLP> getActiveMarkers(int speciesType) throws Exception {
        return markerDAO.getActiveSSLPs(speciesType);
    }

    /**
     * return list of aliases
     *
     * @param rgdId rgd id
     * @return List of Alias objects
     * @throws Exception when something bad in spring framework happens
     */
    List<Alias> getAliases(int rgdId) throws Exception {
        return aliasDAO.getAliases(rgdId);
    }

    /**
     * get list of all active array id aliases from Ensembl for given object type and species
     *
     * @param objType        object type; one of RgdId.OBJECT_KEY constants
     * @param speciesTypeKey species type key
     * @throws Exception if something wrong happens in spring framework
     */
    public List<Alias> getActiveArrayIdAliasesFromEnsembl(int objType, int speciesTypeKey) throws Exception {
        return aliasDAO.getActiveArrayIdAliasesFromEnsembl(objType, speciesTypeKey);
    }

    List<QTL> getQtlsForGene(int geneKey) throws Exception {
        return associationDAO.getQtlAssociationsByGene(geneKey);
    }

    List<Gene> getGeneAssociationsByQtl(int rgdId) throws Exception {
        return associationDAO.getGeneAssociationsByQTL(rgdId);
    }

    List<NomenclatureEvent> getNomenEvents(int rgdId) throws Exception {
        return nomenDAO.getNomenclatureEvents(rgdId);
    }

    List<Strain> getActiveStrains() throws Exception {
        return new StrainDAO().getActiveStrains();
    }

    List<Strain> getStrainsAssociatedWithQtl(int qtlRgdId) throws Exception {
        return associationDAO.getStrainAssociationsForQTL(qtlRgdId);
    }

    List<Gene> getStrainsAlleles(int strainRgdId) throws Exception {
        return associationDAO.getStrainAssociations(strainRgdId, RgdId.OBJECT_KEY_GENES);
    }

    /**
     * get active qtls for given species
     *
     * @param speciesType species type
     * @return list of active qtls for given species
     * @throws Exception if something wrong happens in spring framework
     */
    public List<QTL> getActiveQtls(int speciesType) throws Exception {
        return qtlDAO.getActiveQTLs(speciesType);
    }

    public RgdId getRgdId(int rgdId) throws Exception {
        return rgdIdDAO.getRgdId2(rgdId);
    }

    /**
     * get marker symbol; since marker could be marker, gene or genomic element,
     * we first examine the rgd object type, and then retrieve the symbol from corresponding table
     *
     * @param markerRgdId marker rgd id
     * @return symbol name if available
     * @throws Exception if something wrong happens in spring framework
     */
    public String getSymbolForMarker(int markerRgdId) throws Exception {

        // get object type from rgd_ids table
        RgdId rgdId = getRgdId(markerRgdId);
        if (rgdId == null)
            return null;

        switch (rgdId.getObjectKey()) {
            case 1: // GENES
                return getSymbolForGene(rgdId.getRgdId());

            case 3: // MARKERS
                SSLP marker = markerDAO.getSSLP(rgdId.getRgdId());
                return marker != null ? marker.getName() : null;

            default:
                GenomicElement ge = genomicElementDAO.getElement(rgdId.getRgdId());
                if (ge == null)
                    return null;
                if (ge.getSymbol() != null)
                    return ge.getSymbol();
                return ge.getName();
        }
    }

    String getSymbolForGene(int geneRgdId) throws Exception {
        Gene gene = geneDAO.getGene(geneRgdId);
        return gene != null ? gene.getSymbol() : null;
    }

    /**
     * return an array of qtls related to given qtl
     *
     * @param qtlKey qtl key to be searched for
     * @return string with concatenated list of symbols of related qtls and their relation types: "qtl1 (reltype1);qtl2 (reltype2)"
     * @throws Exception when something wrong happens in spring framework
     */
    String getRelatedQtls(int qtlKey) throws Exception {

        String sql = "SELECT q.qtl_symbol,z.qtl_rel_desc FROM qtls q, (" +
                "SELECT rq.qtl_key1 qtl_key, rt.qtl_rel_desc FROM related_qtls rq, qtl_rel_types rt" +
                "  WHERE rq.qtl_key2=? AND rq.qtl_rel_type_key=rt.qtl_rel_type_key " +
                "UNION " +
                "SELECT rq.qtl_key2 qtl_key, rt.qtl_rel_desc FROM related_qtls rq, qtl_rel_types rt" +
                "  WHERE rq.qtl_key1=? AND rq.qtl_rel_type_key=rt.qtl_rel_type_key " +
                ") z " +
                "WHERE q.qtl_key=z.qtl_key " +
                "ORDER BY qtl_symbol";

        final StringBuffer relQtls = new StringBuffer();
        JdbcTemplate jt = new JdbcTemplate(this.getDataSource());
        jt.query(sql, new Object[]{qtlKey, qtlKey}, new int[]{Types.INTEGER, Types.INTEGER}, new RowMapper() {
            String prevQtl = "";

            public Object mapRow(ResultSet rs, int i) throws SQLException {
                String qtlSymbol = rs.getString(1);
                String qtlRelType = rs.getString(2);
                if (prevQtl.equals(qtlSymbol)) {
                    // just another qtl rel type
                    relQtls.append(',').append(qtlRelType);
                } else {
                    // new qtl symbol
                    if (relQtls.length() > 0)
                        relQtls.append(");"); // move to next qtl
                    relQtls.append(qtlSymbol).append(" (").append(qtlRelType);
                    prevQtl = qtlSymbol;
                }
                return null;
            }
        });

        // append terminating ')' character
        if (relQtls.length() > 0)
            relQtls.append(')');
        return relQtls.toString();
    }

    /**
     * get all annotations for given rgd id
     *
     * @param rgdId annotated object rgd id
     * @return List of Annotation objects
     * @throws Exception
     */
    List<Annotation> getAnnotations(int rgdId) throws Exception {
        return annotationDAO.getAnnotations(rgdId);
    }

    /**
     * get all notes for given rgd id
     *
     * @param rgdId notes object rgd id
     * @return List of Note objects
     * @throws Exception
     */
    List<Note> getNotes(int rgdId) throws Exception {
        return notesDAO.getNotes(rgdId);
    }

    /**
     * get list of all marker alleles for every active marker of given species
     *
     * @param speciesType species type key
     * @param strainNames set of all strain names
     * @return List of SslpAlleles objects
     * @throws Exception
     */
    List<SslpAlleles> getSslpAlleles(int speciesType, final Set<String> strainNames) throws Exception {

        String sql = "SELECT s.RGD_ID, s.RGD_NAME, a.SIZE1, t.STRAIN_SYMBOL " +
                "FROM sslps s, rgd_ids r, sslps_alleles a, strains t " +
                "WHERE s.RGD_ID=r.RGD_ID AND r.SPECIES_TYPE_KEY=? AND r.OBJECT_STATUS='ACTIVE' AND s.SSLP_KEY=a.SSLP_KEY AND a.STRAIN_KEY=t.STRAIN_KEY " +
                "ORDER BY s.RGD_NAME";
        JdbcTemplate jt = new JdbcTemplate(this.getDataSource());
        final List<SslpAlleles> list = new LinkedList<>();
        jt.query(sql, new Object[]{speciesType}, new int[]{Types.INTEGER}, new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
                // extract data from result set
                int markerRgdId = rs.getInt(1);
                String markerName = rs.getString(2);
                int alleleSize = rs.getInt(3);
                String strainSymbol = rs.getString(4);
                strainNames.add(strainSymbol);

                // is the marker already in the list?
                SslpAlleles rec = null;
                if (!list.isEmpty()) {
                    rec = list.get(list.size() - 1);
                    if (rec.markerRgdId != markerRgdId)
                        rec = null; // different marker
                }
                // create new SslpAlleles object if necessary
                if (rec == null) {
                    rec = new SslpAlleles();
                    rec.markerRgdId = markerRgdId;
                    rec.markerName = markerName;
                    list.add(rec);
                }
                // update the allele size and strain symbol
                rec.strainNamesToAlleleSizes.put(strainSymbol, alleleSize);
            }
        });
        return list;
    }

    /**
     * get all orthologs where SRC_RGD_ID is given species
     *
     * @param speciesTypeKey species type key
     * @return List of Ortholog objects
     * @throws Exception
     */
    List<Ortholog> getOrthologs(int speciesTypeKey) throws Exception {
        OrthologDAO dao = new OrthologDAO();
        return dao.getAllOrthologs(speciesTypeKey);
    }

    List<Ortholog> getOrthologs(int speciesTypeKey1, int speciesTypeKey2) throws Exception {
        OrthologDAO dao = new OrthologDAO();
        return dao.getAllOrthologs(speciesTypeKey1, speciesTypeKey2);
    }

    /**
     * get ontology object given ontology id
     *
     * @param ontId ontology id
     * @return ontology object
     * @throws Exception
     */
    public synchronized Ontology getOntology(String ontId) throws Exception {

        // retrieve Ontology object from cache
        Ontology ont = _ontologyCache.get(ontId);
        if (ont == null) {
            // object not in cache -- load from database
            ont = ontologyDAO.getOntology(ontId);
            _ontologyCache.put(ontId, ont);
        }
        return ont;
    }

    private Map<String, Ontology> _ontologyCache = new HashMap<String, Ontology>();

    /**
     * get Term object given term acc id
     *
     * @param termAcc term accession id
     * @return Term object
     * @throws Exception
     */
    public synchronized Term getTermByAccId(String termAcc) throws Exception {

        // retrieve term object from cache
        Term term = _termCache.get(termAcc);
        if (term == null) {
            // term not in cache -- read it from database
            term = ontologyDAO.getTermByAccId(termAcc);
            _termCache.put(termAcc, term);
        }
        return term;
    }

    private Map<String, Term> _termCache = new HashMap<>();


    public List<TermSynonym> getTermSynonyms(String termAcc) throws Exception {
        List<TermSynonym> synonyms = term2synonymsCache.get(termAcc);
        if (synonyms == null) {
            synonyms = ontologyDAO.getTermSynonyms(termAcc);
            term2synonymsCache.put(termAcc, synonyms);
        }
        return synonyms;
    }

    private static Map<String, List<TermSynonym>> term2synonymsCache = new ConcurrentHashMap<>();

    /**
     * Check if a term can be used for curation.
     *
     * @param termAcc term ACC id
     * @return true if the term doesn't have a "Not4Curation" synonym
     * @throws Exception if something wrong happens in spring framework
     */
    public boolean isForCuration(String termAcc) throws Exception {
        Boolean isForCuration;
        synchronized (_isForCurationMap) {
            isForCuration = _isForCurationMap.get(termAcc);
            if (isForCuration == null) {
                isForCuration = ontologyDAO.isForCuration(termAcc);
                _isForCurationMap.put(termAcc, isForCuration);
            }
        }
        return isForCuration;
    }

    static final Map<String, Boolean> _isForCurationMap = new HashMap<>();

    public String getOmimPSTermAccForChildTerm(String childTermAcc, CounterPool counters) throws Exception {
        String sql = "SELECT term_acc FROM ont_synonyms WHERE synonym_name IN\n" +
            "(SELECT phenotypic_series_number omim_ps FROM omim_phenotypic_series WHERE phenotype_mim_number IN\n"+
            " (SELECT synonym_name FROM ont_synonyms WHERE term_acc=? AND synonym_name like 'OMIM:______')"+
            ")";
        List<String> termAccIds = StringListQuery.execute(ontologyDAO, sql, childTermAcc);
        if( termAccIds.isEmpty() ) {
            return null;
        }
        if( termAccIds.size()>1 ) {
            counters.increment("OMIM:PS problem: multiple OMIM:PS parents for child term "+childTermAcc+": "+Utils.concatenate(termAccIds,","));
            return null;
        }
        return termAccIds.get(0);
    }

    public List<Annotation> getAnnotationsBySpecies(int speciesType) throws Exception {
        return annotationDAO.getAnnotationsBySpecies(speciesType);
    }

    public List<Annotation> getAnnotationsBySpecies(int speciesType, int objectKey) throws Exception {
        return annotationDAO.getAnnotationsBySpecies(speciesType, objectKey);
    }

    public List<Annotation> getAnnotationsBySpecies(int speciesTypeKey, String aspect, String source) throws Exception {
        return annotationDAO.getAnnotationsBySpeciesAspectAndSource(speciesTypeKey, aspect, source);
    }

    public List<Transcript> getTranscriptsForGene(int geneRgdId, int mapKey) throws Exception {
        return tdao.getTranscriptsForGene(geneRgdId, mapKey);
    }

    public List<TranscriptFeature> getFeatures(int transcriptRgdId, int mapKey) throws Exception {
        return tdao.getFeatures(transcriptRgdId, mapKey);
    }

    public Omim getOmimByNr(String mimNr) throws Exception {
        return omimDAO.getOmimByNr(mimNr);
    }

    public List<CellLine> getActiveCellLines() throws Exception {
        return cellLineDAO.getActiveCellLines();
    }

    public List<ObsoleteId> getObsoleteIdsForGenes() throws Exception {

        String sql = "SELECT DISTINCT s.common_name Species, g.rgd_id old_gene_rgd_id, g.gene_symbol old_gene_symbol,\n"+
            "r.object_status old_gene_status, g.gene_type_lc old_gene_type, new_rgd_id new_gene_rgd_id,\n"+
            "gh.gene_symbol new_gene_symbol, gh.gene_type_lc new_gene_type, r2.object_status new_gene_status\n" +
            "FROM genes g,rgd_ids r,species_types s,rgd_id_history h,genes gh,rgd_ids r2\n" +
            "WHERE r.rgd_id=g.rgd_id AND r.object_status<>'ACTIVE' AND s.species_type_key=r.species_type_key\n" +
            "AND g.rgd_id=h.old_rgd_id(+) AND h.new_rgd_id=gh.rgd_id(+)\n" +
            "AND h.new_rgd_id=r2.rgd_id(+)\n" +
            "AND NVL(g.gene_type_lc,'?') NOT IN('splice','allele')\n" +
            "ORDER BY g.rgd_id";

        JdbcTemplate jt = new JdbcTemplate(this.getDataSource());
        return jt.query(sql, new RowMapper() {
            public Object mapRow(ResultSet rs, int i) throws SQLException {
                ObsoleteId row = new ObsoleteId();
                row.species = rs.getString(1).toLowerCase();
                row.oldGeneRgdId = rs.getInt(2);
                row.oldGeneSymbol = rs.getString(3);
                row.oldGeneStatus = rs.getString(4);
                row.oldGeneType = rs.getString(5);
                row.newGeneRgdId = rs.getInt(6);
                row.newGeneSymbol = rs.getString(7);
                row.newGeneType = rs.getString(8);
                row.newGeneStatus = rs.getString(9);
                return row;
            }
        });
    }

    public List<String[]> strainToCmo() throws Exception {
        String sql =
            "SELECT DISTINCT SUBSTR(oy.synonym_name,9), os.term_acc, os.term, oc.term_acc, oc.term "+
            "FROM experiment_record er, clinical_measurement cm, ont_terms oc "+
            " ,sample s, ont_synonyms oy, ont_terms os "+
            "WHERE cm.clinical_measurement_id=er.clinical_measurement_id AND oc.term_acc=cm.clinical_measurement_ont_id "+
            " AND s.sample_id=er.sample_id AND oy.term_acc=s.strain_ont_id AND os.term_acc=s.strain_ont_id "+
            " AND er.curation_status=40 AND oy.synonym_name LIKE 'RGD_ID:%' "+
            "ORDER BY os.term, oc.term";

        String[] colNames = new String[]{"STRAIN_RGD_ID","STRAIN_ONT_ID","STRAIN_SYMBOL","CLINICAL_MEASUREMENT_ONT_ID","CMO_TERM"};
        return getRows(sql, colNames);
    }

    public List<String[]> strainToMmo() throws Exception {
        String sql =
            "SELECT DISTINCT SUBSTR(oy.synonym_name,9), os.term_acc, os.term, oc.term_acc, oc.term "+
            "FROM experiment_record er, measurement_method mm, ont_terms oc "+
            " ,sample s, ont_synonyms oy, ont_terms os "+
            "WHERE mm.measurement_method_id=er.measurement_method_id AND oc.term_acc=mm.measurement_method_ont_id "+
            " AND s.sample_id=er.sample_id AND oy.term_acc=s.strain_ont_id AND os.term_acc=s.strain_ont_id "+
            " AND er.curation_status=40 AND oy.synonym_name LIKE 'RGD_ID:%' "+
            "ORDER BY os.term, oc.term";

        String[] colNames = new String[]{"STRAIN_RGD_ID","STRAIN_ONT_ID","STRAIN_SYMBOL","MEASUREMENT_METHOD_ONT_ID","MMO_TERM"};
        return getRows(sql, colNames);
    }

    public List<String[]> strainToXco() throws Exception {
        String sql =
            "SELECT DISTINCT SUBSTR(oy.synonym_name,9), os.term_acc, os.term, oc.term_acc, oc.term "+
            " ,ec.exp_cond_assoc_value_min, ec.exp_cond_assoc_value_max, ec.exp_cond_assoc_units "+
            "FROM experiment_record er, experiment_condition ec, ont_terms oc ,sample s, ont_synonyms oy, ont_terms os "+
            "WHERE ec.experiment_record_id=er.experiment_record_id AND oc.term_acc=ec.exp_cond_ont_id "+
            " AND s.sample_id=er.sample_id AND oy.term_acc=s.strain_ont_id AND os.term_acc=s.strain_ont_id "+
            " AND er.curation_status=40 AND oy.synonym_name LIKE 'RGD_ID:%' "+
            "ORDER BY os.term, oc.term";

        String[] colNames = new String[]{"STRAIN_RGD_ID","STRAIN_ONT_ID","STRAIN_SYMBOL","EXPERIMENT_COND_ONT_ID","XCO_TERM","EXP_COND_VALUE_MIN","EXP_COND_VALUE_MAX","EXP_COND_UNITS"};
        return getRows(sql, colNames);
    }

    public List<String[]> allelesToGenes() throws Exception {
        String sql="SELECT DISTINCT\n" +
                "    a.rgd_id AS allele_rgdid,\n" +
                "    a.gene_symbol AS allele_symbol,\n" +
                "    a.full_name AS allele_name,\n" +
                "    a.gene_desc AS allele_description,\n" +
                "    TO_CHAR(r.CREATED_DATE,'YYYY-MM-DD') as allele_created,\n" +
                "    TO_CHAR(r.LAST_MODIFIED_DATE,'YYYY-MM-DD') as allele_last_modified,\n" +
                "    g.RGD_ID as parent_gene_rgdid,\n" +
                "    g.GENE_SYMBOL as parent_gene_symbol,\n" +
                "    DECODE(md.MAP_KEY,null,'',60,'RGSC 3.4',70,'Rnor_5.0',360,'Rnor_6.0','?') as map_name,\n" +
                "    md.CHROMOSOME as gene_chr,\n" +
                "    md.START_POS as gene_start,\n" +
                "    md.STOP_POS as gene_stop,\n" +
                "    md.STRAND as gene_strand,\n" +
                "    NULL AS allele_synonyms\n" +
                "from GENES a,RGD_IDS r,GENES_VARIATIONS gv, GENES g, MAPS_DATA md  \n" +
                "where a.GENE_TYPE_LC='allele' and r.RGD_ID=a.RGD_ID and gv.VARIATION_KEY=a.GENE_KEY\n" +
                " AND r.object_status='ACTIVE' AND r.species_type_key=3\n" +
                " AND g.gene_key=gv.gene_key AND md.rgd_id(+)=g.rgd_id AND md.map_key(+) IN(60,70,360)\n" +
                "ORDER BY parent_gene_symbol, allele_symbol, map_name";

        String[] colNames = new String[]{"ALLELE_RGDID","ALLELE_SYMBOL","ALLELE_NAME","ALLELE_DESCRIPTION","ALLELE_CREATED",
                "ALLELE_LAST_MODIFIED","PARENT_GENE_RGDID","PARENT_GENE_SYMBOL","MAP_NAME","GENE_CHR","GENE_START","GENE_STOP",
                "GENE_STRAND","ALLELE_SYNONYMS"};
        List<String[]> rows = getRows(sql, colNames);

        // add allele synonyms if available
        for(String[] row: rows) {
            if( row[0].startsWith("ALLELE") )
                continue;

            int alleleRgdId = Integer.parseInt(row[0]);
            List<Alias> aliases = getAliases(alleleRgdId);
            if( !aliases.isEmpty() ) {
                row[row.length-1] = Utils.concatenate(";",aliases,"getValue");
            }
        }
        return rows;
    }

    public List<String[]> splicesToGenes() throws Exception {
        String sql="SELECT DISTINCT\n" +
                "    a.rgd_id AS splice_rgdid,\n" +
                "    a.gene_symbol AS splice_symbol,\n" +
                "    a.full_name AS splice_name,\n" +
                "    a.gene_desc AS splice_description,\n" +
                "    TO_CHAR(r.created_date,'YYYY-MM-DD') AS splice_created,\n" +
                "    TO_CHAR(r.LAST_MODIFIED_DATE,'YYYY-MM-DD') AS splice_last_modified,\n" +
                "    g.RGD_ID as parent_gene_rgdid,\n" +
                "    g.GENE_SYMBOL as parent_gene_symbol,\n" +
                "    DECODE(md.map_key,null,'',60,'RGSC 3.4',70,'Rnor_5.0',360,'Rnor_6.0','?') AS map_name,\n" +
                "    md.CHROMOSOME as gene_chr,\n" +
                "    md.START_POS as gene_start,\n" +
                "    md.STOP_POS as gene_stop,\n" +
                "    md.strand AS gene_strand,\n" +
                "    NULL AS splice_synonyms\n" +
                "FROM genes a,rgd_ids r,genes_variations gv, genes g, maps_data md \n" +
                "WHERE a.gene_type_lc='splice' AND r.rgd_id=a.rgd_id AND gv.variation_key=a.gene_key\n" +
                " AND r.object_status='ACTIVE' AND r.species_type_key=3\n" +
                " AND g.gene_key=gv.gene_key AND md.rgd_id(+)=g.rgd_id AND md.map_key(+) IN(60,70,360)\n" +
                "ORDER BY parent_gene_symbol, splice_symbol, map_name";

        String[] colNames = new String[]{"SPLICE_RGDID","SPLICE_SYMBOL","SPLICE_NAME","SPLICE_DESCRIPTION","SPLICE_CREATED",
                "SPLICE_LAST_MODIFIED","PARENT_GENE_RGDID","PARENT_GENE_SYMBOL","MAP_NAME","GENE_CHR","GENE_START","GENE_STOP",
                "GENE_STRAND","SPLICE_SYNONYMS"};
        List<String[]> rows = getRows(sql, colNames);

        // add splice synonyms if available
        for(String[] row: rows) {
            if( row[0].startsWith("SPLICE") ) // skip header line
                continue;

            int spliceRgdId = Integer.parseInt(row[0]);
            List<Alias> aliases = getAliases(spliceRgdId);
            if( !aliases.isEmpty() ) {
                row[row.length-1] = Utils.concatenate(";",aliases,"getValue");
            }
        }
        return rows;
    }

    protected List<String[]> getRows(String sql, String[] colNames) throws Exception {
        List<String[]> results = new LinkedList<>();
        results.add(colNames);

        try (Connection conn = this.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String[] row = new String[colNames.length];
                for (int i = 0; i < row.length; i++) {
                    row[i] = rs.getString(i + 1);
                }
                results.add(row);
            }
        }

        return results;
    }
}

// represents marker alleles for one marker
class SslpAlleles {
    public int markerRgdId;
    public String markerName;
    public HashMap<String, Integer> strainNamesToAlleleSizes = new HashMap<>(73);
}

class ObsoleteId {
    public String species;

    public int oldGeneRgdId;
    public String oldGeneSymbol;
    public String oldGeneStatus;
    public String oldGeneType;

    public int newGeneRgdId; // 0 means 'null'
    public String newGeneSymbol;
    public String newGeneStatus;
    public String newGeneType;
}