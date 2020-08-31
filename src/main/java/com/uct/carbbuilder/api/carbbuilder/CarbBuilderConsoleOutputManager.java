package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;

import java.io.*;

public class CarbBuilderConsoleOutputManager extends Thread
{
    private PdbEntryAccess pdbEntryAccess;

    private ProcessBuilder process;
    private PdbEntry entry;

    public CarbBuilderConsoleOutputManager(ProcessBuilder processBuilder, PdbEntry entry, PdbEntryAccess pdbEntryAccess)
    {
        this.process = processBuilder;
        this.entry = entry;
        this.pdbEntryAccess = pdbEntryAccess;
    }

    @Override
    public void run()
    {
        Process p = null;
        try
        {
            p = process.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder linkageBuilder = new StringBuilder();
            StringBuilder consoleOutputBuilder = new StringBuilder();
            String line = null;
            while ( (line = reader.readLine()) != null)
            {
                consoleOutputBuilder.append(line).append(System.getProperty("line.separator"));
                if (line.contains("FINAL linkage"))
                    linkageBuilder.append(line).append(System.getProperty("line.separator"));
                else if(line.contains("PDB file Built:"))
                    entry.setBuildSuccess();
            }
            p.waitFor();

            entry.setLinkages(linkageBuilder.toString());
            if (!entry.isBuildSuccess())
            {
                entry.setBuildFailed();
                entry.setConsoleOutput("/faillogs/output" + entry.getId() + ".log");
                FileWriter writer = new FileWriter(new File(System.getProperty("user.dir") + entry.getConsoleOutput()));
                writer.write(consoleOutputBuilder.toString());
                writer.close();
            }

            pdbEntryAccess.save(entry);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }

    }
}
