package com.uct.carbbuilder.api.carbbuilder.payload;

public class CarbBuilderRequest
{
    private String casperInput;
    private int noRepeatingUnits;
    private String customDihedral;

    public CarbBuilderRequest()
    {

    }

    public CarbBuilderRequest(String casperInput, int noRepeatingUnits, String customDihedral)
    {
        this.casperInput = casperInput;
        this.noRepeatingUnits = noRepeatingUnits;
        this.customDihedral = customDihedral;
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

    public String getCustomDihedral()
    {
        return customDihedral;
    }

    public void setCustomDihedral(String customDihedral)
    {
        this.customDihedral = customDihedral;
    }
}
