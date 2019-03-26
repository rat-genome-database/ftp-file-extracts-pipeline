package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.datamodel.*;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map;

/**
 * Created by jthota on 3/22/2019.
 */
public class InteractionsExtractor extends BaseExtractor {

    private String version;
    private String outputDir;
    private String outputSubDir;

    ProteinDAO pdao= new ProteinDAO();
    InteractionsDAO idao= new InteractionsDAO();
    AssociationDAO assocDao= new AssociationDAO();
    OntologyXDAO xdao=new OntologyXDAO();

    Map<String, String> intTypes=new HashMap<>();
    Map<Integer, Protein> proteins=new HashMap<>();
    Map<Integer, List<Gene>> geneProteinMap=new HashMap<>();
    @Override
    public void run(SpeciesRecord speciesInfo) throws Exception {
        int speciesTypeKey= SpeciesType.parse(speciesInfo.getSpeciesName());
        List<Integer> proteinRgdIds= getProteinRgdIds(speciesTypeKey);
        List<Interaction> interactions= getInteractionsByRgdIdsList(proteinRgdIds);
        String tsvFilePath = getExtractDir()+'/'+outputSubDir+"/"+"interactions_"+speciesInfo.getSpeciesName();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        PrintWriter tsvWriter = new PrintWriter(tsvFilePath);
        tsvWriter.write(
                "# RGD-PIPELINE: ftp-file-extracts\n"
                        +"# MODULE: interactions-version-1.0.0\n"
                        +"# GENERATED-ON: "+dateFormat.format(date)+"\n"
                        +"# CONTACT: rgd.data@mcw.edu\n"
                        +"# FORMAT: tab delimited text\n"
                        +"# NOTES:\n\tThis file provides a set of protein-protein binary interactions for species ("+speciesInfo.getSpeciesName() +").\n\tThe file is in the tab-delimited format.\n\tThe interaction data is sourced from the IMEx consortium."
                        +"\n\tMultiple values in a single column are separated by ';'\n"
                        +"# SPECIES: "+ speciesInfo.getSpeciesName()+"\n"
                        +"# COUNT: "+ interactions.size()+" interactions\n"
                        +"Interactor A \tInteractor A Gene\tInteractor A Gene RGD_ID\tInteractor B\tInteractor B Gene\tInteractor B Gene RGD_ID\tSpecies A\tSpecies B\tInteraction Type\tAttributes\n"

        );
        for(Interaction i:interactions){
            Protein interactor1=getProtein(i.getRgdId1());
            Protein interactor2=getProtein(i.getRgdId2());
            List<Gene> genes1=getGeneByProteinRgdId(i.getRgdId1());
            List<Gene> genes2=getGeneByProteinRgdId(i.getRgdId2());
            String geneSymbol1=getGeneSymbol(genes1);
            String geneSymbol2=getGeneSymbol(genes2);
            String geneRgdId1=getGeneRgdId(genes1);
            String geneRgdId2=getGeneRgdId(genes2);
            String interactionType=getInteractionType(i.getInteractionType());

            String species1=SpeciesType.getCommonName(interactor1.getSpeciesTypeKey());
            String species2=SpeciesType.getCommonName(interactor2.getSpeciesTypeKey());
            String attributes= getAttributes(i.getInteractionKey());
            tsvWriter.write(interactor1.getUniprotId()+"\t");
            tsvWriter.write(geneSymbol1+"\t");
            tsvWriter.write(geneRgdId1+"\t");

            tsvWriter.write(interactor2.getUniprotId()+"\t");
            tsvWriter.write(geneSymbol2+"\t");
            tsvWriter.write(geneRgdId2+"\t");
            tsvWriter.write(species1+"\t");
            tsvWriter.write(species2+"\t");
            tsvWriter.write(interactionType+"\t");
            tsvWriter.write(attributes+"\t");
            tsvWriter.write("\n");
        }
        tsvWriter.close();

    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getOutputSubDir() {
        return outputSubDir;
    }

    public void setOutputSubDir(String outputSubDir) {
        this.outputSubDir = outputSubDir;
    }
    public Collection[] split(List<Integer> rgdids, int size) throws Exception {
        int numOfBatches = rgdids.size() / size + 1;
        Collection[] batches = new Collection[numOfBatches];

        for(int index = 0; index < numOfBatches; ++index) {
            int count = index + 1;
            int fromIndex = Math.max((count - 1) * size, 0);
            int toIndex = Math.min(count * size, rgdids.size());
            batches[index] = rgdids.subList(fromIndex, toIndex);
        }

        return batches;
    }
    public List<Integer> getProteinRgdIds(int speciesTypeKey) throws Exception {
        List<Protein> proteins=pdao.getProteins(speciesTypeKey);
        List<Integer> rgdIds= new ArrayList<>();

        for(Protein p: proteins){
            rgdIds.add(p.getRgdId());
        }
        return rgdIds;
    }
    public List<Interaction> getInteractionsByRgdIdsList(List<Integer> proteinRgdIds) throws Exception {
        List<Interaction> interactions= new ArrayList<>();
        Collection[] colletions = this.split(proteinRgdIds, 1000);

        for(int i=0; i<colletions.length;i++){
            List c= (List) colletions[i];
            interactions.addAll(idao.getInteractionsByRgdIdsList(c));
        }
        return interactions;
    }
    public Protein getProtein(int rgdId) throws Exception {
        Protein p= new Protein();
        if(proteins.get(rgdId)!=null){
           p=proteins.get(rgdId);
        }else{
            p=pdao.getProtein(rgdId);
            proteins.put(rgdId, p);
        }
       return p;
    }
    public List<Gene> getGeneByProteinRgdId(int proteinRgdId) throws Exception {
        List<Gene> genes= new ArrayList<>();
        if(geneProteinMap.get(proteinRgdId)!=null){
            genes=geneProteinMap.get(proteinRgdId);
        }else{
            genes=assocDao.getAssociatedGenesForMasterRgdId(proteinRgdId, "protein_to_gene");
            geneProteinMap.put(proteinRgdId, genes);
        }
       return genes;
    }
    public String getGeneSymbol(List<Gene> genes){
        StringBuilder sb= new StringBuilder();
        boolean first=true;
        for(Gene g:genes){
            if(first) {
                sb.append(g.getSymbol());
                first=false;
            }else{
                sb.append("; ").append(g.getSymbol());
            }

        }
        return sb.toString();
    }
    public String getGeneRgdId(List<Gene> genes){
        StringBuilder sb= new StringBuilder();
        boolean first=true;
        for(Gene g:genes){
            if(first) {
                sb.append(g.getRgdId());
                first=false;
            }else{
                sb.append("; ").append(g.getRgdId());
            }

        }
        return sb.toString();
    }
    public String getInteractionType(String accId) {
        String interactionType = new String();
        if (intTypes.get(accId) != null) {
            interactionType = intTypes.get(accId);
        } else {
            try {
                interactionType = xdao.getTermByAccId(accId).getTerm();
                intTypes.put(accId, interactionType);
            } catch (Exception e) {
                System.out.println("interaction TYpe: " + accId);
                e.printStackTrace();
            }

        }
        return interactionType;
    }
    public String getAttributes(int interactionKey) throws Exception {
        InteractionAttributesDAO adao= new InteractionAttributesDAO();
        List<InteractionAttribute> attributes=adao.getAttributes(interactionKey);
        StringBuilder sb=new StringBuilder();
        boolean first=true;
        for(InteractionAttribute a: attributes){
            if(first){
                first=false;
                sb.append(a.getAttributeName()).append(":").append(a.getAttributeValue());
            }else{
                sb.append("; ").append(a.getAttributeName()).append(":").append(a.getAttributeValue());
            }
        }
        return sb.toString();
    }
}
