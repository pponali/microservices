package com.services.integrations.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Page extends Audit{


    @Column(nullable = false)
    String title;
    @Column(nullable = false)
    String description;
    @OneToMany
    List<Slot> slots;

    @Column(nullable = false)
    String channel;

    @ElementCollection
    @CollectionTable(name = "page_slot_mapping",
            joinColumns = {@JoinColumn(name = "page_id", referencedColumnName = "id")})
    @MapKeyColumn(name ="slot_id")
    @Column(name = "associatedSlots")
    Map<String, String> pageSlots;

}
