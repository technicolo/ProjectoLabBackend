package com.example.demoProjectoLabBack.controller;

import com.example.demoProjectoLabBack.Util.JwtUserDetails;
import com.example.demoProjectoLabBack.model.dto.*;
import com.example.demoProjectoLabBack.model.enums.RoleName;
import com.example.demoProjectoLabBack.persistance.entities.Job;
import com.example.demoProjectoLabBack.persistance.entities.Role;
import com.example.demoProjectoLabBack.persistance.entities.User;
import com.example.demoProjectoLabBack.persistance.repository.RoleRepository;
import com.example.demoProjectoLabBack.service.JobService;
import com.example.demoProjectoLabBack.service.UserService;
import com.example.demoProjectoLabBack.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Controller", description = "Admin operations such as user, job, and role management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {


    @Autowired
    private JobService jobService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;



    @PostMapping("/jobs")
    @Operation(summary = "Create a new job")
    public ResponseEntity<Job> createJob(@RequestBody JobDto jobDto) {
        Job createdJob = jobService.createJob(jobDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdJob);
    }


    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Delete a job")
    public ResponseEntity<String> deleteJob(@PathVariable String jobId) {
        jobService.deleteJob(jobId);
        return ResponseEntity.ok("Job deleted successfully");
    }

    @DeleteMapping("/workers/{workerId}/comments/{commentId}")
    @Operation(summary = "Delete a comment from a worker profile")
    public ResponseEntity<String> deleteComment(
            @PathVariable String workerId,
            @PathVariable String commentId) {
        try {
            workerService.deleteWorkerComment(workerId, commentId);
            return ResponseEntity.ok("Comment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting comment: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user by ID")
    public void deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
    }

    @PutMapping("/edit/{id}")
    @Operation(summary = "Update the authenticated user's profile")
    public ResponseEntity<UserDto> editUserProfile(
            @PathVariable String id,
            @RequestBody UserDto updateData) {




        UserDto updatedUser = userService.updateUserProfile(id, updateData);

        return ResponseEntity.ok(updatedUser);
    }


    @PutMapping("worker/{userId}")
    @Operation(summary = "Create a WorkerUser")
    public ResponseEntity<String> updateUserToWorker(
            @PathVariable String userId,
            @RequestBody WorkerForCreationDto request) {
        try {




            workerService.updateUserToWorker(userId, request);


            userService.updateUserRole(userId, RoleName.ROLE_WORKER);

            return ResponseEntity.ok("User updated to worker successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating user: " + e.getMessage());
        }
    }

    @PutMapping("/edit_profile/{userId}")
    @Operation(summary = "Update the authenticated user's worker profile")
    public ResponseEntity<WorkerProfileDto> updateWorkerProfile(
            @PathVariable String userId,
            @RequestBody WorkerProfileForEditDto updateData) {





        WorkerProfileDto updatedProfile = workerService.updateWorkerProfile(userId, updateData);

        return ResponseEntity.ok(updatedProfile);
    }



    @PostMapping("/create-admin")
    @Operation(summary = "Register a new admin")
    public User registerAdminUser(@Validated @RequestBody UserForRegistrationDto userForRegistrationDto) {



        User user = new User();
        user.setUsername(userForRegistrationDto.getUsername());
        user.setName(userForRegistrationDto.getName());
        user.setLastname(userForRegistrationDto.getLastname());
        user.setEmail(userForRegistrationDto.getEmail());


        String encodedPassword = passwordEncoder.encode(userForRegistrationDto.getPassword());
        user.setPassword(encodedPassword);


        Role defaultRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));


        user.setRole(defaultRole);


        return userService.createUser(user);
    }


    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public User registerUser(@Validated @RequestBody UserForRegistrationDto userForRegistrationDto) {

        if (userService.isUsernameTaken(userForRegistrationDto.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }


        User user = new User();
        user.setUsername(userForRegistrationDto.getUsername());
        user.setName(userForRegistrationDto.getName());
        user.setLastname(userForRegistrationDto.getLastname());
        user.setEmail(userForRegistrationDto.getEmail());


        String encodedPassword = passwordEncoder.encode(userForRegistrationDto.getPassword());
        user.setPassword(encodedPassword);


        Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));


        user.setRole(defaultRole);


        return userService.createUser(user);
    }








}

