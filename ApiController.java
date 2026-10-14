package com.skillswap.backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@Validated
public class ApiController {
 final StudentRepo students;
 final SkillRepo skills;
 final SessionRepo sessions;
 final MessageRepo messages;
 final PointsTransactionRepo transactions;
 final DailyCheckinRepo checkins;
 final PasswordEncoder encoder;

 ApiController(StudentRepo s, SkillRepo k, SessionRepo se, MessageRepo m,
               PointsTransactionRepo tr, DailyCheckinRepo ch, PasswordEncoder e) {
  students=s; skills=k; sessions=se; messages=m; transactions=tr; checkins=ch; encoder=e;
 }

 record Auth(@NotBlank @Size(max=100) String name, @Email @NotBlank String email, @Size(min=6,max=100) @NotBlank String password) {}
 record Login(@Email @NotBlank String email, @NotBlank String password) {}
 record Listing(@NotBlank @Size(max=120) String title, @NotBlank String category, @Size(max=2000) String description, Integer pointsPerSession, String teachingFormat) {}
 record SessionReq(Long learnerId, Long listingId, LocalDateTime requestedAt, String message) {}
 record MessageReq(Long senderId, Long recipientId, @NotBlank @Size(max=3000) String body) {}
 record Profile(String name, String email, Integer age, String gender, String college, String course, String yearOfStudy, String city, String bio, String skillsToTeach, String skillsToLearn) {}

 @GetMapping("/health") Map<String,String> health(){ return Map.of("status","ok","app","SkillSwap Backend"); }

 @PostMapping("/auth/register")
 @Transactional
 ResponseEntity<?> register(@Valid @RequestBody Auth a) {
  String email=a.email().trim().toLowerCase(Locale.ROOT);
  if(students.existsByEmailIgnoreCase(email)) return ResponseEntity.status(409).body(Map.of("error","Email already registered"));
  Student s=new Student(); s.name=a.name().trim(); s.email=email; s.passwordHash=encoder.encode(a.password()); s.points=50;
  students.save(s); ledger(s,50,"Welcome bonus — account created");
  return ResponseEntity.status(201).body(publicUser(s));
 }

 @PostMapping("/auth/login")
 ResponseEntity<?> login(@Valid @RequestBody Login a) {
  var f=students.findByEmailIgnoreCase(a.email().trim());
  if(f.isEmpty() || !encoder.matches(a.password(),f.get().passwordHash)) return ResponseEntity.status(401).body(Map.of("error","Invalid email or password"));
  return ResponseEntity.ok(publicUser(f.get()));
 }

 @GetMapping("/students")
 List<Map<String,Object>> allStudents(@RequestParam(required=false) Long excludeId) {
  return students.findAll().stream().filter(s->excludeId==null||!s.id.equals(excludeId)).map(this::publicUser).toList();
 }
 @GetMapping("/students/{id}")
 ResponseEntity<?> student(@PathVariable Long id) {
  return students.findById(id).<ResponseEntity<?>>map(s->ResponseEntity.ok(publicUser(s))).orElseGet(()->ResponseEntity.notFound().build());
 }

 @PutMapping("/students/{id}/profile")
 ResponseEntity<?> profile(@PathVariable Long id,@RequestBody Profile p) {
  var f=students.findById(id); if(f.isEmpty()) return ResponseEntity.notFound().build();
  Student s=f.get();
  if(p.name()!=null&&!p.name().isBlank()) s.name=p.name().trim();
  if(p.email()!=null&&!p.email().isBlank()&&!p.email().equalsIgnoreCase(s.email)) {
   String email=p.email().trim().toLowerCase(Locale.ROOT);
   if(students.existsByEmailIgnoreCase(email)) return ResponseEntity.status(409).body(Map.of("error","Email already registered"));
   s.email=email;
  }
  s.age=p.age(); s.gender=p.gender(); s.college=p.college(); s.course=p.course(); s.yearOfStudy=p.yearOfStudy(); s.city=p.city();
  s.bio=p.bio(); s.skillsToTeach=p.skillsToTeach(); s.skillsToLearn=p.skillsToLearn();
  return ResponseEntity.ok(publicUser(students.save(s)));
 }

 @GetMapping("/skills")
 List<Map<String,Object>> getSkills(@RequestParam(required=false) String category,@RequestParam(required=false) Long teacherId) {
  List<SkillListing> list;
  if(teacherId!=null) list=skills.findByTeacherIdOrderByCreatedAtDesc(teacherId);
  else if(category==null||category.isBlank()) list=skills.findAll();
  else list=skills.findByCategoryIgnoreCase(category);
  return list.stream().map(this::publicSkill).toList();
 }
 @PostMapping("/skills")
 ResponseEntity<?> createSkill(@RequestParam Long teacherId,@Valid @RequestBody Listing a) {
  var f=students.findById(teacherId); if(f.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Unknown teacher"));
  if(a.pointsPerSession()!=null&&(a.pointsPerSession()<1||a.pointsPerSession()>100)) return ResponseEntity.badRequest().body(Map.of("error","Points per session must be between 1 and 100"));
  SkillListing s=new SkillListing(); s.teacher=f.get(); s.title=a.title().trim(); s.category=a.category().trim(); s.description=a.description()==null?"":a.description().trim();
  s.pointsPerSession=a.pointsPerSession()==null?25:a.pointsPerSession(); s.teachingFormat=a.teachingFormat()==null?"Live session":a.teachingFormat();
  return ResponseEntity.status(201).body(publicSkill(skills.save(s)));
 }
 @DeleteMapping("/skills/{id}")
 ResponseEntity<?> deleteSkill(@PathVariable Long id,@RequestParam Long teacherId) {
  var f=skills.findById(id); if(f.isEmpty()) return ResponseEntity.notFound().build();
  if(!f.get().teacher.id.equals(teacherId)) return ResponseEntity.status(403).body(Map.of("error","Only the owner can delete this listing"));
  if(sessions.findByLearnerIdOrTeacherIdOrderByIdDesc(teacherId,teacherId).stream().anyMatch(s->s.listing!=null&&s.listing.id.equals(id)))
   return ResponseEntity.status(409).body(Map.of("error","This skill has session history and cannot be deleted"));
  skills.delete(f.get()); return ResponseEntity.noContent().build();
 }

 @GetMapping("/sessions")
 List<Map<String,Object>> getSessions(@RequestParam Long studentId) {
  return sessions.findByLearnerIdOrTeacherIdOrderByIdDesc(studentId,studentId).stream().map(this::publicSession).toList();
 }
 @PostMapping("/sessions")
 ResponseEntity<?> createSession(@RequestBody SessionReq a) {
  var l=students.findById(a.learnerId()); var sk=skills.findById(a.listingId());
  if(l.isEmpty()||sk.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Unknown learner/listing"));
  if(l.get().id.equals(sk.get().teacher.id)) return ResponseEntity.badRequest().body(Map.of("error","Cannot request your own skill"));
  if(a.requestedAt()!=null&&a.requestedAt().isBefore(LocalDateTime.now().minusMinutes(5))) return ResponseEntity.badRequest().body(Map.of("error","Choose a future date and time"));
  LearningSession s=new LearningSession(); s.learner=l.get(); s.teacher=sk.get().teacher; s.listing=sk.get();
  s.requestedAt=a.requestedAt()==null?LocalDateTime.now():a.requestedAt(); s.message=a.message()==null?"":a.message().trim();
  s.points=sk.get().pointsPerSession; s.status="REQUESTED";
  return ResponseEntity.status(201).body(publicSession(sessions.save(s)));
 }
 @PatchMapping("/sessions/{id}/status")
 @Transactional
 ResponseEntity<?> updateSession(@PathVariable Long id,@RequestParam Long studentId,@RequestParam String status) {
  var f=sessions.findById(id); if(f.isEmpty()) return ResponseEntity.notFound().build();
  LearningSession s=f.get(); String st=status.toUpperCase(Locale.ROOT);
  if(!Set.of("ACCEPTED","REJECTED","COMPLETED","CANCELLED").contains(st)) return ResponseEntity.badRequest().body(Map.of("error","Unsupported session status"));
  if(Set.of("ACCEPTED","REJECTED").contains(st)&&!s.teacher.id.equals(studentId)) return ResponseEntity.status(403).body(Map.of("error","Only the teacher can accept or reject"));
  if(Set.of("COMPLETED","CANCELLED").contains(st)&&!s.learner.id.equals(studentId)) return ResponseEntity.status(403).body(Map.of("error","Only the learner can complete or cancel"));
  if(st.equals("ACCEPTED")||st.equals("REJECTED")) {
   if(!s.status.equals("REQUESTED")) return ResponseEntity.badRequest().body(Map.of("error","Only pending requests can be accepted or rejected"));
   s.status=st;
  } else if(st.equals("CANCELLED")) {
   if(!s.status.equals("REQUESTED")) return ResponseEntity.badRequest().body(Map.of("error","Only pending requests can be cancelled"));
   s.status=st;
  } else {
   if(!s.status.equals("ACCEPTED")) return ResponseEntity.badRequest().body(Map.of("error","The teacher must accept the session before completion"));
   if(s.learner.points < s.points) return ResponseEntity.badRequest().body(Map.of("error","Not enough points to complete this session"));
   s.learner.points-=s.points; s.teacher.points+=s.points;
   ledger(s.learner,-s.points,"Learning session: "+s.listing.title);
   ledger(s.teacher,s.points,"Taught session: "+s.listing.title);
   s.status="COMPLETED";
   students.save(s.learner); students.save(s.teacher);
  }
  return ResponseEntity.ok(publicSession(sessions.save(s)));
 }

 @GetMapping("/messages")
 List<Map<String,Object>> conversation(@RequestParam Long user1,@RequestParam Long user2) {
  return messages.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderBySentAtAsc(user1,user2,user2,user1).stream().map(this::publicMessage).toList();
 }
 @PostMapping("/messages")
 ResponseEntity<?> send(@Valid @RequestBody MessageReq a) {
  if(Objects.equals(a.senderId(),a.recipientId())) return ResponseEntity.badRequest().body(Map.of("error","You cannot message yourself"));
  var s=students.findById(a.senderId()); var r=students.findById(a.recipientId());
  if(s.isEmpty()||r.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Unknown sender/recipient"));
  ChatMessage m=new ChatMessage(); m.sender=s.get(); m.recipient=r.get(); m.body=a.body().trim();
  return ResponseEntity.status(201).body(publicMessage(messages.save(m)));
 }

 @GetMapping("/points/{id}")
 ResponseEntity<?> points(@PathVariable Long id) {
  return students.findById(id).<ResponseEntity<?>>map(s->ResponseEntity.ok(Map.of("studentId",s.id,"points",s.points))).orElseGet(()->ResponseEntity.notFound().build());
 }
 @GetMapping("/points/{id}/transactions")
 ResponseEntity<?> pointTransactions(@PathVariable Long id) {
  if(!students.existsById(id)) return ResponseEntity.notFound().build();
  return ResponseEntity.ok(transactions.findByStudentIdOrderByCreatedAtDesc(id).stream().map(t->Map.of("id",t.id,"delta",t.delta,"reason",t.reason,"createdAt",t.createdAt)).toList());
 }
 @GetMapping("/points/{id}/checkins")
 ResponseEntity<?> getCheckins(@PathVariable Long id) {
  if(!students.existsById(id)) return ResponseEntity.notFound().build();
  LocalDate today=LocalDate.now(), start=today.with(java.time.DayOfWeek.MONDAY), end=start.plusDays(6);
  return ResponseEntity.ok(checkins.findByStudentIdAndCheckinDateBetween(id,start,end).stream()
    .map(c->c.checkinDate.getDayOfWeek().getValue()-1).sorted().toList());
 }

 @PostMapping("/points/{id}/checkin")
 @Transactional
 ResponseEntity<?> checkin(@PathVariable Long id) {
  var f=students.findById(id); if(f.isEmpty()) return ResponseEntity.notFound().build();
  Student s=f.get(); LocalDate today=LocalDate.now();
  if(checkins.existsByStudentIdAndCheckinDate(id,today)) return ResponseEntity.ok(Map.of("alreadyCheckedIn",true,"weeklyBonus",false,"points",s.points,"message","You already checked in today"));
  DailyCheckin c=new DailyCheckin(); c.student=s; c.checkinDate=today; checkins.save(c);
  s.points+=1; ledger(s,1,"Daily learning check-in");
  LocalDate start=today.with(java.time.DayOfWeek.MONDAY), end=start.plusDays(6);
  long count=checkins.findByStudentIdAndCheckinDateBetween(id,start,end).size();
  boolean bonus=false;
  if(count>=7 && transactions.findByStudentIdOrderByCreatedAtDesc(id).stream().noneMatch(t->t.reason.equals("Weekly consistency bonus "+start))) {
   s.points+=25; ledger(s,25,"Weekly consistency bonus "+start); bonus=true;
  }
  students.save(s);
  return ResponseEntity.ok(Map.of("alreadyCheckedIn",false,"weeklyBonus",bonus,"points",s.points,"message",bonus?"Weekly bonus unlocked!":"Check-in complete"));
 }

 private void ledger(Student s,int delta,String reason) {
  PointsTransaction t=new PointsTransaction(); t.student=s; t.delta=delta; t.reason=reason; transactions.save(t);
 }
 Map<String,Object> publicUser(Student s) {
  Map<String,Object> m=new LinkedHashMap<>(); m.put("id",s.id);m.put("name",s.name);m.put("email",s.email);m.put("age",s.age);m.put("gender",s.gender);m.put("college",s.college);m.put("course",s.course);m.put("yearOfStudy",s.yearOfStudy);m.put("city",s.city);m.put("bio",s.bio);m.put("skillsToTeach",s.skillsToTeach);m.put("skillsToLearn",s.skillsToLearn);m.put("points",s.points);return m;
 }
 Map<String,Object> publicSkill(SkillListing s) {
  Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.id);m.put("teacherId",s.teacher.id);m.put("teacherName",s.teacher.name);m.put("title",s.title);m.put("category",s.category);m.put("description",s.description==null?"":s.description);m.put("pointsPerSession",s.pointsPerSession);m.put("teachingFormat",s.teachingFormat==null?"Live session":s.teachingFormat);m.put("createdAt",s.createdAt);return m;
 }
 Map<String,Object> publicSession(LearningSession s) {
  Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.id);m.put("learnerId",s.learner.id);m.put("learnerName",s.learner.name);m.put("teacherId",s.teacher.id);m.put("teacherName",s.teacher.name);m.put("listingId",s.listing==null?null:s.listing.id);m.put("title",s.listing==null?"Skill":s.listing.title);m.put("requestedAt",s.requestedAt);m.put("message",s.message==null?"":s.message);m.put("status",s.status);m.put("points",s.points);return m;
 }
 Map<String,Object> publicMessage(ChatMessage m) {
  return Map.of("id",m.id,"senderId",m.sender.id,"senderName",m.sender.name,"recipientId",m.recipient.id,"recipientName",m.recipient.name,"body",m.body,"sentAt",m.sentAt);
 }
}
