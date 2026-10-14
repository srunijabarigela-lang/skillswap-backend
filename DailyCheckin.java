package com.skillswap.backend;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity
@Table(name="daily_checkins", uniqueConstraints=@UniqueConstraint(columnNames={"student_id","checkin_date"}))
public class DailyCheckin {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="student_id") public Student student;
 @Column(name="checkin_date",nullable=false) public LocalDate checkinDate;
 public DailyCheckin(){}
}
