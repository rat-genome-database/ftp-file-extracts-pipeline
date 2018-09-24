package edu.mcw.rgd;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 8/26/14
 * Time: 11:09 AM
 * <p>
 * helper class to describe information about an extract file
 */
public class FileInfo {
    private String objectType; // f.e. 'genes','qtls','strains'
    private String fileName;
    private String assembly1Build;
    private int assembly1MapKey;
    private String assembly2Build;
    private int assembly2MapKey;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAssembly1Build() {
        return assembly1Build;
    }

    public void setAssembly1Build(String assembly1Build) {
        this.assembly1Build = assembly1Build;
    }

    public int getAssembly1MapKey() {
        return assembly1MapKey;
    }

    public void setAssembly1MapKey(int assembly1MapKey) {
        this.assembly1MapKey = assembly1MapKey;
    }

    public String getAssembly2Build() {
        return assembly2Build;
    }

    public void setAssembly2Build(String assembly2Build) {
        this.assembly2Build = assembly2Build;
    }

    public int getAssembly2MapKey() {
        return assembly2MapKey;
    }

    public void setAssembly2MapKey(int assembly2MapKey) {
        this.assembly2MapKey = assembly2MapKey;
    }
}
