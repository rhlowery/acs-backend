package com.rhlowery.acs.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "acs_groups")
public class GroupEntity extends PanacheEntityBase {
    @Id
    public String id;
    
    public String name;
    
    @Column(length = 2000)
    public String description;
    
    public String persona;

    public GroupEntity() {}

    public GroupEntity(String id, String name, String description, String persona) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.persona = persona;
    }
}
