package com.services.integrations.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name ="slots")
public class Slot extends Audit{

    @Column(name = "banners")
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<Banner> bannerList;


}
