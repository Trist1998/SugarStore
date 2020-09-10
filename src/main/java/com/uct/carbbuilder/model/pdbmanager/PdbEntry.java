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

    private static final String OUTPUT_FOLDER = "pdbfiles/";
    private static final String DIHEDRAL_FOLDER = "dihedrals/";
    private static final String CONSOLE_LOG_FOLDER = "faillogs/";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique = true)
    private String buildHash;

    @Column(columnDefinition="TEXT")
    private String casperInput;
    private int noRepeatingUnits;

    private String carbBuilderVersion;
    private String pdbFilePath;
    private String dihedralFilePath;
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
        return escapeHash(output);
    }

    private static String escapeHash(byte[] bytes)
    {
        String out = "";
        for(byte by: bytes)
        {
            int b = Math.abs(by);
            if(b < 48 || (b > 57 && b < 65) || (b > 90 && b < 97) || b > 122)
                out += (int) b;
            else
                out += (char)b;

        }
        return out;
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

    public String getPdbFilePath()
    {
        if (pdbFilePath == null || pdbFilePath.equals(""))
        {
            pdbFilePath = OUTPUT_FOLDER + "output" + id + ".pdb";
        }

        return pdbFilePath;
    }

    public void setPdbFilePath(String filePath)
    {
        this.pdbFilePath = filePath;
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
        if (consoleOutput == null || consoleOutput.equals(""))
        {
            consoleOutput = CONSOLE_LOG_FOLDER + "console" + id + ".log";
        }

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

    public String getDihedralFilePath()
    {
        if (dihedralFilePath == null || dihedralFilePath.equals(""))
        {
            dihedralFilePath = DIHEDRAL_FOLDER + "dihedral" + id + ".txt";
        }

        return dihedralFilePath;
    }

    public void setDihedralFilePath(String dihedralFilePath)
    {
        this.dihedralFilePath = dihedralFilePath;
    }

}

