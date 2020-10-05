package com.uct.carbbuilder.api.file;

import com.uct.carbbuilder.CarbBuilderApplication;
import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccessService;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccessService;
import org.json.JSONObject;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CarbBuilderApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
class PDBFileControllerTest
{
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PdbBuildAccessService testPdbBuildService;

    @Autowired
    private PdbEntryAccessService testPdbEntryService;

    @Test
    void getFileText() throws Exception
    {
        PdbBuild build = new PdbBuild("aDMan(1->3)aDMan", 1, "1.0", "");
        mvc.perform(post("/carbbuilder/build")
                .content("{\"casperInput\": \"aDMan(1->3)aDMan\",\"noRepeatingUnits\": 1,\"customDihedral\": \"\"}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted()).andExpect(content().string(build.getBuildHash()));
        Thread.sleep(5000); //May need to be increased for older systems
        String resp = mvc.perform(get("/file/text/" + build.getBuildHash()))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        JSONObject respObj = new JSONObject(resp);

        assertEquals(removeDate(String.valueOf(respObj.get("pdb"))), removeDate(PDBFileController.fileToString("testpdb/test1.pdb")));
    }

    private String removeDate(String pdb)
    {
        String firstRemoved = pdb.substring(pdb.indexOf('\n') + 1);
        return firstRemoved.substring(firstRemoved.indexOf('\n') + 1);
    }

}