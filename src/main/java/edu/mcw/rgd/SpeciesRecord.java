package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;

import java.text.SimpleDateFormat;

/**
 * @author mtutaj
 * @since Oct 11, 2010
 * All configurable information for given species
 */
public class SpeciesRecord {
    private int speciesType;
    private int newRefAssemblyMapKeyForMarkers;
    private int oldRefAssemblyMapKeyForMarkers;
    private int celeraAssemblyMapKey;
    private int cytoMapKey;
    private int cmMapKey; // for mouse only

    private String qtlFileName;
    private int qtlSizeEstimate;
    private String gp2ProteinFileName;
    private String markerFileName;
    private String markerAllelesFileName;

    /**
     * get today date formatted as yyyy/mm/dd
     * @return string containing today's date
     */
    static SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy/MM/dd");

    static public String getTodayDate() {
        return _dateFormat.format(new java.util.Date());
    }

    public String getSpeciesName() {
        return SpeciesType.getCommonName(speciesType);
    }

    public int getSpeciesType() {
        return speciesType;
    }

    public void setSpeciesType(int speciesType) {
        this.speciesType = speciesType;
    }

    public int getCeleraAssemblyMapKey() {
        return celeraAssemblyMapKey;
    }

    public void setCeleraAssemblyMapKey(int celeraAssemblyMapKey) {
        this.celeraAssemblyMapKey = celeraAssemblyMapKey;
    }

    public int getCytoMapKey() {
        return cytoMapKey;
    }

    public void setCytoMapKey(int cytoMapKey) {
        this.cytoMapKey = cytoMapKey;
    }

    public String getQtlFileName() {
        return qtlFileName;
    }

    public void setQtlFileName(String qtlFilename) {
        this.qtlFileName = qtlFilename;
    }

    public int getQtlSizeEstimate() {
        return qtlSizeEstimate;
    }

    public void setQtlSizeEstimate(int qtlSizeEstimate) {
        this.qtlSizeEstimate = qtlSizeEstimate;
    }

    public int getCmMapKey() {
        return cmMapKey;
    }

    public void setCmMapKey(int cmMapKey) {
        this.cmMapKey = cmMapKey;
    }

    public String getMarkerFileName() {
        return markerFileName;
    }

    public void setMarkerFileName(String fileName) {
        this.markerFileName = fileName;
    }

    public String getGp2ProteinFileName() {
        return gp2ProteinFileName;
    }

    public void setGp2ProteinFileName(String gp2ProteinFileName) {
        this.gp2ProteinFileName = gp2ProteinFileName;
    }

    public void setNewRefAssemblyMapKeyForMarkers(int newRefAssemblyMapKeyForMarkers) {
        this.newRefAssemblyMapKeyForMarkers = newRefAssemblyMapKeyForMarkers;
    }

    public int getNewRefAssemblyMapKeyForMarkers() {
        return newRefAssemblyMapKeyForMarkers;
    }

    public void setOldRefAssemblyMapKeyForMarkers(int oldRefAssemblyMapKeyForMarkers) {
        this.oldRefAssemblyMapKeyForMarkers = oldRefAssemblyMapKeyForMarkers;
    }

    public int getOldRefAssemblyMapKeyForMarkers() {
        return oldRefAssemblyMapKeyForMarkers;
    }

    public void setMarkerAllelesFileName(String markerAllelesFileName) {
        this.markerAllelesFileName = markerAllelesFileName;
    }

    public String getMarkerAllelesFileName() {
        return markerAllelesFileName;
    }
}
