package com.services.integrations.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NavigationBar extends Audit{

    @Column
    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    List<NavLink> navLinkList;
}
