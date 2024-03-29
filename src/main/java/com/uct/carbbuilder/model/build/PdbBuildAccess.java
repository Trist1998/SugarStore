package com.uct.carbbuilder.model.build;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PdbBuildAccess extends JpaRepository<PdbBuild, Long>
{
    @Query("SELECT e FROM PdbBuild e WHERE e.casperInput = :casperInput AND e.noRepeatingUnits = :noRepeatingUnits AND e.carbBuilderVersion = :version")
    Optional<PdbBuild> findByCasperInput(@Param("casperInput") String casperInput, @Param("noRepeatingUnits") int noRepeatingUnits, @Param("version") String version);

    @Query("SELECT e FROM PdbBuild e WHERE e.buildHash = :buildHash")
    Optional<PdbBuild> findByHash(@Param("buildHash") String buildHash);
}
