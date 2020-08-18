package com.uct.carbbuilder.api.file;

import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping(path = "/file")
public class PDBFileController
{
    @Autowired
    private PdbEntryAccess pdbEntryAccess;

    @CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "/{file_id}", method = RequestMethod.GET)
    public void getFile(@PathVariable("file_id") long fileId, HttpServletResponse response)
    {
        String fileName = "Not Found";
        try {
            response.setContentType("application/pdb");
            fileName = pdbEntryAccess.findById(fileId).get().getFilePath();
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName+ "\"");
            InputStream is = new FileInputStream(new File(fileName));
            IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        }
        catch (IOException ex)
        {
            System.out.println("Error writing file to output stream. Filename was " + fileName);
            throw new RuntimeException("IOError writing file to output stream");
        }

    }
}
