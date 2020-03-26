package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.api.carbbuilder.payload.CarbBuilderRequest;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping(path = "/carbbuilder")
public class CarbBuilderController
{
    @Autowired
    private PdbEntryAccess pdbEntryAccess;

    @Value("${twoody.app.carbbuilderversion}")
    private String carbBuilderVersion;

    @Value("${twoody.app.carbbuilderurl}")
    private String carbBuilderFileLocation;

    @PostMapping("/build")
    public ResponseEntity<?> carbBuilderRequest(@RequestBody CarbBuilderRequest request) throws IOException
    {
        if(!request.isValid())
            return ResponseEntity.badRequest().body("Invalid Input");

        Optional<PdbEntry> optionalPdbEntry = pdbEntryAccess.findByCasperInput(request.getCasperInput(), request.getNoRepeatingUnits(), carbBuilderVersion);
        String path = "";
        if(!optionalPdbEntry.isPresent())
        {
            PdbEntry entry = pdbEntryAccess.save(new PdbEntry(request.getCasperInput(), request.getNoRepeatingUnits(), carbBuilderVersion));
            String filename = "output" + entry.getId();
            entry.setFilePath(filename + ".pdb");
            pdbEntryAccess.save(entry);
            ProcessBuilder pb = new ProcessBuilder(carbBuilderFileLocation, "-i", request.getCasperInput(), "-o", filename);
            if(request.getNoRepeatingUnits() > 0)
            {
                pb.command().add("-r");
                pb.command().add(String.valueOf(request.getNoRepeatingUnits()));
            }

            try
            {
                pb.start();
            }
            catch (Exception e)
            {
                System.out.println(e.toString());
                return ResponseEntity.badRequest().body("Error occurred in the processing of this request");
            }
            path = entry.getFilePath();
        }
        else
        {
           path = optionalPdbEntry.get().getFilePath();
        }

        try
        {
            String fileOutput = fileToString(path);
            return ResponseEntity.accepted().body(fileOutput);
        }
        catch(Exception e)
        {
            return ResponseEntity.badRequest().body("Error occurred in the processing of this request");
        }

    }

    private static String fileToString(String filePath) throws Exception
    {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath)))
        {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null)
            {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        }
        catch (IOException e)
        {
            throw e;
        }
        return contentBuilder.toString();
    }

}
