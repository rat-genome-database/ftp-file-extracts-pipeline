package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.datamodel.MappedGene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author mtutaj
 * @since 6/10/15
 */
public class AssemblyComparisonExtractor extends BaseExtractor {
    private String outputDir;
    private List<String> assemblies;
    private Map<Integer,String> assemblyNames;

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setAssemblies(List<String> assemblies) {
        this.assemblies = assemblies;
    }

    public List<String> getAssemblies() {
        return assemblies;
    }

    public void setAssemblyNames(Map<Integer,String> assemblyNames) {
        this.assemblyNames = assemblyNames;
    }

    public Map<Integer,String> getAssemblyNames() {
        return assemblyNames;
    }

    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {

        // it runs only for rat
        if( speciesInfo.getSpeciesType()!= SpeciesType.RAT )
            return;

        for( String entry: getAssemblies() ) {
            int dashPos = entry.indexOf('-');
            int mapKey1 = Integer.parseInt(entry.substring(0, dashPos));
            int mapKey2 = Integer.parseInt(entry.substring(dashPos+1));

            run(mapKey1, mapKey2);
        }
    }

    /**
     *
     * @param mapKeyNew map key for the new (newer) assembly
     * @param mapKeyOld map key for the old (older) assembly
     */
    void run(int mapKeyNew, int mapKeyOld) throws Exception {

        String assemblyNew = getAssemblyNames().get(mapKeyNew);
        String assemblyOld = getAssemblyNames().get(mapKeyOld);
        String dirName = getOutputDir()+"/"+assemblyNew+" vs "+assemblyOld;

        // ensure the output directory is created
        File dir = new File(dirName);
        dir.mkdirs();


        // generate file with genes only on the new assembly
        Date now = new Date();
        SimpleDateFormat sdt = new SimpleDateFormat("yyyyMMdd");
        String today = sdt.format(now);
        SimpleDateFormat sdt2 = new SimpleDateFormat("MMM dd, yyyy");
        String header =
                "# genes having position on rat assembly "+assemblyNew+",\n" +
                "# but not having position on assembly "+assemblyOld+"\n" +
                "# generated on: "+sdt2.format(now)+"\n" +
                "#\n";
        String[] colNames = {"RGD_ID","GENE_SYMBOL","GENE_TYPE","CHROMOSOME","START_POS","STOP_POS"};
        String fileName = dir + "/genes_on_" + assemblyNew + "_only_" + today + ".txt";
        String sql =
          "select g.rgd_id,g.gene_symbol,g.gene_type_lc GENE_TYPE,m.chromosome,m.start_pos,m.stop_pos "+
          "from genes g,rgd_ids r,maps_data m "+
          "where g.rgd_id=r.rgd_id and object_status='ACTIVE' "+
          "and exists(select 1 from maps_data m where m.rgd_id=g.rgd_id and map_key="+mapKeyNew+") "+
          "and not exists(select 1 from maps_data m where m.rgd_id=g.rgd_id and map_key="+mapKeyOld+") "+
          "and g.rgd_id=m.rgd_id and m.map_key="+mapKeyNew+" "+
          "order by gene_symbol_lc ";

        generateFile(fileName, header, sql, colNames);


        // generate file with genes only on the old assembly
        header =
                "# genes having position on rat assembly "+assemblyOld+",\n" +
                "# but not having position on assembly "+assemblyNew+"\n" +
                "# generated on: "+sdt2.format(now)+"\n" +
                "#\n";
        fileName = dir + "/genes_on_" + assemblyOld + "_only_" + today + ".txt";
        sql =
          "select g.rgd_id,g.gene_symbol,g.gene_type_lc GENE_TYPE,m.chromosome,m.start_pos,m.stop_pos "+
          "from genes g,rgd_ids r,maps_data m "+
          "where g.rgd_id=r.rgd_id and object_status='ACTIVE' "+
          "and exists(select 1 from maps_data m where m.rgd_id=g.rgd_id and map_key="+mapKeyOld+") "+
          "and not exists(select 1 from maps_data m where m.rgd_id=g.rgd_id and map_key="+mapKeyNew+") "+
          "and g.rgd_id=m.rgd_id and m.map_key="+mapKeyOld+" "+
          "order by gene_symbol_lc ";

        generateFile(fileName, header, sql, colNames);


        // generate file with genes on different chromosomes
        header =
                "#genes that are on different chromosomes in the assemblies ("+assemblyOld+","+assemblyNew+")\n" +
                "# generated on: "+sdt2.format(now)+"\n" +
                "#\n";
        fileName = dir + "/genes_diff_chr_" + today + ".txt";
        generateGenesWithPosOnDiffChr(fileName, header, mapKeyOld, mapKeyNew, assemblyOld, assemblyNew);
        /*
        String[] colNames2 = {"RGD_ID","GENE_SYMBOL","GENE_TYPE","CHR_"+assemblyOld,"CHR_"+assemblyNew};
        sql =
          "select c1.rgd_id,g.gene_symbol,g.gene_type_lc GENE_TYPE,c1.chromosome \"CHR_"+assemblyOld+"\",c2.chromosome \"CHR_"+assemblyNew+"\" from (  \n" +
          "  select distinct g.rgd_id,chromosome from genes g,rgd_ids r,maps_data m\n" +
          "  where g.rgd_id=r.rgd_id and object_status='ACTIVE' and m.rgd_id=g.rgd_id and map_key="+mapKeyOld+"\n" +
          "  ) c1, (\n" +
          "  select distinct g.rgd_id,chromosome from genes g,rgd_ids r,maps_data m\n" +
          "  where g.rgd_id=r.rgd_id and object_status='ACTIVE' and m.rgd_id=g.rgd_id and map_key="+mapKeyNew+"\n" +
          ") c2,\n" +
          "rgd_ids r, genes g\n" +
          "where c1.chromosome<>c2.chromosome and c1.rgd_id=c2.rgd_id  and c1.rgd_id=r.rgd_id and object_status='ACTIVE'\n" +
          "and r.rgd_id=g.rgd_id\n"+
          "order by gene_symbol_lc ";

        generateFile(fileName, header, sql, colNames2);
        */

        // generate file with genes not having positions on neither the old or the new assembly
        header =
                "#genes not positioned on "+assemblyOld+" assembly nor "+assemblyNew+" assembly\n" +
                "# generated on: "+sdt2.format(now)+"\n" +
                "#\n";
        String[] colNames3 = {"RGD_ID","GENE_SYMBOL","GENE_TYPE","NUCLEOTIDE_ACC_IDS","PROTEIN_ACC_IDS"};
        fileName = dir + "/genes_no_pos_" + today + ".txt";
        sql =
          "select g.rgd_id,g.gene_symbol,g.gene_type_lc GENE_TYPE,\n" +
          "     (select listagg(acc_id,',') within group (order by acc_id) FROM rgd_acc_xdb x where x.rgd_id=g.rgd_id and xdb_key=1) NUCL_IDS,\n" +
          "     (select listagg(acc_id,',') within group (order by acc_id) FROM rgd_acc_xdb x where x.rgd_id=g.rgd_id and xdb_key=7) PROT_IDS\n" +
          "  from genes g,rgd_ids r\n" +
          "  where g.rgd_id=r.rgd_id and object_status='ACTIVE' AND species_type_key=3 AND gene_type_lc not in('allele','splice')\n" +
          "  and not exists(select 1 from maps_data m where m.rgd_id=g.rgd_id and map_key="+mapKeyOld+")\n" +
          "  and not exists(select 1 from maps_data m where m.rgd_id=g.rgd_id and map_key="+mapKeyNew+")\n"+
          "order by gene_symbol_lc ";

        generateFile(fileName, header, sql, colNames3);

        System.out.println("===OK===");
    }

    // generate file with genes on different chromosomes
    void generateGenesWithPosOnDiffChr(String fileName, String header, int mapKeyOld, int mapKeyNew, String assemblyOld, String assemblyNew) throws Exception {

        System.out.println("  "+fileName);

        String[] colNames2 = {"RGD_ID","GENE_SYMBOL","GENE_TYPE","CHR_"+assemblyOld,"CHR_"+assemblyNew,
                "START_POS_"+assemblyOld, "STOP_POS_"+assemblyOld, "START_POS_"+assemblyNew, "STOP_POS_"+assemblyNew,};

        try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {

            out.write(header);

            out.write(Utils.concatenate(colNames2, "\t"));
            out.write("\n");

            // rat genes having positions for both assemblies
            String sql = "SELECT g.rgd_id,g.gene_symbol,g.gene_type_lc FROM genes g,rgd_ids r\n" +
                    "WHERE g.rgd_id=r.rgd_id AND species_type_key=3 AND object_status='ACTIVE'\n" +
                    "  AND EXISTS(SELECT 1 FROM maps_data md WHERE md.rgd_id=g.rgd_id AND md.map_key="+mapKeyOld+")\n" +
                    "  AND EXISTS(SELECT 1 FROM maps_data md WHERE md.rgd_id=g.rgd_id AND md.map_key="+mapKeyNew+")\n" +
                    "ORDER BY gene_symbol_lc";
            String[] colNames = {"RGD_ID","GENE_SYMBOL","GENE_TYPE"};
            List<String[]> genesInfo = getDao().getRows(sql, colNames);
            for( String[] info: genesInfo ) {
                if( info[0].equals("RGD_ID") ) { continue; }
                int rgdId = Integer.parseInt(info[0]);
                List<MapData> md1s = getDao().getMapData(rgdId, mapKeyOld);
                List<MapData> md2s = getDao().getMapData(rgdId, mapKeyNew);

                // if there is a shared chromosome, remove thee positions from both lists
                removeSharedChr(md1s, md2s);

                String chrOld, startPosOld, stopPosOld;
                String chrNew, startPosNew, stopPosNew;

                if( md1s.isEmpty() && md2s.isEmpty() ) {
                    continue;
                }

                if( md1s.isEmpty() ) {
                    md1s.add(new MapData());
                }
                if( md2s.isEmpty() ) {
                    md2s.add(new MapData());
                }

                for( MapData md1: md1s ) {
                    if( md1.getChromosome()==null ) {
                        chrOld = startPosOld = stopPosOld = "-";
                    } else {
                        chrOld = md1.getChromosome();
                        startPosOld = Integer.toString(md1.getStartPos());
                        stopPosOld = Integer.toString(md1.getStopPos());
                    }

                    for( MapData md2: md2s ) {
                        if( md2.getChromosome()==null ) {
                            chrNew = startPosNew = stopPosNew = "-";
                        } else {
                            chrNew = md2.getChromosome();
                            startPosNew = Integer.toString(md2.getStartPos());
                            stopPosNew = Integer.toString(md2.getStopPos());
                        }

                        out.write(info[0]); // RGD_ID
                        out.write("\t");
                        out.write(info[1]); // GENE_SYMBOL
                        out.write("\t");
                        out.write(info[2]); // GENE_TYPE
                        out.write("\t");

                        out.write(chrOld);
                        out.write("\t");
                        out.write(chrNew);
                        out.write("\t");

                        out.write(startPosOld);
                        out.write("\t");
                        out.write(stopPosOld);
                        out.write("\t");

                        out.write(startPosNew);
                        out.write("\t");
                        out.write(stopPosNew);
                        out.write("\n");
                    }
                }
            }
        }

        /*
        header =
                "#genes that are on different chromosomes in the assemblies ("+assemblyOld+","+assemblyNew+")\n" +
                        "# generated on: "+sdt2.format(now)+"\n" +
                        "#\n";
        String[] colNames2 = {"RGD_ID","GENE_SYMBOL","GENE_TYPE","CHR_"+assemblyOld,"CHR_"+assemblyNew};
        fileName = dir + "/genes_diff_chr_" + today + ".txt";
        sql =
                "select c1.rgd_id,g.gene_symbol,g.gene_type_lc GENE_TYPE,c1.chromosome \"CHR_"+assemblyOld+"\",c2.chromosome \"CHR_"+assemblyNew+"\" from (  \n" +
                        "  select distinct g.rgd_id,chromosome from genes g,rgd_ids r,maps_data m\n" +
                        "  where g.rgd_id=r.rgd_id and object_status='ACTIVE' and m.rgd_id=g.rgd_id and map_key="+mapKeyOld+"\n" +
                        "  ) c1, (\n" +
                        "  select distinct g.rgd_id,chromosome from genes g,rgd_ids r,maps_data m\n" +
                        "  where g.rgd_id=r.rgd_id and object_status='ACTIVE' and m.rgd_id=g.rgd_id and map_key="+mapKeyNew+"\n" +
                        ") c2,\n" +
                        "rgd_ids r, genes g\n" +
                        "where c1.chromosome<>c2.chromosome and c1.rgd_id=c2.rgd_id  and c1.rgd_id=r.rgd_id and object_status='ACTIVE'\n" +
                        "and r.rgd_id=g.rgd_id\n"+
                        "order by gene_symbol_lc ";

        generateFile(fileName, header, sql, colNames2);
        */
    }

    void removeSharedChr(List<MapData> md1s, List<MapData> md2s) {
        while( _removeSharedChr(md1s, md2s) ) {
        }
    }

    boolean _removeSharedChr(List<MapData> md1s, List<MapData> md2s) {
        String chr = null;
        for( MapData md1: md1s ) {
            for( MapData md2: md2s ) {
                if( md1.getChromosome().equals(md2.getChromosome()) ) {
                    chr = md1.getChromosome();
                    break;
                }
            }
            if( chr!=null ) {
                break;
            }
        }

        if( chr==null ) {
            return false;
        }

        Iterator<MapData> it = md1s.iterator();
        while( it.hasNext() ) {
            MapData md = it.next();
            if( md.getChromosome().equals(chr) ) {
                it.remove();
            }
        }
        it = md2s.iterator();
        while( it.hasNext() ) {
            MapData md = it.next();
            if( md.getChromosome().equals(chr) ) {
                it.remove();
            }
        }
        return true;
    }

    void generateFile(String fileName, String header, String sql, String[] colNames) throws Exception {

        System.out.println("  "+fileName);

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(header);

            for( String[] row: getDao().getRows(sql, colNames) ) {
                int i = 0;
                for( String col: row ) {
                    if( i>0 )
                        writer.write('\t');
                    if( col!=null )
                        writer.write(col);
                    i++;
                }
                writer.write('\n');
            }
        }
    }
}
