package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.api.carbbuilder.payload.CarbBuilderRequest;
import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccess;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;

import java.io.*;

public class CarbBuilderConsoleOutputManager extends Thread
{
    private PdbEntryAccess pdbEntryAccess;
    private PdbBuildAccess pdbBuildAccess;

    private ProcessBuilder process;
    private PdbBuild build;



    public CarbBuilderConsoleOutputManager(PdbBuild build,  PdbBuildAccess pdbBuildAccess, PdbEntryAccess pdbEntryAccess, String carbBuilderFileLocation, boolean onLinux) throws IOException
    {
        this.build = build;
        this.pdbBuildAccess = pdbBuildAccess;
        this.pdbEntryAccess = pdbEntryAccess;

        if (!build.getCustomDihedral().trim().equals(""))
        {
            FileWriter writer = new FileWriter(new File(System.getProperty("user.dir") + "/" + build.getDihedralFilePath()));
            writer.write(build.getCustomDihedral());
            writer.close();
        }

        this.process = new ProcessBuilder();

        String fileName =  build.getPdbFilePath().replaceAll(".pdb", "");
        if (onLinux)
            process = new ProcessBuilder("mono", carbBuilderFileLocation, "-i", build.getCasperInput(), "-o",  System.getProperty("user.dir") + "/" + fileName);
        else
            process = new ProcessBuilder( carbBuilderFileLocation, "-i", build.getCasperInput(), "-o", fileName);

        if(build.getNoRepeatingUnits() > 0)
        {
            process.command().add("-r");
            process.command().add(String.valueOf(build.getNoRepeatingUnits()));
        }

        if (!build.getCustomDihedral().trim().equals(""))
        {
            process.command().add("-d");
            process.command().add(build.getDihedralFilePath());
        }
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
            String failReason = "This structure could not be built";
            String line = null;
            while ( (line = reader.readLine()) != null)
            {
                consoleOutputBuilder.append(line).append(System.getProperty("line.separator"));
                if (line.contains("FINAL linkage"))
                {
                    line = line.substring(line.indexOf(':') + 1).trim();
                    String res1 = line.substring(0, line.indexOf('('));
                    String pos1 = line.substring(line.indexOf('(') + 1, line.indexOf('-'));
                    String pos2 = line.substring(line.indexOf('>') + 1, line.indexOf(')'));
                    String res2 = line.substring(line.indexOf(')') + 1, line.indexOf(':'));
                    String phi = line.substring(line.indexOf(':') + 1, line.indexOf(','));
                    String[] angles = line.substring(line.indexOf(',') + 1).split(",");
                    String psi = angles[0];
                    String other = "";
                    for (int i = 1; i < angles.length; i++)
                    {
                        other += ',' + angles[i];
                    }
                    String out = res1 + ' ' + pos1 + ' ' + pos2 + ' ' + res2 + ",2,";
                    out += phi + ' ' + psi + ' ' + other;
                    linkageBuilder.append(out).append(System.getProperty("line.separator"));
                }
                else if(line.contains("not yet supported"))
                {
                    failReason = "Unsupported Residues: " + line.substring(line.indexOf('{') + 1, line.indexOf('}'));
                }
                else if(line.contains("PDB file Built:"))
                    build.setBuildSuccess();
            }
            p.waitFor();


            if (!build.isBuildSuccess())
            {
                build.setBuildFailed();
                build.setFailReason(failReason);
                build.setConsoleOutput("/faillogs/output" + build.getId() + ".log");
                FileWriter writer = new FileWriter(new File(System.getProperty("user.dir") + build.getConsoleOutput()));
                writer.write(consoleOutputBuilder.toString());
                writer.close();
            }
            else
            {
                PdbEntry entry = new PdbEntry(build.getId(), build.getPdbFilePath());
                entry.setLinkages(linkageBuilder.toString());
                pdbEntryAccess.save(entry);
            }

            pdbBuildAccess.save(build);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }

    }
}
