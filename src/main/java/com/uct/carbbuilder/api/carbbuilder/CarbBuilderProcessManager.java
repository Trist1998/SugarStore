package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccessService;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccessService;

import java.io.*;

public class CarbBuilderProcessManager extends Thread
{
    private PdbEntryAccessService pdbEntryAccess;
    private PdbBuildAccessService pdbBuildAccess;

    private ProcessBuilder process;
    private PdbBuild build;



    public CarbBuilderProcessManager(PdbBuild build, PdbBuildAccessService pdbBuildAccess, PdbEntryAccessService pdbEntryAccess, String carbBuilderFileLocation, boolean onLinux) throws IOException
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

        process.command().add("-PSF");
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
                    String resid1 = line.substring(line.indexOf('#') + 1, line.indexOf(' '));
                    String res1 = line.substring(line.indexOf(' ') + 1, line.indexOf('('));
                    String pos1 = line.substring(line.indexOf('(') + 1, line.indexOf('-'));
                    String pos2 = line.substring(line.indexOf('>') + 1, line.indexOf(')'));
                    String res2id2 = line.substring(line.indexOf(')') + 1, line.indexOf(':')).trim();
                    String resid2 = res2id2.substring(res2id2.indexOf('#') + 1, res2id2.indexOf(' '));
                    String res2 = res2id2.substring(res2id2.indexOf(' ') + 1);
                    String phi = line.substring(line.indexOf(':') + 1, line.indexOf(','));
                    String[] angles = line.substring(line.indexOf(',') + 1).split(",");
                    String psi = angles[0];
                    String other = "";
                    for (int i = 1; i < angles.length; i++)
                    {
                        other += ',' + angles[i];
                    }
                    String out = resid1 + ' ' + res1 + ' ' + pos1 + ' ' + pos2 + ' ' + resid2 + ' ' + res2 + phi + ' ' + psi + ' ' + other;
                    linkageBuilder.append(out).append(System.getProperty("line.separator"));
                }
                else if(line.contains("not yet supported"))
                {
                    failReason = "Unsupported Residues: " + line.substring(line.indexOf('{') + 1, line.indexOf('}'));
                }

            }
            p.waitFor();
            if (linkageBuilder.toString().isEmpty())
                build.setBuildFailed();
            else
                build.setBuildSuccess();

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
                build.setPsfBuilt(new File(entry.getPsfFilePath()).exists());
            }

            pdbBuildAccess.save(build);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }

    }
}
