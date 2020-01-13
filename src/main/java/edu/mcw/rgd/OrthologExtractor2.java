package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.Map;

/**
 * @author mtutaj
 * @since Jan 13, 2020
 */
public class OrthologExtractor2 extends BaseExtractor {

    final String HEADER =
        "# RGD-PIPELINE: ftp-file-extracts\n"+
        "# MODULE: orthologs-2-version-2000-01-13\n"+
        "# GENERATED-ON: #DATE#\n"+
        "# RGD Ortholog FTP file for #SPECIES#\n" +
        "# From: RGD\n" +
        "# URL: http://rgd.mcw.edu\n" +
        "# Contact: RGD.Data@mcw.edu\n" +
        "#\n" +
        "# Format:\n" +
        "# Comment lines begin with '#'\n" +
        "# Tab delimited fields, one line per #SPECIES# gene.\n" +
        "#\n" +
        "# Most orthologs listed will be one-to-one across each pair of species.\n" +
        "# However, where multiple homologs are present in RGD, fields will contain multiple entries separated by '|'.\n" +
        "#\n";

    Logger log = Logger.getLogger(getClass());

    public void run(SpeciesRecord speciesRec) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesRec.getSpeciesType()).toUpperCase();
        System.out.println(getVersion()+"   "+speciesName);

        // load data for the source species
        Map<Integer, StringBuffer> map = new HashMap<>();

        StringBuffer colInfoLine = new StringBuffer();
        colInfoLine.append(speciesName+"_GENE_SYMBOL\t");
        colInfoLine.append(speciesName+"_GENE_RGD_ID\t");
        if( speciesRec.getSpeciesType()==SpeciesType.HUMAN ) {
            colInfoLine.append("HGNC_ID\t");
        }
        else if( speciesRec.getSpeciesType()==SpeciesType.MOUSE ) {
            colInfoLine.append("MGI_ID\t");
        }

        colInfoLine.append(speciesName+"_GENE_NCBI_GENE_ID\t");
        colInfoLine.append(speciesName+"_GENE_ENSEMBL_GENE_ID");

        for( GeneInfo info: loadGeneInfo(speciesRec.getSpeciesType()).values() ) {

            StringBuffer lineBuf = new StringBuffer();
            map.put(info.geneRgdId, lineBuf);

            lineBuf.append(Utils.defaultString(info.geneSymbol)).append("\t");
            lineBuf.append(info.geneRgdId).append("\t");

            if( speciesRec.getSpeciesType()==SpeciesType.HUMAN ) {
                lineBuf.append(Utils.defaultString(info.primaryId)).append("\t");
            }
            else if( speciesRec.getSpeciesType()==SpeciesType.MOUSE ) {
                lineBuf.append(Utils.defaultString(info.primaryId)).append("\t");
            }

            lineBuf.append(Utils.defaultString(info.ncbiGeneId)).append("\t");
            lineBuf.append(Utils.defaultString(info.ensemblGeneId));
        }

        // load ortholog info
        for( int speciesTypeKey: SpeciesType.getSpeciesTypeKeys() ) {

            if( !SpeciesType.isSearchable(speciesTypeKey) ) {
                continue;
            }

            loadOrthologInfo(speciesRec.getSpeciesType(), speciesTypeKey, colInfoLine, map);
        }


        List<String> lines = sortLines(map);


        File dir = new File(getExtractDir());
        if( !dir.exists() ) {
            dir.mkdirs();
        }

        String outputFileName = getExtractDir()+'/'+speciesName+"_ORTHOLOGS.txt";
        log.info("started extraction to "+outputFileName);
        final PrintWriter writer = new PrintWriter(outputFileName);

        // prepare header common lines
        String commonLines = HEADER
            .replace("#DATE#", SpeciesRecord.getTodayDate())
            .replace("#SPECIES#", speciesName);
        writer.print(commonLines);

        colInfoLine.append("\n");
        writer.print(colInfoLine);

        for( String line: lines ) {
            writer.print(line);
        }
        writer.close();

        System.out.println(getVersion()+"   "+speciesName+"  OK");
    }

    List<String> sortLines( Map<Integer, StringBuffer> map ) {
        List<String> lines = new ArrayList<>(map.size());
        for( StringBuffer buf: map.values() ) {
            buf.append("\n");
            lines.add(buf.toString());
        }
        Collections.sort(lines);

        map.clear();
        return lines;
    }

    void loadOrthologInfo(int speciesTypeKey1, int speciesTypeKey2, StringBuffer colInfoLine, Map<Integer, StringBuffer> map) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesTypeKey2).toUpperCase();

        List<Ortholog> orthologs = getDao().getOrthologs(speciesTypeKey1, speciesTypeKey2);

        // build a map of orthologs
        Map<Integer, Ortholog> orthologMap = new HashMap<>(orthologs.size());
        for( Ortholog o: orthologs ) {
            Ortholog orthologInMap = orthologMap.get(o.getSrcRgdId());
            if( orthologInMap==null ) {
                orthologMap.put(o.getSrcRgdId(), o);
            } else {
                String symbol1 = getDao().getSymbolForGene(orthologInMap.getDestRgdId());
                String symbol2 = getDao().getSymbolForGene(o.getDestRgdId());
                if( symbol1.equals(symbol2) ) {
                    System.out.println("cannot resolve multi orthologs, same symbol");
                    System.out.println("  in-map: "+orthologInMap.dump("|"));
                    System.out.println("  incoming: "+o.dump("|"));
                } else {
                    String symbol = getDao().getSymbolForGene(o.getSrcRgdId());
                    if( symbol.equalsIgnoreCase(symbol1) ) {
                        System.out.println("multi orthologs, resolved by symbol");
                    } else
                    if( symbol.equalsIgnoreCase(symbol2) ) {
                        orthologMap.put(o.getSrcRgdId(), o);
                        System.out.println("multi orthologs, resolved by symbol");
                    } else {
                        System.out.println("cannot resolve multi orthologs, diff symbol");
                        System.out.println("  in-map: "+orthologInMap.dump("|"));
                        System.out.println("  incoming: "+o.dump("|"));
                    }
                }
            }
        }

        // generate columns in the header for species2
        colInfoLine.append("\t"+speciesName+"_GENE_SYMBOL");
        colInfoLine.append("\t"+speciesName+"_GENE_RGD_ID");

        if( speciesTypeKey2==SpeciesType.HUMAN ) {
            colInfoLine.append("\tHGNC_ID");
        }
        else if( speciesTypeKey2==SpeciesType.MOUSE ) {
            colInfoLine.append("\tMGI_ID");
        }

        colInfoLine.append("\t"+speciesName+"_GENE_NCBI_GENE_ID");
        colInfoLine.append("\t"+speciesName+"_GENE_ENSEMBL_GENE_ID");


        Map<Integer, GeneInfo> geneInfos = loadGeneInfo(speciesTypeKey2);

        for( int srcRgdId: orthologMap.keySet() ) {
            Ortholog o = orthologMap.get(srcRgdId);

            StringBuffer buf = map.get(o.getSrcRgdId());
            if( buf==null ) {
                System.out.println("unexpected hit: RGD:"+o.getDestRgdId());
                continue;
            }

            GeneInfo info = geneInfos.get(o.getDestRgdId());
            if( info==null ) {
                System.out.println("unexpected empty info: RGD:"+o.getDestRgdId());
                continue;
            }

            buf.append("\t").append(Utils.defaultString(info.geneSymbol));
            buf.append("\t").append(info.geneRgdId);

            if( speciesTypeKey2==SpeciesType.HUMAN ) {
                buf.append("\t").append(Utils.defaultString(info.primaryId));
            }
            else if( speciesTypeKey2==SpeciesType.MOUSE ) {
                buf.append("\t").append(Utils.defaultString(info.primaryId));
            }

            buf.append("\t").append(Utils.defaultString(info.ncbiGeneId));
            buf.append("\t").append(Utils.defaultString(info.ensemblGeneId));
        }
    }

    Map<Integer, GeneInfo> loadGeneInfo( int speciesTypeKey ) throws Exception {

        Map<Integer, GeneInfo> infos = new HashMap<>();

        // load gene rgd id and symbol
        for( Gene gene: getDao().getActiveGenes(speciesTypeKey) ) {
            GeneInfo info = new GeneInfo();
            info.geneRgdId = gene.getRgdId();
            info.geneSymbol = gene.getSymbol();
            infos.put(info.geneRgdId, info);
        }

        // load primary ids
        List<XdbId> primaryIds = null;
        if( speciesTypeKey==SpeciesType.HUMAN ) {
            primaryIds = getDao().getActiveXdbIds(XdbId.XDB_KEY_HGNC, RgdId.OBJECT_KEY_GENES);
        }
        else if( speciesTypeKey==SpeciesType.MOUSE ) {
            primaryIds = getDao().getActiveXdbIds(XdbId.XDB_KEY_MGD, RgdId.OBJECT_KEY_GENES);
        }
        if( primaryIds!=null ) {
            for( XdbId xdbId: primaryIds ) {
                GeneInfo info = infos.get(xdbId.getRgdId());
                if( info!=null ) {
                    if( info.primaryId==null ) {
                        info.primaryId = xdbId.getAccId();
                    } else {
                        info.primaryId += "|" + xdbId.getAccId();
                    }
                }
            }
        }

        // load NCBI gene ids
        List<XdbId> ncbiGeneIds = getDao().getActiveXdbIds(XdbId.XDB_KEY_NCBI_GENE, RgdId.OBJECT_KEY_GENES);
        for( XdbId xdbId: ncbiGeneIds ) {
            GeneInfo info = infos.get(xdbId.getRgdId());
            if( info!=null ) {
                if( info.ncbiGeneId==null ) {
                    info.ncbiGeneId = xdbId.getAccId();
                } else {
                    info.ncbiGeneId += "|" + xdbId.getAccId();
                }
            }
        }

        // load Ensembl gene ids
        List<XdbId> ensemblGeneIds = getDao().getActiveXdbIds(XdbId.XDB_KEY_ENSEMBL_GENES, RgdId.OBJECT_KEY_GENES);
        for( XdbId xdbId: ensemblGeneIds ) {
            GeneInfo info = infos.get(xdbId.getRgdId());
            if( info!=null ) {
                if( info.ensemblGeneId==null ) {
                    info.ensemblGeneId = xdbId.getAccId();
                } else {
                    if( !info.ensemblGeneId.contains(xdbId.getAccId()) ) {
                        info.ensemblGeneId += "|" + xdbId.getAccId();
                    }
                }
            }
        }

        return infos;
    }

    class GeneInfo {
        public int geneRgdId;
        public String geneSymbol;
        public String primaryId; // HGNC ID or MGI ID
        public String ncbiGeneId;
        public String ensemblGeneId;
    }
}
