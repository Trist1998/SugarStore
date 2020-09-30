package com.uct.carbbuilder.api.file;


import com.uct.carbbuilder.model.build.PdbBuild;
import com.uct.carbbuilder.model.build.PdbBuildAccess;
import com.uct.carbbuilder.model.build.PdbBuildAccessService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;



@RunWith(SpringRunner.class)
@WebMvcTest(PDBFileController.class)
@DataJpaTest
class PDBFileControllerTest
{

    @Autowired
    private TestEntityManager entityManager;


    @Autowired
    private PdbBuildAccessService buildAccessService;

    @MockBean
    private PdbBuildAccess buildAccess;

    @Test
    void getPDBFileDownload() throws NoSuchAlgorithmException
    {
        PdbBuild build = new PdbBuild("Casper", 1, "1", "CustomDihedral");
        entityManager.persist(build);
        entityManager.flush();
        PdbBuild found = buildAccessService.findByBuildHash(build.getBuildHash()).get();
        assertEquals(found.getCasperInput(), "Casper");
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