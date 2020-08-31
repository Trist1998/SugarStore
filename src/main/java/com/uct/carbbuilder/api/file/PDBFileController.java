package com.uct.carbbuilder.api.file;

import com.uct.carbbuilder.api.file.payload.PDBFileRequest;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;
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
    private PdbEntryAccess pdbEntryAccess;

    @CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "/{buildHash}", method = RequestMethod.GET)
    public void getFileDownload(@PathVariable("buildHash") String buildHash, HttpServletResponse response) throws IOException
    {
        String fileName = "Not Found";
        try
        {
            response.setContentType("application/pdb");
            fileName = pdbEntryAccess.findByHash(buildHash).get().getFilePath();
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName+ "\"");
            InputStream is = new FileInputStream(new File(fileName));
            IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        }
        catch (IOException ex)
        {
            System.out.println("Error writing file to output stream. Filename was " + fileName);
            response.sendError(-1, "Output Not found");
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "/text", method = RequestMethod.POST)
    public ResponseEntity<?> getFileText(@RequestBody PDBFileRequest request, HttpServletResponse response)
    {
        String filePath = "Not Found";
        try
        {
            PdbEntry entry =  pdbEntryAccess.findByHash(request.getBuildHash()).get();
            JSONObject responseData = new JSONObject();
            responseData.put("buildStatus", entry.getBuildStatus());
            if (entry.isBuildSuccess())
            {
                filePath = entry.getFilePath();
                String fileOutput = fileToString(filePath);

                responseData.put("pdb", fileOutput);
                responseData.put("linkages", entry.getLinkages());
            }
            return ResponseEntity.accepted().body(responseData.toJSONString());

        }
        catch (Exception ex)
        {
            System.out.println("Error writing file to output stream. Filename was " + filePath);
            return ResponseEntity.badRequest().body("Server Error");
        }
    }

    private static String fileToString(String filePath) throws IOException
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
