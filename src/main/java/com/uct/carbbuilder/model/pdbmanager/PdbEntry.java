package com.uct.carbbuilder.model.pdbmanager;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(uniqueConstraints=
            {
                @UniqueConstraint(columnNames = {"casperInput", "noRepeatingUnits", "carbBuilderVersion"})
            }
        )
public class PdbEntry
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String casperInput;
    private int noRepeatingUnits;

    private String carbBuilderVersion;
    private String filePath;
    private Date createDate;

    public PdbEntry()
    {
    }

    public PdbEntry(String casperInput, int noRepeatingUnits, String carbBuilderVersion)
    {
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
}
