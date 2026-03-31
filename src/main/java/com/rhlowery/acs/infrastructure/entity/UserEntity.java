package com.rhlowery.acs.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acs_users")
public class UserEntity extends PanacheEntityBase {
    @Id
    public String id;
    
    public String name;
    
    public String email;
    
    public String role;
    
    public String persona;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_groups", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "group_id")
    public List<String> groups = new ArrayList<>();

    public UserEntity() {}

    public UserEntity(String id, String name, String email, String role, List<String> groups, String persona) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.groups = groups != null ? groups : new ArrayList<>();
        this.persona = persona;
    }
}
