package com.year2.queryme.controller;

import com.year2.queryme.model.Admin;
import com.year2.queryme.repository.AdminRepository;
import com.year2.queryme.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminRepository adminRepository;

    @PostMapping("/register")
    public Admin register(@RequestBody Map<String, String> data) {
        return adminService.registerAdmin(
                data.get("email"),
                data.get("password"),
                data.get("fullName")
        );
    }

    @PutMapping("/{id}")
    public Admin update(@PathVariable Long id, @RequestBody Map<String, String> data) {
        return adminService.updateProfile(id, data);
    }

    @GetMapping
    public List<Admin> getAll() {
        return adminRepository.findAll();
    }
}
