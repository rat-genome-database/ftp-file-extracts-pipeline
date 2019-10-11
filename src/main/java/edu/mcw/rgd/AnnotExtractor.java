package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;


/**
 * @author mtutaj
 * @since June 28, 2011
 * Extracts annotated rgd objects by ontology.
 * The annotated objects being exported are genes, qtls and strains.
 */
public class AnnotExtractor extends AnnotBaseExtractor {


    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: annotations-version-1.1.9 (Oct 10, 2019)\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: annotations about active #SPECIES# objects extracted from RGD database\n"
    +"# ONTOLOGY: #ONT#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"# NOTES: multiple values in a single column are separated by '|'\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"#\n"
    +"#1   RGD_ID	          unique RGD_ID of the annotated object\n"
    +"#2   OBJECT_SYMBOL      official symbol of the annotated object\n"
    +"#3   OBJECT_NAME        official name of the annotated object\n"
    +"#4   OBJECT_TYPE        annotated object data type: one of ['gene','qtl','strain']\n"
    +"#5   TERM_ACC_ID        ontology term accession id\n"
    +"#6   TERM_NAME          ontology term name\n"
    +"#7   QUALIFIER          optional qualifier\n"
    +"#8   EVIDENCE           evidence\n"
    +"#9   WITH               with info\n"
    +"#10  ASPECT             aspect\n"
    +"#11  REFERENCES         db references (Reference RGDID|PUBMED ID)\n"
    +"#12  CREATED_DATE       created date\n"
    +"#13  ASSIGNED_BY        assigned by\n"
    +"#14  MESH_OMIM_ID       MESH:xxx or OMIM:xxx id corresponding to RDO:xxx id found in TERM_ACC_ID column (RGD/CTD Disease Ontology annotations only)\n"
    +"#15  CURATION_NOTES     curation notes provided by RGD curators\n"
    +"#16  ORIGINAL_REFERENCE original reference\n"
    +""
    +"RGD_ID\tOBJECT_SYMBOL\tOBJECT_NAME\tOBJECT_TYPE\tTERM_ACC_ID\tTERM_NAME\tQUALIFIER\tEVIDENCE\tWITH\tASPECT\tREFERENCES\tCREATED_DATE\tASSIGNED_BY\tMESH_OMIM_ID\tCURATION_NOTES\tORIGINAL_REFERENCE\n";

    String getOutputFileNamePrefix(int speciesTypeKey) {

        String taxName = SpeciesType.getTaxonomicName(speciesTypeKey);
        int spacePos = taxName.indexOf(' ');
        if( spacePos>0 ) {
            // f.e. for rat, return 'rattus_terms_';
            return taxName.substring(0, spacePos).toLowerCase()+"_terms_";
        }
        return "terms_";
    }

    String getOutputFileNameSuffix(String ontId, int objectKey) {
        return ontId.toLowerCase();
    }

    String getHeaderCommonLines() {
        return HEADER_COMMON_LINES;
    }

    String writeLine(AnnotRecord rec) {

        String line = String.valueOf(rec.annot.getAnnotatedObjectRgdId()) +
                '\t' +
                checkNull(rec.annot.getObjectSymbol()) +
                '\t' +
                checkNull(rec.annot.getObjectName()) +
                '\t' +
                checkNull(rec.objectType) +
                '\t' +
                checkNull(rec.termAccId) +
                '\t' +
                checkNull(rec.termName) +
                '\t' +
                checkNull(rec.annot.getQualifier()) +
                '\t' +
                checkNull(rec.annot.getEvidence()) +
                '\t' +
                checkNull(rec.withInfo) +
                '\t' +
                checkNull(rec.annot.getAspect()) +
                '\t' +
                checkNull(rec.references) +
                '\t' +
                checkNull(rec.createdDate) +
                '\t' +
                checkNull(rec.annot.getDataSrc()) +
                '\t' +
                checkNull(rec.meshOrOmimId) +
                '\t' +
                checkNull(rec.curationNotes) +
                '\t' +
                checkNull(rec.annot.getXrefSource()) +
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
        return false;
    }

    boolean loadUniProtIds() {
        return false;
    }
}