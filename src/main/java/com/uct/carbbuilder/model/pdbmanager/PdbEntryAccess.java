package com.uct.carbbuilder.model.pdbmanager;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PdbEntryAccess extends JpaRepository<PdbEntry, Long>
{
    @Query("SELECT e FROM PdbEntry e WHERE e.pdbBuildId = :buildId")
    Optional<PdbEntry> findByBuildId(@Param("buildId")long buildId);
}
