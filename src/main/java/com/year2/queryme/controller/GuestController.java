package com.year2.queryme.controller;

import com.year2.queryme.model.Guest;
import com.year2.queryme.repository.GuestRepository;
import com.year2.queryme.service.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guests")
public class GuestController {

    @Autowired
    private GuestService guestService;

    @Autowired
    private GuestRepository guestRepository;

    @PostMapping("/register")
    public Guest register(@RequestBody Map<String, String> data) {
        return guestService.registerGuest(
                data.get("email"),
                data.get("password"),
                data.get("fullName")
        );
    }

    @PutMapping("/{id}")
    public Guest update(@PathVariable Long id, @RequestBody Map<String, String> data) {
        return guestService.updateProfile(id, data);
    }

    @GetMapping
    public List<Guest> getAll() {
        return guestRepository.findAll();
    }
}
