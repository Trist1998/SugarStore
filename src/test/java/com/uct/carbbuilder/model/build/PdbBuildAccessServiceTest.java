package com.uct.carbbuilder.model.build;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
class PdbBuildAccessServiceTest
{
    @Autowired
    private PdbBuildAccessService testPdbBuildService;

    @MockBean
    private PdbBuildAccess pdbBuildAccess;

    @Before
    public void setUp() throws NoSuchAlgorithmException
    {
        PdbBuild build = new PdbBuild("Casper", 1, "1", "CustomDihedral");

        Mockito.when(pdbBuildAccess.findByHash(build.getBuildHash()))
                .thenReturn(java.util.Optional.of(build));
    }

    @Test
    void findByBuildHash() throws NoSuchAlgorithmException
    {
        PdbBuild testbuild = new PdbBuild("Casper", 1, "1", "CustomDihedral");

        Mockito.when(pdbBuildAccess.findByHash(testbuild.getBuildHash()))
                .thenReturn(java.util.Optional.of(testbuild));

        PdbBuild build = new PdbBuild("Casper", 1, "1", "CustomDihedral");
        if(testPdbBuildService.findByBuildHash(build.getBuildHash()).isPresent())
        {
            PdbBuild found = testPdbBuildService.findByBuildHash(build.getBuildHash()).get();
            assertEquals(found.getCasperInput(), testbuild.getCasperInput());
        }

    }

}