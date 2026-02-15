package com.openfolio.skill;

import com.openfolio.portfolio.Portfolio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(nullable = false)
    private String name;

    private String category;

    @Enumerated(EnumType.STRING)
    private Proficiency proficiency;

    @Column(name = "display_order")
    private int displayOrder;
}
