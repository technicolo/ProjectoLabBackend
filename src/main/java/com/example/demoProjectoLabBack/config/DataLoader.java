package com.example.demoProjectoLabBack.config;


import com.example.demoProjectoLabBack.persistance.entities.User;
import com.example.demoProjectoLabBack.persistance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.demoProjectoLabBack.model.enums.RoleName;
import com.example.demoProjectoLabBack.persistance.entities.Job;
import com.example.demoProjectoLabBack.persistance.entities.Role;
import com.example.demoProjectoLabBack.persistance.repository.JobRepository;
import com.example.demoProjectoLabBack.persistance.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
public class DataLoader {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JobRepository jobRepository;

    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            if (roleRepository.count() == 0) {
                Role userRole = new Role();
                userRole.setName(RoleName.ROLE_USER);
                roleRepository.save(userRole);

                Role adminRole = new Role();
                adminRole.setName(RoleName.ROLE_ADMIN);
                roleRepository.save(adminRole);

                Role workerRole = new Role();
                workerRole.setName(RoleName.ROLE_WORKER);
                roleRepository.save(workerRole);
            }

            if (jobRepository.count() == 0) {
                Job gardenerJob = new Job();
                gardenerJob.setTitle("Gardener");
                gardenerJob.setDescription("Handles all gardening tasks");
                jobRepository.save(gardenerJob);

                Job electricianJob = new Job();
                electricianJob.setTitle("Electrician");
                electricianJob.setDescription("Handles all electrical tasks");
                jobRepository.save(electricianJob);

                Job plumberJob = new Job();
                electricianJob.setTitle("Plumber");
                electricianJob.setDescription("Handles all plumbing tasks");
                jobRepository.save(electricianJob);
            }


            if (userRepository.count() == 0) { // Check if users exist
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setName("admin");
                adminUser.setLastname("hermann");
                adminUser.setEmail("pepito@gmail.com");
                adminUser.setPassword(passwordEncoder.encode("adminadmin")); // Hash the password

                // Get the ADMIN role from the repository
                Optional<Role> adminRoleOptional = roleRepository.findByName(RoleName.ROLE_ADMIN);
                if (adminRoleOptional.isPresent()) {
                    adminUser.setRole(adminRoleOptional.get()); // Set the role if present
                } else {
                    // Handle the case where the role is not found
                    throw new RuntimeException("Admin role not found");
                }

                userRepository.save(adminUser); // Save the user
            }

        };
    }
}
