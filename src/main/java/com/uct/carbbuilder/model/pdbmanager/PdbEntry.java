package com.uct.carbbuilder.model.pdbmanager;

import javax.persistence.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Entity
@Table
public class PdbEntry
{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String pdbFilePath;

    private long pdbBuildId;

    @Column(columnDefinition="TEXT")
    private String linkages;

    private Date createDate;

    public PdbEntry()
    {
    }

    public PdbEntry(long pdbBuildId, String fileName)
    {
        this.pdbBuildId = pdbBuildId;
        this.createDate = new Date();
        this.pdbFilePath = fileName;
    }



    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
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

    public String getPdbFilePath()
    {
        return pdbFilePath;
    }

    public void setPdbFilePath(String filePath)
    {
        this.pdbFilePath = filePath;
    }

    public String getLinkages()
    {
        return linkages;
    }

    public void setLinkages(String linkages)
    {
        this.linkages = linkages;
    }

    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    public long getPdbBuildId()
    {
        return pdbBuildId;
    }

    public void setPdbBuildId(long pdbBuildId)
    {
        this.pdbBuildId = pdbBuildId;
    }

    public String getPsfFilePath()
    {
        return pdbFilePath.replace(".pdb", ".psf");
    }
}

