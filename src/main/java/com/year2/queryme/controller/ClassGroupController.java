package com.year2.queryme.controller;

import com.year2.queryme.model.ClassGroup;
import com.year2.queryme.repository.ClassGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/class-groups")
public class ClassGroupController {

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @PostMapping
    public ClassGroup create(@RequestBody ClassGroup classGroup) {
        return classGroupRepository.save(classGroup);
    }

    @GetMapping
    public List<ClassGroup> getAll() {
        return classGroupRepository.findAll();
    }

    @GetMapping("/course/{courseId}")
    public List<ClassGroup> getByCourse(@PathVariable Long courseId) {
        return classGroupRepository.findByCourseId(courseId);
    }
}
