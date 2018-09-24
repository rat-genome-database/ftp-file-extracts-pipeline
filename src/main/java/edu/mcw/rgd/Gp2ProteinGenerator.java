package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Nov 11, 2010
 * Time: 4:00:10 PM
 */
public class Gp2ProteinGenerator extends BaseExtractor {

    final String HEADER = "!Version: 1.7.1\n" +
            "!Date: %s\n" +
            "!Gene RGD IDs mapped to Uniprot and NCBI protein accessions\n" +
            "!From: RGD\n" +
            "!URL: http://rgd.mcw.edu\n" +
            "!Contact Email: RGD.Developers@mcw.edu\n" +
            "!\n" +
            "!Notes:\n" +
            "! Where UniProt ids are available they are used in preference to RefSeqs.\n" +
            "! If UniProtKB/Swiss-Prot entries are present, UniProtKB/TrEMBL ids won't be exported.\n" +
            "! Where multiple RefSeqs are mapped NP sequences are listed before XP (predicted) sequences.\n" +
            "! If RefSeq are not available, NCBI protein ids are listed, if present.\n" +
            "! All active protein-coding rat genes are included in the listing.\n" +
            "! Where no protein ID is available the second column/field is left blank.\n" +
            "!\n";

    int genesSkipped = 0;
    int genesWithUniProtKBSwiss = 0;
    int genesWithUniProtKB = 0;
    int genesWithRefSeq = 0;
    int genesWithGeneBankProt = 0;
    int genesWithNoProt = 0;

    public void run(SpeciesRecord speciesRec) throws Exception {

        String outputFile = speciesRec.getGp2ProteinFileName();
        if( outputFile==null )
            return;
        System.out.println("Running gp2protein.rgd export to file "+ outputFile);

        // extract protein info from database and write it into data file
        String fileName = generateFile(outputFile, getExtractDir());

        System.out.println("including:");
        System.out.println("  with UniProtKB/Swiss: "+genesWithUniProtKBSwiss);
        System.out.println("  with UniProtKB other: "+genesWithUniProtKB);
        System.out.println("  with RefSeq:          "+genesWithRefSeq);
        System.out.println("  with GeneBankProt:    "+genesWithGeneBankProt);
        System.out.println("  without a protein:    "+genesWithNoProt);

        System.out.println("Generation of gp2protein.rgd file complete!");
    }

    // analyze all rat genes and generate gp2protein.rgd file
    String generateFile(String fname, String tmpDir) throws Exception {

        // create uncompressed output file
        String fileName = tmpDir+'/'+fname;
        BufferedWriter writer;
        if( fileName.endsWith(".gz") ) {
            writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName))));
        } else {
            writer = new BufferedWriter(new FileWriter(fileName));
        }

        // write the header
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        writer.write(String.format(HEADER, sdt.format(new java.util.Date())));

        // Process active rat genes:
        // 1) with gene type 'protein-coding'
        // 2) with gene type 'gene' (corresponding to 'unknown' or 'other' gene type at NCBI)
        //    that do have protein accession ids
        // Retrieved genes are sorted by RGD-ID
        List<Gene> genes = getProteinCodingRatGenes();
        int proteinCodingGenes = 0;
        for( Gene gene: genes ) {
            if( processGene(writer, gene) ) {
                proteinCodingGenes++;
            } else {
                genesSkipped++;
            }
        }
        writer.close();

        System.out.println("Number of non-protein-coding genes: "+genesSkipped);
        System.out.println("Number of protein-coding genes:     "+proteinCodingGenes);

        return fileName;
    }

    /**
     *
     * @param writer
     * @param gene
     * @return true if a gene was processed, false if not
     * @throws Exception
     */
    boolean processGene(BufferedWriter writer, Gene gene) throws Exception {

        // first process all UniProtKB records
        // if there are UniProtKB/Swiss entries, use only them
        // otherwise use all available UniProtKB ids, which most likely will be UniProtKB/TrEMBL entries
        Set<String> proteinIds = processUniProt(gene.getRgdId());
        if( proteinIds.isEmpty() ) {
            // no UniProtKB ids: process NCBI protein ids
            // use RefSeq ids, if available (ignore others)
            List<XdbId> gbList = getDao().getXdbIds(gene.getRgdId(), XdbId.XDB_KEY_GENEBANKPROT);
            boolean areRefSeqsPresent = false;
            for( XdbId xdbId: gbList ) {
                // the protein accession id must start with either "NP_" or "XP_" ignore other
                if( xdbId.getAccId()==null )
                    continue;
                if( xdbId.getAccId().startsWith("NP_") || xdbId.getAccId().startsWith("XP_") ) {
                    proteinIds.add("RefSeq:"+xdbId.getAccId());
                    areRefSeqsPresent = true;
                }
            }

            if( areRefSeqsPresent ) {
                genesWithRefSeq++;
            } else {
                // process non-RefSeq protein ids
                boolean hasNcbiProtein = false;
                for( XdbId xdbId: gbList ) {
                    // the protein accession id must not start with neither "NP_" nor "XP_"
                    if( xdbId.getAccId()==null )
                        continue;
                    if( !xdbId.getAccId().startsWith("NP_") && !xdbId.getAccId().startsWith("XP_") ) {
                        proteinIds.add("NCBI:"+xdbId.getAccId());
                        hasNcbiProtein = true;
                    }
                }
                if( hasNcbiProtein ) {
                    genesWithGeneBankProt++;
                } else {
                    if( gene.getType().equals("protein-coding") ) {
                        genesWithNoProt++;
                    } else {
                        return false;
                    }
                }
            }
        }

        // output rgd id
        String line = "RGD:" + gene.getRgdId() + '\t' +
                Utils.concatenate(proteinIds, ";") + '\n';
        // write the line no matter if some protein entries have been written or not
        writer.write(line);

        return true;
    }

    List<Gene> getProteinCodingRatGenes() throws Exception {

        List<Gene> genes = getDao().getActiveGenes(SpeciesType.RAT);
        List<Gene> rgdIds = new ArrayList<>(genes.size());
        for( Gene gene: genes ) {
            if( Utils.stringsAreEqual(gene.getType(),"protein-coding")
                    || Utils.stringsAreEqual(gene.getType(),"gene")) {
                rgdIds.add(gene);
            } else {
                genesSkipped++;
            }
        }
        Collections.sort(rgdIds, new Comparator<Gene>() {
            @Override
            public int compare(Gene o1, Gene o2) {
                return o1.getRgdId() - o2.getRgdId();
            }
        });
        return rgdIds;
    }

    Set<String> processUniProt(int geneRgdId) throws Exception {

        // first sort all entries: swiss first then trembl
        List<XdbId> uniprotList = getDao().getXdbIds(geneRgdId, XdbId.XDB_KEY_UNIPROT);

        // RULE valid as of Sep 1, 2011 :
        // if there is at least one UniProtKB/Swiss entry, all UniProtKB/TrEMBL (and NCBI GENE) entries should be skipped
        Set<String> proteinIds = new TreeSet<>();
        for( XdbId xdbId: uniprotList ) {

            if( Utils.defaultString(xdbId.getSrcPipeline()).toUpperCase().contains("SWISS") ) {
                // the current entry is a swiss entry -- write it out
                proteinIds.add("UniProtKB:"+xdbId.getAccId());
            }
        }
        if( !proteinIds.isEmpty() ) {
            genesWithUniProtKBSwiss++;
            return proteinIds;
        }

        // no UniProtKB/Swiss entry, just get the rest, if any
        for( XdbId xdbId: uniprotList ) {
            proteinIds.add("UniProtKB:"+xdbId.getAccId());
        }
        if( !proteinIds.isEmpty() ) {
            genesWithUniProtKB++;
        }
        return proteinIds;
    }
}
