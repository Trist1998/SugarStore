package com.uct.carbbuilder.api.file;

import com.uct.carbbuilder.api.file.payload.PDBFileRequest;
import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccessService;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccessService;
import net.minidev.json.JSONObject;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@RestController
@RequestMapping(path = "/file")
public class PDBFileController
{
    @Autowired
    private PdbEntryAccessService pdbEntryAccess;

    @Autowired
    private PdbBuildAccessService pdbBuildAccess;

    @CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "download/pdb/{buildHash}", method = RequestMethod.GET)
    public void getPDBFileDownload(@PathVariable("buildHash") String buildHash, HttpServletResponse response) throws IOException
    {
        String fileName = "Not Found";
        try
        {
            response.setContentType("application/pdb");
            PdbBuild build = pdbBuildAccess.findByBuildHash(buildHash).get();
            if (build.isBuildSuccess())
            {
                fileName = pdbEntryAccess.findByBuildId(build.getId()).get().getPdbFilePath();
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                InputStream is = new FileInputStream(new File(fileName));
                IOUtils.copy(is, response.getOutputStream());
                response.flushBuffer();
            }
            else
            {
                response.sendError(0, "File not found");
            }
        }
        catch (IOException ex)
        {
            System.out.println("Error writing file to output stream. Filename was " + fileName);
            response.sendError(-1, "Output Not found");
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "download/psf/{buildHash}", method = RequestMethod.GET)
    public void getPSFFileDownload(@PathVariable("buildHash") String buildHash, HttpServletResponse response) throws IOException
    {
        String fileName = "Not Found";
        try
        {
            response.setContentType("application/pdb");
            PdbBuild build = pdbBuildAccess.findByBuildHash(buildHash).get();
            if (build.isBuildSuccess())
            {
                fileName = pdbEntryAccess.findByBuildId(build.getId()).get().getPdbFilePath().replace(".pdb", ".psf");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName+ "\"");
                InputStream is = new FileInputStream(new File(fileName));
                IOUtils.copy(is, response.getOutputStream());
                response.flushBuffer();
            }
            else
            {
                response.sendError(0, "File not found");
            }
        }
        catch (IOException ex)
        {
            System.out.println("Error writing file to output stream. Filename was " + fileName);
            response.sendError(-1, "Output Not found");
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "/text/{buildHash}", method = RequestMethod.GET)
    public ResponseEntity<?> getFileText(@PathVariable("buildHash") String buildHash, HttpServletResponse response)
    {
        String filePath = "Not Found";
        PdbBuild build;
        try
        {
            build =  pdbBuildAccess.findByBuildHash(buildHash).get();

            JSONObject responseData = new JSONObject();
            responseData.put("buildStatus", build.getBuildStatus());
            if (build.isBuildSuccess())
            {
                responseData.put("psfBuilt", build.isPsfBuilt());
                PdbEntry entry = pdbEntryAccess.findByBuildId(build.getId()).get();
                filePath = entry.getPdbFilePath();
                String fileOutput = fileToString(filePath);
                responseData.put("pdb", fileOutput);
                responseData.put("linkages", entry.getLinkages());
            }
            else if(build.isBuildFailed())
            {
                responseData.put("failReason", build.getFailReason());
            }
            return ResponseEntity.accepted().body(responseData.toJSONString());

        }
        catch (Exception ex)
        {
            System.out.println("Error could not process request!");
            return ResponseEntity.badRequest().body("Server Error");
        }
    }

    public static String fileToString(String filePath) throws IOException
    {
        StringBuilder contentBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null)
        {
            contentBuilder.append(sCurrentLine).append("\n");
        }

        return contentBuilder.toString();
    }
}
