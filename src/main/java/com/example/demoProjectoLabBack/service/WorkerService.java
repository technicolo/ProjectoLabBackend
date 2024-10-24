package com.example.demoProjectoLabBack.service;

import com.example.demoProjectoLabBack.model.dto.*;
import com.example.demoProjectoLabBack.persistance.entities.Job;
import com.example.demoProjectoLabBack.persistance.entities.Rating;
import com.example.demoProjectoLabBack.persistance.entities.User;
import com.example.demoProjectoLabBack.persistance.entities.WorkerProfile;
import com.example.demoProjectoLabBack.persistance.repository.JobRepository;
import com.example.demoProjectoLabBack.persistance.repository.UserRepository;
import com.example.demoProjectoLabBack.persistance.repository.WorkerRepository;
import jakarta.transaction.Transactional;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkerService {


    private static final Logger logger = LoggerFactory.getLogger(WorkerService.class);


    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateUserToWorker(String userId, WorkerForCreationDto request) {
        // Fetch the user by ID from MongoDB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create or update the WorkerProfile for the user
        WorkerProfile workerProfile = new WorkerProfile();
        workerProfile.setUser(user);
        workerProfile.setDescription(request.getDescription());
        workerProfile.setDni(request.getDni());
        workerProfile.setDireccion(request.getDireccion());
        workerProfile.setRating(request.getRating());
        workerProfile.setImageUrl(request.getImageUrl());



        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getId() == null) {
            throw new RuntimeException("The Job retrieved from the database has a null ID");
        }

        workerProfile.addJob(job);




        // Save the updated WorkerProfile and Job association
        workerRepository.save(workerProfile);

        job.getWorkerProfiles().add(workerProfile);
        jobRepository.save(job);

    }


    public List<WorkerProfile> getWorkerProfilesByJobId(String jobId) {
        logger.debug("Fetching WorkerProfiles for Job ID: {}", jobId);
        List<WorkerProfile> profiles = workerRepository.findByJobs_Id(jobId);
        logger.debug("Found WorkerProfiles: {}", profiles);
        return profiles;
    }

    public List<WorkerProfileDto> findAllWorkers() {
        List<WorkerProfile> workers = workerRepository.findAll();
        if (workers == null || workers.isEmpty()) {
            // Log or handle the case where no workers are found
            return Collections.emptyList();
        }
        return workers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }



    public WorkerProfileDto getWorkerProfileByUserId(String userId) {
        WorkerProfile workerProfile = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Worker profile not found for user ID: " + userId));

        // Convert WorkerProfile to WorkerProfileDto (assuming you have a conversion method)
        return convertToDto(workerProfile);
    }


    private WorkerProfileDto convertToDto(WorkerProfile workerProfile) {
        if (workerProfile == null) {
            return null; // or throw an exception
        }

        User user = workerProfile.getUser();
        UserDto userDto = (user != null) ? new UserDto(user.getId(), user.getUsername(), user.getName(), user.getLastname(), user.getEmail()) : null;

        List<String> jobTitles = workerProfile.getJobs() != null ?
                workerProfile.getJobs().stream()
                        .filter(Objects::nonNull) // Ensure Job is not null
                        .map(Job::getTitle)
                        .collect(Collectors.toList()) : Collections.emptyList();

        return new WorkerProfileDto(workerProfile.getId(),
                workerProfile.getDescription(),
                workerProfile.getDni(),
                workerProfile.getDireccion(),
                workerProfile.getRating(),
                workerProfile.getImageUrl(),
                userDto,
                jobTitles);
    }


    public void rateWorker(String workerId, RatingDto ratingDto, String userId) {
        //logger.info("Rating worker with ID: {}", workerId);
        //logger.info("Rating DTO: Rating - {}, Comment - {}", ratingDto.getRating(), ratingDto.getComment());
        //logger.info("Rated by user ID: {}", userId);

        // Fetch the WorkerProfile by workerId
        Optional<WorkerProfile> workerProfileOptional = workerRepository.findById(workerId);

        if (workerProfileOptional.isPresent()) {
            WorkerProfile workerProfile = workerProfileOptional.get();

            // Fetch the user who made the rating
            Optional<User> ratedByUserOptional = userRepository.findById(userId);
            if (!ratedByUserOptional.isPresent()) {
                //logger.error("User with ID: {} not found", userId);
                throw new RuntimeException("User not found for rating");
            }
            User ratedByUser = ratedByUserOptional.get();

            boolean hasAlreadyRated = workerProfile.getComments().stream()
                    .anyMatch(rating -> rating.getRatedBy().getId().equals(userId));

            if (hasAlreadyRated) {
                throw new RuntimeException("You have already rated this worker.");
            }

            // Add the rating and comment
            Rating rating = new Rating();
            rating.setRating(ratingDto.getRating());
            rating.setComment(ratingDto.getComment());
            rating.setRatedBy(ratedByUser);

            //logger.info("Adding rating: {}", rating.getRating());

            workerProfile.addRating(rating);

            // Save the updated profile
            workerRepository.save(workerProfile);
            //logger.info("Worker profile updated with new rating.");
        } else {
            //logger.error("Worker with ID: {} not found", workerId);
            throw new RuntimeException("Worker not found");
        }
    }


    public List<RatingResponseDto> getWorkerComments(String workerId) {
        Optional<WorkerProfile> workerProfileOptional = workerRepository.findById(workerId);

        if (workerProfileOptional.isPresent()) {
            WorkerProfile workerProfile = workerProfileOptional.get();
            List<RatingResponseDto> ratingDtos = new ArrayList<>();

            for (Rating rating : workerProfile.getComments()) {
                RatingResponseDto ratingDto = new RatingResponseDto();
                ratingDto.setId(rating.getId());  // Set the rating ID
                ratingDto.setRating(rating.getRating());
                ratingDto.setComment(rating.getComment());
                ratingDto.setRatedByUserId(rating.getRatedBy().getUsername());  // Optionally include the rater's username
                ratingDtos.add(ratingDto);
            }

            return ratingDtos;
        } else {
            throw new RuntimeException("Worker not found");
        }
    }



    public WorkerProfileDto updateWorkerProfile(String userId, WorkerProfileForEditDto updateData) {
        WorkerProfile workerProfile = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Worker profile not found for user ID: " + userId));

        // Update fields of the WorkerProfile from updateData
        workerProfile.setDescription(updateData.getDescription());;
        workerProfile.setDireccion(updateData.getDireccion());
        workerProfile.setImageUrl(updateData.getImageUrl);


        // Save updated WorkerProfile
        WorkerProfile updatedProfile = workerRepository.save(workerProfile);

        return convertToDto(updatedProfile);  // Return updated DTO
    }


    public void deleteWorkerComment(String workerId, String commentId) {
        // Fetch the worker profile by ID
        Optional<WorkerProfile> workerProfileOptional = workerRepository.findById(workerId);

        if (workerProfileOptional.isPresent()) {
            WorkerProfile workerProfile = workerProfileOptional.get();

            // Find and remove the rating based on commentId
            workerProfile.getComments().removeIf(rating -> rating.getId().equals(commentId));

            // Save the updated profile after removal
            workerRepository.save(workerProfile);
        } else {
            throw new RuntimeException("Worker not found");
        }
    }



}
