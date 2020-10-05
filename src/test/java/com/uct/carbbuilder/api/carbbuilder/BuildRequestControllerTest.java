package com.uct.carbbuilder.api.carbbuilder;

import com.uct.carbbuilder.CarbBuilderApplication;
import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccessService;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccessService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CarbBuilderApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
class BuildRequestControllerTest
{
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PdbBuildAccessService testPdbBuildService;

    @Autowired
    private PdbEntryAccessService testPdbEntryService;


    @Test
    void carbBuilderRequest() throws Exception
    {
        PdbBuild build = new PdbBuild("aDMan(1->3)aDMan", 1, "1.0", "");
        mvc.perform(post("/carbbuilder/build")
                .content("{\"casperInput\": \"aDMan(1->3)aDMan\",\"noRepeatingUnits\": 1,\"customDihedral\": \"\"}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted()).andExpect(content().string(build.getBuildHash()));
        PdbBuild build2 = testPdbBuildService.findByBuildHash(build.getBuildHash()).get();
        assertEquals(build2.getCasperInput(), "aDMan(1->3)aDMan");
        assertTrue(build2.isBuildSuccess() || build2.isBuildInProgress());
        mvc.perform(post("/carbbuilder/build")
                .content("{\"casperInput\": \"aDMan(1->3)aDMan\",\"noRepeatingUnits\": 1,\"customDihedral\": \"\"}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted()).andExpect(content().string(build.getBuildHash()));
        build2 = testPdbBuildService.findByBuildHash(build.getBuildHash()).get();
        assertEquals(build2.getCasperInput(), "aDMan(1->3)aDMan");
        int i = 0;
        while (build2.isBuildInProgress() && i < 10)
        {
            build2 = testPdbBuildService.findByBuildHash(build.getBuildHash()).get();
            if(!build2.isBuildInProgress())
                assertTrue(build2.isBuildSuccess());
            i++;
            Thread.sleep(1000);
        }
    }
}