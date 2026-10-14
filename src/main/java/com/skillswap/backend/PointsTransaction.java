package com.skillswap.backend;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="points_transactions")
public class PointsTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Student student;
 @Column(nullable=false) public Integer delta;
 @Column(nullable=false,length=250) public String reason;
 public LocalDateTime createdAt=LocalDateTime.now();
 public PointsTransaction(){}
}
