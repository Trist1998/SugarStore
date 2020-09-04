package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.api.carbbuilder.payload.CarbBuilderRequest;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
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
    @Value("${twoody.app.onlinux}")
    private boolean onLinux;





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
            if (!request.getCustomDihedral().trim().equals(""))
            {
                FileWriter writer = new FileWriter(new File(System.getProperty("user.dir") + "/" + entry.getDihedralFilePath()));
                writer.write(request.getCustomDihedral());
                writer.close();
            }
            String filename = entry.getPdbFilePath().replaceAll(".pdb", "");
            entry.setBuildInProgress();
            pdbEntryAccess.save(entry);
            ProcessBuilder pb;
            if (onLinux)
                pb = new ProcessBuilder("mono", carbBuilderFileLocation, "-i", request.getCasperInput(), "-o", System.getProperty("user.dir")+ "/" + filename);
            else
                pb = new ProcessBuilder( carbBuilderFileLocation, "-i", request.getCasperInput(), "-o", filename);

            if(request.getNoRepeatingUnits() > 0)
            {
                pb.command().add("-r");
                pb.command().add(String.valueOf(request.getNoRepeatingUnits()));
            }

            try
            {
                for (String cm: pb.command())
                {
                    System.out.println(cm);
                }
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
