package com.uct.carbbuilder.api.carbbuilder.payload;

public class CarbBuilderRequest
{
    private String casperInput;
    private int noRepeatingUnits;

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

}
