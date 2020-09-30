package com.uct.carbbuilder.model.pdbmanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PdbEntryAccessService
{
    @Autowired
    private PdbEntryAccess pdbEntryAccess;

    public Optional<PdbEntry> findByBuildId(long id)
    {
        return pdbEntryAccess.findByBuildId(id);
    }

    public void save(PdbEntry entry)
    {
        pdbEntryAccess.save(entry);
    }
}
