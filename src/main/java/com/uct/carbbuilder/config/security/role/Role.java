package com.uct.carbbuilder.config.security.role;

import javax.persistence.*;

@Entity
public class Role
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    public Role()
    {}

    public Role(ERole name)
    {
        this.name = name;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public ERole getName()
    {
        return name;
    }

    public void setName(ERole name)
    {
        this.name = name;
    }
}
