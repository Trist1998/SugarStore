package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.api.carbbuilder.payload.CarbBuilderRequest;
import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccess;
import com.uct.carbbuilder.model.build.PdbBuildAccessService;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@RestController
@RequestMapping(path = "/carbbuilder")
public class BuildRequestController
{
    @Autowired
    private PdbBuildAccessService pdbBuildAccess;

    @Autowired
    private PdbEntryAccessService pdbEntryAccess;

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
        Optional<PdbBuild> optionalPdbBuild = pdbBuildAccess.findByBuildHash(buildHash);
        PdbBuild build;
        if(!optionalPdbBuild.isPresent())
        {
            build = pdbBuildAccess.save(new PdbBuild(request, carbBuilderVersion));

            build.setBuildInProgress();

            pdbBuildAccess.save(build);

            try
            {
                CarbBuilderProcessManager manager = new CarbBuilderProcessManager(build, pdbBuildAccess, pdbEntryAccess, carbBuilderFileLocation, onLinux);
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
           build = optionalPdbBuild.get();
        }

        return ResponseEntity.accepted().body(build.getBuildHash());

    }

}
