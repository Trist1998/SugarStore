package com.uct.carbbuilder.model.pdbmanager;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PdbEntryAccess extends JpaRepository<PdbEntry, Long>
{
    @Query("SELECT e FROM PdbEntry e WHERE e.casperInput = :casperInput AND e.noRepeatingUnits = :noRepeatingUnits AND e.carbBuilderVersion = :version")
    Optional<PdbEntry> findByCasperInput(@Param("casperInput")String casperInput, @Param("noRepeatingUnits")int noRepeatingUnits, @Param("version")String version);

    @Query("SELECT e FROM PdbEntry e WHERE e.buildHash = :buildHash")
    Optional<PdbEntry> findByHash(@Param("buildHash")String buildHash);
}
