package com.skillswap.backend;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="chat_messages")
public class ChatMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Student sender;
 @ManyToOne(optional=false) public Student recipient;
 @Column(nullable=false,length=3000) public String body;
 public LocalDateTime sentAt=LocalDateTime.now();
 public ChatMessage(){}
}
