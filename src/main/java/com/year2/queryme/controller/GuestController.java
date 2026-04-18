package com.year2.queryme.controller;

import com.year2.queryme.model.Guest;
import com.year2.queryme.repository.GuestRepository;
import com.year2.queryme.service.GuestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/guests")
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
    public Page<Guest> getAll(Pageable pageable) {
        return guestRepository.findAll(pageable);
    }
}
