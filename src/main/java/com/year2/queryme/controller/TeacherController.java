package com.year2.queryme.controller;

import com.year2.queryme.model.Teacher;
import com.year2.queryme.repository.TeacherRepository;
import com.year2.queryme.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherRepository teacherRepository;

    @PostMapping("/register")
    public Teacher register(@RequestBody Map<String, String> data) {
        return teacherService.registerTeacher(
                data.get("email"),
                data.get("password"),
                data.get("fullName")
        );
    }

    @PutMapping("/{id}")
    public Teacher update(@PathVariable Long id, @RequestBody Map<String, String> data) {
        return teacherService.updateProfile(id, data);
    }

    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepository.findAll();
    }
}
