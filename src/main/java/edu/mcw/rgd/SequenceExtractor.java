package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.FastaParser;
import edu.mcw.rgd.process.Utils;
import edu.mcw.rgd.process.mapping.MapManager;

import java.io.*;
import java.util.*;

/**
 * Created by mtutaj on 2/12/2018.
 */
public class SequenceExtractor extends BaseExtractor {

    final String header = """
        ; RGD-PIPELINE: ftp-file-extracts
        ; MODULE: sequence-extractor   build Oct 20, 2025
        ; GENERATED-ON: #DATE#
        ; CONTACT: rgd.data@mcw.edu
        ; FORMAT: fasta/Pearson sequence format
        ; DESCRIPTION:
        ;This file contains transcript sequences derived from the genomic reference assembly sequence.
        ;For each transcript, the chromosome, start position, stop position and strand for all exons
        ;in the transcript are downloaded from NCBI's RefSeq nucleotide database.  Using these boundaries,
        ;exon sequences are extracted from the genomic reference and these subsequences are joined to form
        ;the corresponding 'reference-derived' transcript sequence.  The RefSeq ID given for each sequence
        ;indicates what exon boundaries were used to construct that sequence.  The sequences in this file may contain,
        ;and often do contain, differences compared to the corresponding sequences found in the NCBI RefSeq nucleotide database.
        """;

    FastaParser fastaParser = new FastaParser();

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        edu.mcw.rgd.datamodel.Map refAssemblyMap = MapManager.getInstance().getReferenceAssembly(speciesInfo.getSpeciesType());
        final int mapKey = refAssemblyMap.getKey();
        fastaParser.setMapKey(mapKey);
        if( fastaParser.getChrDir()==null ) {
            System.out.println(fastaParser.getLastError());
            return;
        }

        String fastaFilePath = getSpeciesSpecificExtractDir(speciesInfo)+"/"+refAssemblyMap.getName()+"-derived_transcript_sequences.fa.gz";
        BufferedWriter out = Utils.openWriter(fastaFilePath);

        String headerWithDate = header.replace("#DATE#", SpeciesRecord.getTodayDate());
        out.write(headerWithDate);

        List<Gene> genes = getDao().getActiveGenes(speciesInfo.getSpeciesType());
        Collections.sort(genes, (o1, o2) -> Utils.stringsCompareToIgnoreCase(o1.getSymbol(), o2.getSymbol()));

        for( Gene gene: genes ) {
            dumpFastaSeqs(gene, mapKey, out);
        }
        out.close();
    }

    void dumpFastaSeqs(Gene gene, int mapKey, BufferedWriter out) throws Exception {

        List<Transcript> transcripts = getDao().getTranscriptsForGene(gene.getRgdId(), mapKey);
        for( Transcript tr: transcripts ) {

            // note: multiple loci could be returned: filter out those outside range
            // and focus only on exons
            List<MapData> loci = getLoci(tr, mapKey);
            for( MapData locus: loci ) {
                List<TranscriptFeature> tfs = getDao().getFeatures(tr.getRgdId(), locus.getMapKey());
                Iterator<TranscriptFeature> it = tfs.iterator();
                while (it.hasNext()) {
                    TranscriptFeature tf = it.next();
                    if (tf.getFeatureType() != TranscriptFeature.FeatureType.EXON) {
                        it.remove();
                        continue;
                    }
                    if (!transcriptPositionOverlapsGenePosition(locus, tf)) {
                        it.remove();
                    }
                }

                StringBuilder buf = new StringBuilder();
                if (locus.getStrand().equals("+")) {
                    // join exons ascendingly
                    for (TranscriptFeature tf : tfs) {

                        String chunk = fastaParser.getSequence(tf.getChromosome(), tf.getStartPos(), tf.getStopPos());
                        chunk = chunk.replace("\n","");
                        buf.append(chunk);
                    }

                } else if (locus.getStrand().equals("-")) {
                    // join exons descendingly
                    for (int i = tfs.size() - 1; i >= 0; i--) {
                        TranscriptFeature tf = tfs.get(i);
                        String chunk = fastaParser.getSequence(tf.getChromosome(), tf.getStartPos(), tf.getStopPos());
                        if( chunk==null ) {
                            String msg = "ERROR: cannot find reference sequence file for chromosome "+tf.getChromosome()
                                + " at "+fastaParser.getChrDir();
                            throw new Exception(msg);
                        }
                        chunk = chunk.replace("\n","");
                        StringBuffer chunk2 = reverseComplement(chunk);
                        buf.append(chunk2);
                    }

                } else {
                    System.out.println("unexpected tr strand");
                    continue;
                }

                out.write(">"+tr.getAccId()+", gene RGD:"+gene.getRgdId()+", gene symbol: "+gene.getSymbol());
                out.write(", locus: chr"+locus.getChromosome()+":"+locus.getStartPos()+".."+locus.getStopPos()+"\n");

                // line len 70
                writeFasta(out, buf);
            }
        }
    }

    List<MapData> getLoci(Transcript tr, int mapKey) {
        List<MapData> loci = tr.getGenomicPositions();
        Iterator<MapData> it = loci.iterator();
        while( it.hasNext() ) {
            MapData md = it.next();
            if( md.getMapKey()!=mapKey ) {
                it.remove();
            }
        }
        return loci;
    }

    public boolean transcriptPositionOverlapsGenePosition(MapData md1, MapData md2) {
        // map keys must match
        if( !md1.getMapKey().equals(md2.getMapKey()) )
            return false;
        // chromosomes must match
        if( !Utils.stringsAreEqualIgnoreCase(md1.getChromosome(), md2.getChromosome()) )
            return false;
        // positions must overlap
        if( md1.getStopPos() < md2.getStartPos() )
            return false;
        if( md2.getStopPos() < md1.getStartPos() )
            return false;
        return true;
    }

    static public StringBuffer reverseComplement(CharSequence dna) throws Exception {

        StringBuffer buf = new StringBuffer(dna.length());
        for( int i=dna.length()-1; i>=0; i-- ) {
            char ch = dna.charAt(i);
            if( ch=='A' || ch=='a' ) {
                buf.append('T');
            } else if( ch=='C' || ch=='c' ) {
                buf.append('G');
            } else if( ch=='G' || ch=='g' ) {
                buf.append('C');
            } else if( ch=='T' || ch=='t' ) {
                buf.append('A');
            } else if( ch=='N' || ch=='n' ) {
                buf.append('N');
            }
            else throw new Exception("reverseComplement: unexpected nucleotide ["+ch+"]");
        }
        return buf;
    }

    void writeFasta(BufferedWriter out, StringBuilder seq) throws IOException {
        // break lines by 70
        for( int pos = 0; pos<seq.length(); pos += 70 ) {
            int end = pos + 70;
            if( end>=seq.length() ) {
                out.write(seq.substring(pos));
            } else {
                out.write(seq.substring(pos, end));
            }
            out.write("\n");
        }
    }
}
