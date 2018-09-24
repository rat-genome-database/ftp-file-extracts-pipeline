package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;


/**
 * @author mtutaj
 * @since June 28, 2011
 * Extracts annotated rgd objects by ontology in GAF 2.1 format.
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
        "!gaf-version: 2.1\n"+
        "!{ As of December 2016, the gene_association.rgd file only contains 'RGD' in column 1 and RGD gene identifiers in column 2. }\n"+
        "!{ The gene_protein_association.rgd file (available on the RGD ftp site) contains both RGD gene and UniProt protein IDs. }\n";
    final String AGR_HEADER_COMMON_LINES =
            "!gaf-version: 2.1\n";
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

    String getHeaderCommonLines() {
        return isGenerateForAgr() ? AGR_HEADER_COMMON_LINES : HEADER_COMMON_LINES;
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

    void writeLine(AnnotRecord rec, PrintWriter writer) {

        String objectID, references;

        if( isGenerateForAgr() ) {
            if (Utils.isStringEmpty(rec.hgncId)) {
                return; // skip genes without HGNC id
            }
            objectID = rec.hgncId;
            references = rec.annot.getXrefSource();
        } else {
            objectID = rec.annot.getAnnotatedObjectRgdId().toString();
            references = mergeWithXrefSource(rec.references, rec.annot.getXrefSource());
        }

        // column contents must comply with GAF 2.0 format
        writer.append("RGD")
            .append('\t')
            .append(objectID)
            .append('\t')
            .append(checkNull(rec.annot.getObjectSymbol()))
            .append('\t')
            .append(checkNull(rec.annot.getQualifier()))
            .append('\t')
            .append(checkNull(rec.termAccId))
            .append('\t')
            .append(checkNull(references))
            .append('\t')
            .append(checkNull(rec.annot.getEvidence()))
            .append('\t')
            .append(checkNull(rec.withInfo))
            .append('\t')
            .append(checkNull(rec.annot.getAspect()))
            .append('\t')
            .append(checkNull(rec.annot.getObjectName()))
            .append('\t')
            .append(checkNull(rec.meshOrOmimId))
            .append('\t')
            .append(checkNull(rec.objectType))
            .append('\t')
            .append(checkNull(rec.taxon))
            .append('\t')
            .append(checkNull(rec.createdDate))
            .append('\t')
            .append(checkNull(rec.annot.getDataSrc()))
            .append('\t')
            .append('\t')
            .append('\n');
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