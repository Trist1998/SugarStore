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

