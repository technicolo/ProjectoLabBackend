package com.example.demoProjectoLabBack.service;

import com.example.demoProjectoLabBack.model.enums.RoleName;
import com.example.demoProjectoLabBack.persistance.entities.Role;
import com.example.demoProjectoLabBack.persistance.entities.User;
import com.example.demoProjectoLabBack.persistance.repository.RoleRepository;
import com.example.demoProjectoLabBack.persistance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;



    public User createUser(User user) {
        user.setPassword(user.getPassword());
        return userRepository.save(user);
    }

    public User findUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }






    public boolean isUsernameTaken(String username) {
        return userRepository.findByUsername(username).isPresent();
    }





    public List<User> findAllUsersWithUserRole() {

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Role not found"));


        return userRepository.findAllByRoleId(userRole.getId());
    }

    public List<User> findAllUsersWithWorkerRole() {
        Role userRole = roleRepository.findByName(RoleName.ROLE_WORKER)
                .orElseThrow(() -> new RuntimeException("Role not found"));


        return userRepository.findAllByRoleId(userRole.getId());
    }

    public void deleteUser(String id) {
        userRepository.deleteById(String.valueOf(id));
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void updateUserRole(String userId, RoleName roleName) {
        // Fetch the user by ID from MongoDB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch the role by its name (e.g., ROLE_WORKER)
        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        // Set the new role for the user
        user.setRole(newRole);

        // Save the updated user in MongoDB
        userRepository.save(user);
    }

}