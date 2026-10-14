package com.skillswap.backend;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="skill_listings")
public class SkillListing {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Student teacher;
 @Column(nullable=false) public String title, category;
 @Column(length=2000) public String description;
 public Integer pointsPerSession=25; public String teachingFormat="Live session";
 public LocalDateTime createdAt=LocalDateTime.now();
 public SkillListing(){}
}
