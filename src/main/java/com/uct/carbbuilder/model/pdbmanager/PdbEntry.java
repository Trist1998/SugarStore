package com.uct.carbbuilder.model.pdbmanager;

import javax.persistence.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Entity
public class PdbEntry
{
    public static final short IN_PROGRESS = 0;
    public static final short SUCCESS = 1;
    public static final short FAILED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique = true)
    private String buildHash;

    @Column(columnDefinition="TEXT")
    private String casperInput;
    private int noRepeatingUnits;

    private String carbBuilderVersion;
    private String filePath;
    private Date createDate;

    @Column(columnDefinition="TEXT")
    private String consoleOutput;

    private short buildStatus;

    @Column(columnDefinition="TEXT")
    private String linkages;

    @Column(columnDefinition="TEXT")
    private String customDihedral;

    public PdbEntry()
    {
    }

    public PdbEntry(String casperInput, int noRepeatingUnits, String carbBuilderVersion, String customDihedral) throws NoSuchAlgorithmException
    {
        this.buildHash = getCasperHash(casperInput, noRepeatingUnits, carbBuilderVersion, customDihedral);
        this.casperInput = casperInput;
        this.noRepeatingUnits = noRepeatingUnits;
        this.carbBuilderVersion = carbBuilderVersion;
        this.createDate = new Date();
    }



    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getCasperInput()
    {
        return casperInput;
    }

    public void setCasperInput(String casperInput)
    {
        this.casperInput = casperInput;
    }

    public static String getCasperHash(String casperInput, int noRepeatingUnits, String carbBuilderVersion, String customDihedral) throws NoSuchAlgorithmException
    {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        String input = casperInput +  " " + noRepeatingUnits + " " + carbBuilderVersion + " " + customDihedral;
        byte[] output = messageDigest.digest(input.getBytes());
        return new String(output);
    }

    public int getNoRepeatingUnits()
    {
        return noRepeatingUnits;
    }

    public void setNoRepeatingUnits(int noRepeatingUnits)
    {
        this.noRepeatingUnits = noRepeatingUnits;
    }

    public boolean isValid()
    {
        return !getCasperInput().isEmpty();
    }

    public String getCarbBuilderVersion()
    {
        return carbBuilderVersion;
    }

    public void setCarbBuilderVersion(String carbBuilderVersion)
    {
        this.carbBuilderVersion = carbBuilderVersion;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    public String getBuildHash()
    {
        return buildHash;
    }

    public void setBuildHash(String casperHash)
    {
        this.buildHash = casperHash;
    }

    public String getConsoleOutput()
    {
        return consoleOutput;
    }

    public void setConsoleOutput(String consoleOutput)
    {
        this.consoleOutput = consoleOutput;
    }

    public boolean isBuildInProgress()
    {
        return buildStatus == IN_PROGRESS;
    }

    public void setBuildInProgress()
    {
        this.buildStatus = IN_PROGRESS;
    }

    public boolean isBuildSuccess()
    {
        return buildStatus == SUCCESS;
    }

    public void setBuildSuccess()
    {
        this.buildStatus = SUCCESS;
    }

    public boolean isBuildFailed()
    {
        return buildStatus == FAILED;
    }

    public void setBuildFailed()
    {
        this.buildStatus = FAILED;
    }

    public String getCustomDihedral()
    {
        return customDihedral;
    }

    public void setCustomDihedral(String customDihedral)
    {
        this.customDihedral = customDihedral;
    }

    public String getLinkages()
    {
        return linkages;
    }

    public void setLinkages(String linkages)
    {
        this.linkages = linkages;
    }

    public short getBuildStatus()
    {
        return buildStatus;
    }
}
