package com.uct.carbbuilder.model.build;

import com.uct.carbbuilder.api.carbbuilder.payload.CarbBuilderRequest;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class PdbBuildTest
{

    @Test
    void isValid()
    {
        PdbBuild test = new PdbBuild();
        assertFalse(test.isValid());
        test.setCasperInput("Casper Entered");
        assertTrue(test.isValid());
    }

    @Test
    void testConstructor() throws NoSuchAlgorithmException
    {
        PdbBuild test = new PdbBuild("Casper",1,"1","CustomDihedrals");
        assertEquals(test.getCasperInput(), "Casper");
        assertEquals(test.getNoRepeatingUnits(), 1);
        assertEquals(test.getCarbBuilderVersion(), "1");
        assertEquals(test.getCustomDihedral(), "CustomDihedrals");
        assertEquals(test.getBuildHash(), "23129284042R22125M94p124EB29c41g6139fc0s94k123k15t64");

        CarbBuilderRequest request = new CarbBuilderRequest();
        request.setCasperInput("Casper");
        request.setCustomDihedral("CustomDihedrals");
        request.setNoRepeatingUnits(1);
        test = new PdbBuild(request, "1");
        assertEquals(test.getCasperInput(), "Casper");
        assertEquals(test.getNoRepeatingUnits(), 1);
        assertEquals(test.getCarbBuilderVersion(), "1");
        assertEquals(test.getCustomDihedral(), "CustomDihedrals");
        assertEquals(test.getBuildHash(), "23129284042R22125M94p124EB29c41g6139fc0s94k123k15t64");

    }

    @Test
    void getBuildHash() throws NoSuchAlgorithmException
    {
        String hash1 = PdbBuild.getBuildHash("Casper",1,"1","CustomDihedrals");
        String hash2 = PdbBuild.getBuildHash("Casper1",1,"1","CustomDihedrals");
        String hash3 = PdbBuild.getBuildHash("Casper",2,"1","CustomDihedrals");
        String hash4 = PdbBuild.getBuildHash("Casper",1,"2","CustomDihedrals");
        String hash5 = PdbBuild.getBuildHash("Casper",1,"2","CustomDihedrals");
        String hash6 = PdbBuild.getBuildHash("Casper",1,"2","CustomDihedrals1");
        String hash7 = PdbBuild.getBuildHash("Casper",1,"1","CustomDihedrals");
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        assertNotEquals(hash1, hash4);
        assertNotEquals(hash1, hash5);
        assertNotEquals(hash1, hash6);
        assertEquals(hash1, hash7);
        assertEquals(hash1, "23129284042R22125M94p124EB29c41g6139fc0s94k123k15t64");

    }

    @Test
    void isBuildInProgress()
    {
        PdbBuild test = new PdbBuild();
        assertTrue(test.isBuildInProgress());
        test.setBuildFailed();
        assertFalse(test.isBuildInProgress());
        test.setBuildSuccess();
        assertFalse(test.isBuildInProgress());
        test.setBuildInProgress();
        assertTrue(test.isBuildInProgress());
    }

    @Test
    void isBuildSuccess()
    {
        PdbBuild test = new PdbBuild();
        assertFalse(test.isBuildSuccess());
        test.setBuildFailed();
        assertFalse(test.isBuildSuccess());
        test.setBuildSuccess();
        assertTrue(test.isBuildSuccess());
        test.setBuildInProgress();
        assertFalse(test.isBuildSuccess());
    }

    @Test
    void isBuildFailed()
    {
        PdbBuild test = new PdbBuild();
        assertFalse(test.isBuildFailed());
        test.setBuildFailed();
        assertTrue(test.isBuildFailed());
        test.setBuildSuccess();
        assertFalse(test.isBuildFailed());
        test.setBuildInProgress();
        assertFalse(test.isBuildFailed());
    }
}