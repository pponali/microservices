package com.services.integrations.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name = "nav_link", indexes = @Index(name = "nav_link_idx", columnList = "url", unique = true))
public class NavLink extends Audit {

    @Column
    String url;

    @PrimaryKeyJoinColumn(name = "id")
    @OneToOne
    NavLink navLink;
}
