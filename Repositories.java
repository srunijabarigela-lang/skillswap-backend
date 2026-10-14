package com.skillswap.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
interface StudentRepo extends JpaRepository<Student,Long>{Optional<Student> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email);}
interface SkillRepo extends JpaRepository<SkillListing,Long>{List<SkillListing> findByCategoryIgnoreCase(String category); List<SkillListing> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);}
interface SessionRepo extends JpaRepository<LearningSession,Long>{List<LearningSession> findByLearnerIdOrTeacherIdOrderByIdDesc(Long learnerId,Long teacherId);}
interface MessageRepo extends JpaRepository<ChatMessage,Long>{List<ChatMessage> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderBySentAtAsc(Long a,Long b,Long c,Long d);}
interface PointsTransactionRepo extends JpaRepository<PointsTransaction,Long>{List<PointsTransaction> findByStudentIdOrderByCreatedAtDesc(Long studentId);}
interface DailyCheckinRepo extends JpaRepository<DailyCheckin,Long>{boolean existsByStudentIdAndCheckinDate(Long studentId, LocalDate date); List<DailyCheckin> findByStudentIdAndCheckinDateBetween(Long studentId, LocalDate start, LocalDate end);}
