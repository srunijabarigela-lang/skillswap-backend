package com.skillswap.backend;
import jakarta.persistence.*;
@Entity @Table(name="students")
public class Student {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String name;
 @Column(nullable=false,unique=true) public String email;
 @Column(nullable=false) public String passwordHash;
 public Integer age; public String gender, college, course, yearOfStudy, city;
 @Column(length=1000) public String bio, skillsToTeach, skillsToLearn;
 @Column(nullable=false) public Integer points=50;
 public Student(){}
}
