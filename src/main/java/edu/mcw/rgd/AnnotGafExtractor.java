package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;


/**
 * @author mtutaj
 * @since June 28, 2011
 * Extracts annotated rgd objects by ontology in GAF 2.2 format.
 * The annotated objects being exported are genes, qtls and strains.
 * <p>
 * There is also an option to extract human GO annotations for AGR. Details specific to AGR extract:
 * <ol>
 * <li>The file is named differently and it is generated in AGR directory</li>
 * <li>HGNC ids are used as ObjectIDs (instead of RGD ids)</li>
 * <li>REFERENCES field is set to XREF_SOURCE (instead of REF_RGD_ID merged with PMIDs extracted from WITH_INFO and RGD_ACC_XDB</li>
 * <li>lines with DATA_SRC='RGD' are skipped from the extract</li>
 * </ol>
 */
public class AnnotGafExtractor extends AnnotBaseExtractor {
    final String HEADER_COMMON_LINES =
        "!gaf-version: 2.2\n"+
        "!{ As of December 2016, the gene_association.rgd file only contains 'RGD' in column 1 and RGD gene identifiers in column 2. }\n"+
        "!{ The gene_protein_association.rgd file (available on the RGD ftp site) contains both RGD gene and UniProt protein IDs. }\n"+
        "!generated-by: RGD\n"+
        "!date-generated: #DATEX#\n";
    final String AGR_HEADER_COMMON_LINES =
        "!gaf-version: 2.2\n"+
        "!generated-by: RGD\n"+
        "!date-generated: #DATEX#\n";
    private String annotAgrDir;
    private boolean generateForAgr = false;

    boolean onInit() {
        return isGenerateForAgr() ? getSpeciesTypeKey()==SpeciesType.HUMAN : super.onInit();
    }

    String getOutputFileNamePrefix(int speciesTypeKey) {

        if( isGenerateForAgr() ) {
            return getAnnotAgrDir()+"9606_";
        }

        // taxonomy name is two words separated by space, f.e 'Rattus norvegicus'
        // we take only the first word
        String taxName = SpeciesType.getTaxonomicName(speciesTypeKey).toLowerCase();
        String prefix;
        int spacePos = taxName.indexOf(' ');
        if( spacePos>0 )
            prefix = taxName.substring(0, spacePos)+"_";
        else
            prefix = taxName;
        return prefix;
    }

    String getOutputFileNameSuffix(String ontId, int objectKey) {

        String suffix = RgdId.getObjectTypeName(objectKey).toLowerCase() + "s_";
        if( ontId.equals("CC") || ontId.equals("MF") || ontId.equals("BP") ) {
            suffix += "go";
        } else {
            suffix += ontId.toLowerCase();
        }
        if( isGenerateForAgr() ) {
            suffix += ".gaf";
        }
        return suffix;
    }

    static SimpleDateFormat _gafDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String getHeaderCommonLines() {
        // SpeciesRecord has a utility class that generates todays date and formats it as 'yyyy/MM/dd'
        // however gaf 2.2 spec requires the date in the header to be in format yyyy-MM-dd
        String todayDate;
        synchronized(_gafDateFormat) {
            todayDate = _gafDateFormat.format(new java.util.Date());
        }
        String header = isGenerateForAgr() ? AGR_HEADER_COMMON_LINES : HEADER_COMMON_LINES;
        return header.replace("#DATEX#", todayDate);
    }

    boolean acceptAnnotation(Annotation a) {
        // for AGR, process only GO annotations
        // and exclude annotations with 'RGD' source, because they were made by mistake
        if( isGenerateForAgr() ) {
            return a.getTermAcc().startsWith("GO:")
                    && !Utils.stringsAreEqual(a.getDataSrc(), "RGD");
        } else {
            return true;
        }
    }

    String mergeWithXrefSource(String references, String xrefSource) {

        if( Utils.isStringEmpty(xrefSource) ) {
            return references;
        }

        Set<String> refs = new TreeSet<>();

        String[] objs = references.split("[\\|\\,\\;]");
        Collections.addAll(refs, objs);

        objs = xrefSource.split("[\\|\\,\\;]");
        Collections.addAll(refs, objs);

        return Utils.concatenate(refs, "|");
    }

    String replaceRefs(String references, String oldRef, String newRef) {

        Set<String> refs = new TreeSet<>();

        String[] objs = references.split("[\\|\\,\\;]");
        Collections.addAll(refs, objs);

        if( refs.contains(oldRef) ) {
            refs.remove(oldRef);
            refs.add(newRef);
        }

        return Utils.concatenate(refs, "|");
    }

    String writeLine(AnnotRecord rec) {

        String objectID, references;

        if( isGenerateForAgr() ) {
            if (Utils.isStringEmpty(rec.hgncId)) {
                return null; // skip genes without HGNC id
            }
            objectID = rec.hgncId;
            references = rec.annot.getXrefSource();

            // patch: for IBA annotations, if 'references' contains PMID:21873635, GO_REF:0000033 will be appended
            //      this is to prevent a bug in ontobio python lib
            if( rec.annot.getEvidence().equals("IBA") && references!=null && references.contains("PMID:21873635") ) {
                references = replaceRefs(references, "PMID:21873635", "GO_REF:0000033");
            }

        } else {
            objectID = rec.annot.getAnnotatedObjectRgdId().toString();
            references = mergeWithXrefSource(rec.references, rec.annot.getXrefSource());
        }

        // column contents must comply with GAF 2.2 format
        String line = "RGD" +
                '\t' +
                objectID +
                '\t' +
                checkNull(rec.annot.getObjectSymbol()) +
                '\t' +
                checkNull(rec.annot.getQualifier()) +
                '\t' +
                checkNull(rec.termAccId) +
                '\t' +
                checkNull(references) +
                '\t' +
                checkNull(rec.annot.getEvidence()) +
                '\t' +
                checkNull(rec.withInfo) +
                '\t' +
                checkNull(rec.annot.getAspect()) +
                '\t' +
                checkNull(rec.annot.getObjectName()) +
                '\t' +
                checkNull(rec.meshOrOmimId) +
                '\t' +
                checkNull(rec.objectType) +
                '\t' +
                checkNull(rec.taxon) +
                '\t' +
                checkNull(rec.createdDate) +
                '\t' +
                checkNull(rec.annot.getDataSrc()) +
                '\t' +
                checkNull(rec.annot.getAnnotationExtension()) +
                '\t' +
                checkNull(rec.annot.getGeneProductFormId()) +
                '\n';
        return line;
    }

    private String annotDir;

    public String getAnnotDir() {
        return annotDir;
    }

    public void setAnnotDir(String annotDir) {
        this.annotDir = annotDir;
    }

    boolean processOnlyGenes() {
        return isGenerateForAgr();
    }

    boolean loadUniProtIds() {
        return false;
    }

    public void setAnnotAgrDir(String annotAgrDir) {
        this.annotAgrDir = annotAgrDir;
    }

    public String getAnnotAgrDir() {
        return annotAgrDir;
    }

    public boolean isGenerateForAgr() {
        return generateForAgr;
    }

    public void setGenerateForAgr(boolean generateForAgr) {
        this.generateForAgr = generateForAgr;
    }
}