package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.process.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author mtutaj
 * @since Oct 11, 2010
 * represents gene information extracted from database
 */
public class GeneExtractRecord {


    int geneKey;
    int rgdId; // gene rgd id
    String geneSymbol;
    String geneFullName;
    String geneDesc;
    String geneType;
    String refSeqStatus;

    // map data
    List<MapData> cytoMap = new ArrayList<>();
    List<MapData> celeraMap = new ArrayList<>();
    List<MapData> assembly1Map = new ArrayList<>();
    List<MapData> assembly2Map = new ArrayList<>();
    List<MapData> assembly3Map = new ArrayList<>();
    List<MapData> assembly4Map = new ArrayList<>();
    Double absPos; // absolute position for cM mouse map
    List<MapData> ensemblMap = new ArrayList<>();

    Set<String> curatedRefRgdIds = new TreeSet<String>();
    Set<String> curatedPubmedIds = new TreeSet<String>();
    Set<String> uncuratedPubmedIds = new TreeSet<String>();
    Set<String> ncbiGeneIds = new TreeSet<>();
    Set<String> uniprotIds = new TreeSet<>();
    Set<String> geneBankNucleoIds = new TreeSet<>();
    Set<String> tigerIds = new TreeSet<String>();
    Set<String> geneBankProteinIds = new TreeSet<String>();
    Set<String> uniGeneIds = new TreeSet<String>();
    Set<String> hgncIds = new TreeSet<String>(); // human only
    Set<String> vgncIds = new TreeSet<String>(); // pig and dog only
    Set<String> omimIds = new TreeSet<String>(); // human only
    Set<String> mgdIds = new TreeSet<String>(); // mouse only
    Set<String> ensemblGeneIds = new TreeSet<String>();

    Set<String> spliceRgdIds = new TreeSet<String>();
    Set<String> spliceSymbols = new TreeSet<String>();

    Set<String> markerRgdIds = new TreeSet<String>();
    Set<String> markerNames = new TreeSet<String>();

    Set<String> oldGeneSymbols = new TreeSet<String>();
    Set<String> oldGeneNames = new TreeSet<String>();

    Set<String> qtlRgdIds = new TreeSet<String>();
    Set<String> qtlNames = new TreeSet<String>();

    Set<String> refSeqNucleoIds = new TreeSet<String>();
    Set<String> refSeqProteinIds = new TreeSet<String>();

    String nomenEvents;

    String getString(Set<String> set) {
        return Utils.concatenate(set, ";");
    }

    public int getGeneKey() {
        return geneKey;
    }

    public void setGeneKey(int geneKey) {
        this.geneKey = geneKey;
    }

    public int getRgdId() {
        return rgdId;
    }

    public void setRgdId(int rgdId) {
        this.rgdId = rgdId;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public String getGeneFullName() {
        return geneFullName;
    }

    public void setGeneFullName(String geneFullName) {
        this.geneFullName = geneFullName;

        // replace any whitespace characters, like tabs and newlines, with a single space
        // (extra tabs in full name when exported into tab-separated-file could cause trouble)
        if( this.geneFullName!=null ) {
            this.geneFullName = this.geneFullName.replaceAll("\\s+", " ").trim();
        }
    }

    public String getGeneDesc() {
        return geneDesc;
    }

    public void setGeneDesc(String geneDesc) {
        this.geneDesc = geneDesc;
    }

    public String getGeneType() {
        return geneType;
    }

    public void setGeneType(String geneType) {
        this.geneType = geneType;
    }

    public String getRefSeqStatus() {
        return refSeqStatus;
    }

    public void setRefSeqStatus(String refSeqStatus) {
        this.refSeqStatus = refSeqStatus;
    }

    public String getCuratedRefRgdIds() {
        return getString(curatedRefRgdIds);
    }

    public void addCuratedRefRgdIds(String curatedRefRgdIds) {
        if( curatedRefRgdIds!=null )
            this.curatedRefRgdIds.add(curatedRefRgdIds);
    }

    public String getCuratedPubmedIds() {
        return getString(curatedPubmedIds);
    }

    public void addCuratedPubmedIds(String curatedPubmedIds) {
        if( curatedPubmedIds!=null )
            this.curatedPubmedIds.add(curatedPubmedIds);
    }

    public String getUncuratedPubmedIds() {
        return getString(uncuratedPubmedIds);
    }

    public void addUncuratedPubmedIds(String uncuratedPubmedIds) {
        if( uncuratedPubmedIds!=null )
            this.uncuratedPubmedIds.add(uncuratedPubmedIds);
    }

    public String getNcbiGeneIds() {
        return getString(ncbiGeneIds);
    }

    public void addNcbiGeneIds(String ncbiGeneIds) {
        if( ncbiGeneIds!=null )
            this.ncbiGeneIds.add(ncbiGeneIds);
    }

    public String getUniprotIds() {
        return getString(uniprotIds);
    }

    public void addUniprotIds(String uniprotIds) {
        if( uniprotIds!=null )
            this.uniprotIds.add(uniprotIds);
    }

    public String getGeneBankNucleoIds() {
        return getString(geneBankNucleoIds);
    }

    public void addGeneBankNucleoIds(String geneBankNucleoIds) {
        if( geneBankNucleoIds!=null ) {
            this.geneBankNucleoIds.add(geneBankNucleoIds);
            if( geneBankNucleoIds.startsWith("XM_")||geneBankNucleoIds.startsWith("NM_")||geneBankNucleoIds.startsWith("XR_"))
                this.refSeqNucleoIds.add(geneBankNucleoIds);
        }
    }

    public String getTigerIds() {
        return getString(tigerIds);
    }

    public void addTigerIds(String tigerIds) {
        if( tigerIds!=null )
            this.tigerIds.add(tigerIds);
    }

    public String getGeneBankProteinIds() {
        return getString(geneBankProteinIds);
    }

    public void addGeneBankProteinIds(String geneBankProteinIds) {
        if( geneBankProteinIds!=null ) {
            this.geneBankProteinIds.add(geneBankProteinIds);
            if( geneBankProteinIds.startsWith("XP_")||geneBankProteinIds.startsWith("NP_") )
                this.refSeqProteinIds.add(geneBankProteinIds);
        }
    }

    public String getUniGeneIds() {
        return getString(uniGeneIds);
    }

    public void addUniGeneIds(String uniGeneIds) {
        if( uniGeneIds!=null )
            this.uniGeneIds.add(uniGeneIds);
    }

    public String getEnsemblGeneIds() {
        return getString(ensemblGeneIds);
    }

    public void addEnsemblGeneIds(String ensemblGeneIds) {
        if( ensemblGeneIds!=null )
            this.ensemblGeneIds.add(ensemblGeneIds);
    }

    public String getSpliceRgdIds() {
        return getString(spliceRgdIds);
    }

    public void addSpliceRgdIds(String spliceRgdIds) {
        if( spliceRgdIds!=null )
            this.spliceRgdIds.add(spliceRgdIds);
    }

    public String getSpliceSymbols() {
        return getString(spliceSymbols);
    }

    public void addSpliceSymbols(String spliceSymbols) {
        if( spliceSymbols!=null )
            this.spliceSymbols.add(spliceSymbols);
    }

    public String getMarkerRgdIds() {
        return getString(markerRgdIds);
    }

    public void addMarkerRgdIds(String markerRgdIds) {
        if( markerRgdIds!=null )
            this.markerRgdIds.add(markerRgdIds);
    }

    public String getMarkerNames() {
        return getString(markerNames);
    }

    public void addMarkerNames(String markerNames) {
        if( markerNames!=null )
            this.markerNames.add(markerNames);
    }

    public String getOldGeneSymbols() {
        return getString(oldGeneSymbols);
    }

    public void addOldGeneSymbols(String oldGeneSymbols) {
        if( oldGeneSymbols!=null )
            this.oldGeneSymbols.add(oldGeneSymbols);
    }

    public String getOldGeneNames() {
        return getString(oldGeneNames);
    }

    public void addOldGeneNames(String oldGeneNames) {
        if( oldGeneNames!=null )
            this.oldGeneNames.add(oldGeneNames);
    }

    public String getQtlRgdIds() {
        return getString(qtlRgdIds);
    }

    public void addQtlRgdIds(String qtlRgdIds) {
        if( qtlRgdIds!=null )
            this.qtlRgdIds.add(qtlRgdIds);
    }

    public String getQtlNames() {
        return getString(qtlNames);
    }

    public void addQtlNames(String qtlNames) {
        if( qtlNames!=null )
            this.qtlNames.add(qtlNames);
    }

    public String getNomenEvents() {
        return nomenEvents;
    }

    public void setNomenEvents(String nomenEvents) {
        this.nomenEvents = nomenEvents;
    }

    public String getHgncIds() {
        return getString(hgncIds);
    }

    public void addHgncIds(String hgncIds) {
        if( hgncIds!=null )
            this.hgncIds.add(hgncIds);
    }

    public String getVgncIds() {
        return getString(vgncIds);
    }

    public void addVgncIds(String vgncIds) {
        if( vgncIds!=null )
            this.vgncIds.add(vgncIds);
    }

    public String getMgdIds() {
        return getString(mgdIds);
    }

    public void addMgdIds(String mgdIds) {
        if( mgdIds!=null )
            this.mgdIds.add(mgdIds);
    }

    public Double getAbsPos() {
        return absPos;
    }

    public void setAbsPos(Double absPos) {
        this.absPos = absPos;
    }

    public String getOmimIds() {
        return getString(omimIds);
    }

    public void addOmimIds(String omimIds) {
        if( omimIds!=null )
            this.omimIds.add(omimIds);
    }

    public String getRefSeqNucleoIds() {
        return getString(refSeqNucleoIds);
    }

    public String getRefSeqProteinIds() {
        return getString(refSeqProteinIds);
    }
}
