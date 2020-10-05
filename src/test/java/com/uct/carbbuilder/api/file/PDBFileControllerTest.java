package com.uct.carbbuilder.api.file;


import com.uct.carbbuilder.CarbBuilderApplication;
import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccess;
import com.uct.carbbuilder.model.build.PdbBuildAccessService;
import com.uct.carbbuilder.model.pdbmanager.PdbEntry;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccess;
import com.uct.carbbuilder.model.pdbmanager.PdbEntryAccessService;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes= CarbBuilderApplication.class)
@WebMvcTest(PDBFileController.class)
class PDBFileControllerTest
{

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PdbBuildAccessService buildAccessService;

    @Autowired
    private PdbEntryAccessService entryAccessService;

    @MockBean
    private PdbBuildAccess pdbBuildAccess;

    @MockBean
    private PdbEntryAccess pdbEntryAccess;


    @Before
    public void setUp() throws NoSuchAlgorithmException
    {
        PdbBuild build = new PdbBuild("Casper", 1, "1", "CustomDihedral");

        Mockito.when(pdbBuildAccess.findByHash(build.getBuildHash()))
                .thenReturn(java.util.Optional.of(build));

        PdbEntry entry = new PdbEntry();
        Mockito.when(pdbEntryAccess.findByBuildId(build.getId()))
                .thenReturn(java.util.Optional.of(entry));
    }

    @Test
    void getPDBFileDownload() throws Exception
    {
        String buildHash = new PdbBuild("Casper", 1, "1", "CustomDihedral").getBuildHash();
        PdbBuild found = buildAccessService.findByBuildHash(buildHash).get();
        assertEquals(found.getCasperInput(), "Casper");
        mvc.perform(get("/carbbuilder/file/download/pdb/"+buildHash).contentType("application/pdb"))
        .andExpect(status().isOk());
    }

    @Test
    void getPSFFileDownload()
    {
    }

    @Test
    void getFileText()
    {
    }
}