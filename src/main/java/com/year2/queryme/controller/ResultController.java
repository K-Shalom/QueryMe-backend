package com.year2.queryme.controller;

import com.year2.queryme.model.dto.StudentExamResultDto;
import com.year2.queryme.model.dto.TeacherDashboardRowDto;
import com.year2.queryme.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    @Autowired
    private ResultService resultService;

    /**
     * ENDPOINT FOR STUDENT PORTAL (Group B)
     * Retrieves results for a specific session, subject to visibility logic.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<StudentExamResultDto> getStudentResults(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(resultService.getResultsForStudent(sessionId));
    }

    /**
     * ENDPOINT FOR TEACHER PORTAL (Group H)
     * Retrieves the full results dashboard for a specific exam.
     * Restricted to users with TEACHER role.
     */
    @GetMapping("/exam/{examId}/dashboard")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<TeacherDashboardRowDto>> getTeacherDashboard(@PathVariable UUID examId) {
        return ResponseEntity.ok(resultService.getResultsForTeacher(examId));
    }
}
