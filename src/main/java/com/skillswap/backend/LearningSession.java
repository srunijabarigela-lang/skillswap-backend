package com.skillswap.backend;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="learning_sessions")
public class LearningSession {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Student learner;
 @ManyToOne(optional=false) public Student teacher;
 @ManyToOne public SkillListing listing;
 public LocalDateTime requestedAt=LocalDateTime.now();
 @Column(length=1000) public String message;
 public String status="REQUESTED"; public Integer points=25;
 public LearningSession(){}
}
