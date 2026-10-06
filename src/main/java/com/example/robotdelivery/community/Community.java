package com.example.robotdelivery.community;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "community")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    /** 物业是否要求限制夜间运行 */
    @Column(name = "night_restricted", nullable = false)
    private boolean nightRestricted;

    /** 夜间限制开始时间 HH:mm */
    @Column(name = "night_start", nullable = false, length = 5)
    private String nightStart;

    /** 夜间限制结束时间 HH:mm */
    @Column(name = "night_end", nullable = false, length = 5)
    private String nightEnd;
}
