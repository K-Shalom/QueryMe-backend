package com.year2.queryme.controller;

import com.year2.queryme.model.ClassGroup;
import com.year2.queryme.repository.ClassGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/class-groups")
public class ClassGroupController {

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @PostMapping
    public ClassGroup create(@RequestBody ClassGroup classGroup) {
        return classGroupRepository.save(classGroup);
    }

    @GetMapping
    public Page<ClassGroup> getAll(Pageable pageable) {
        return classGroupRepository.findAll(pageable);
    }

    @GetMapping("/course/{courseId}")
    public Page<ClassGroup> getByCourse(@PathVariable Long courseId, Pageable pageable) {
        return classGroupRepository.findByCourseId(courseId, pageable);
    }
}
