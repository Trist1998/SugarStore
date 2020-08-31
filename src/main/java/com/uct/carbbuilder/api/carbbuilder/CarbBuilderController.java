package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.api.carbbuilder.payload.CarbBuilderRequest;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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

    private static final String outputFolder = "pdbfiles/";

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/build")
    public ResponseEntity<?> carbBuilderRequest(@RequestBody CarbBuilderRequest request) throws NoSuchAlgorithmException, IOException
    {
        if(!request.isValid())
            return ResponseEntity.badRequest().body("Invalid Input");
        String buildHash = PdbEntry.getCasperHash(request.getCasperInput(), request.getNoRepeatingUnits(), carbBuilderVersion, request.getCustomDihedral());

        Optional<PdbEntry> optionalPdbEntry = pdbEntryAccess.findByHash(buildHash);
        PdbEntry entry;
        if(!optionalPdbEntry.isPresent())
        {
            entry = pdbEntryAccess.save(new PdbEntry(request.getCasperInput(), request.getNoRepeatingUnits(), carbBuilderVersion, request.getCustomDihedral()));
            String filename = outputFolder + "output" + entry.getId();
            entry.setFilePath(filename + ".pdb");
            entry.setBuildInProgress();
            pdbEntryAccess.save(entry);
            ProcessBuilder pb = new ProcessBuilder(carbBuilderFileLocation, "-i", request.getCasperInput(), "-o", filename);
            if(request.getNoRepeatingUnits() >= 0)
            {
                pb.command().add("-r");
                pb.command().add(String.valueOf(request.getNoRepeatingUnits()));
            }

            try
            {
                CarbBuilderConsoleOutputManager manager = new CarbBuilderConsoleOutputManager(pb, entry, pdbEntryAccess);
                manager.start();
            }
            catch (Exception e)
            {
                System.out.println(e.toString());
                return ResponseEntity.badRequest().body("Error occurred in the processing of this request");
            }
        }
        else
        {
           entry = optionalPdbEntry.get();
        }

        return ResponseEntity.accepted().body(entry.getBuildHash());

    }

}
