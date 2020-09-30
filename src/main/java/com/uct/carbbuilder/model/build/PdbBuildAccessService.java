package com.uct.carbbuilder.model.build;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PdbBuildAccessService
{
    @Autowired
    private PdbBuildAccess pdbBuildAccess;

    public Optional<PdbBuild> findByBuildHash(String buildHash)
    {
        return pdbBuildAccess.findByHash(buildHash);
    }

    public PdbBuild save(PdbBuild build)
    {
        return pdbBuildAccess.save(build);
    }
}
