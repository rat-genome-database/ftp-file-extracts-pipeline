package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author mtutaj
 * @since August 18, 2016
 * Extracts annotated rgd genes with uniprot ids by ontology.
 */
public class AnnotUniProtExtractor extends AnnotBaseExtractor {


    final String HEADER_COMMON_LINES =
     "# RGD-PIPELINE: ftp-file-extracts\n"
    +"# MODULE: uniprot-annotations-version-1.0.1 (Sep 30, 2019)\n"
    +"# GENERATED-ON: #DATE#\n"
    +"# PURPOSE: uniprot annotations with references\n"
    +"# ONTOLOGY: #ONT#\n"
    +"# CONTACT: rgd.data@mcw.edu\n"
    +"# FORMAT: tab delimited text\n"
    +"#\n"
    +"#COLUMN INFORMATION:\n"
    +"#\n"
    +"#1   GENE_RGD_ID        unique RGD_ID of the annotated gene\n"
    +"#2   GENE_SYMBOL        official symbol of the annotated gene\n"
    +"#3   UNIPROT_ID         UniProtKB ID\n"
    +"#4   PMID               PubMed ID\n"
    +"#5   ONTOLOGY           ontology category\n"
    +"#6   EVIDENCE           evidence\n"
    +""
    +"GENE_RGD_ID\tGENE_SYMBOL\tUNIPROT_ID\tPMID\tONTOLOGY\tEVIDENCE\n";

    String getOutputFileNamePrefix(int speciesTypeKey) {
        return "bibliography_"+SpeciesType.getCommonName(speciesTypeKey).toLowerCase()+"_";
    }

    String getOutputFileNameSuffix(String ontId, int objectKey) {
        return ontId.toLowerCase();
    }

    String getHeaderCommonLines() {
        return HEADER_COMMON_LINES;
    }

    static Set<String> writtenLinesCache = new ConcurrentSkipListSet<>();
    static int added = 0;
    static int skipped = 0;

    void writeLine(AnnotRecord rec, PrintWriter writer) {

        Collection<String> pmids = new HashSet<>();
        for( String ref: rec.references.split("[\\|]") ) {
            if( ref.startsWith("PMID:") ) {
                pmids.add(ref.substring(5));
            }
        }

        for( String uniProtId: rec.uniprotIds ) {
            for( String pmid: pmids ) {

                String line = rec.annot.getAnnotatedObjectRgdId() + "\t"
                        + checkNull(rec.annot.getObjectSymbol()) + '\t'
                        + uniProtId + '\t'
                        + pmid + '\t'
                        + rec.ontName + '\t'
                        + rec.annot.getEvidence() + '\n';

                if( writtenLinesCache.add(line) ) {
                    writer.print(line);
                    ++added;
                } else {
                    ++skipped;
                }
            }
        }

        //System.out.println("  added "+added+" skipped "+skipped);
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
        return true;
    }
}