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



    public boolean isDniTaken(Integer dni) {
        return workerRepository.findByDni(dni).isPresent();
    }

    public boolean isPhoneNumberTaken(long phoneNumber) {
        return workerRepository.findByPhoneNumber(phoneNumber).isPresent();
    }



    @Transactional
    public void updateUserToWorker(String userId, WorkerForCreationDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WorkerProfile workerProfile = new WorkerProfile();
        workerProfile.setUser(user);
        workerProfile.setDescription(request.getDescription());
        workerProfile.setDni(request.getDni());
        workerProfile.setDireccion(request.getDireccion());
        workerProfile.setRating(request.getRating());
        workerProfile.setPhoneNumber(request.getPhoneNumber());



        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));
        workerProfile.addJobId(job.getId());

        workerRepository.save(workerProfile);


        job.getWorkerProfileIds().add(workerProfile.getId());
        jobRepository.save(job);

    }


    public List<WorkerProfileDto> getWorkerProfilesByJobId(String jobId) {
        logger.debug("Fetching WorkerProfiles for Job ID: {}", jobId);
        List<WorkerProfile> profiles = workerRepository.findByJobIdsContaining(jobId);

        if (profiles == null || profiles.isEmpty()) {
            return Collections.emptyList();
        }


        return profiles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<WorkerProfileDto> findAllWorkers() {
        List<WorkerProfile> workers = workerRepository.findAll();
        if (workers == null || workers.isEmpty()) {

            return Collections.emptyList();
        }
        return workers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }



    public WorkerProfileDto getWorkerProfileByUserId(String userId) {
        WorkerProfile workerProfile = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Worker profile not found for user ID: " + userId));


        return convertToDto(workerProfile);
    }


    private WorkerProfileDto convertToDto(WorkerProfile workerProfile) {
        if (workerProfile == null) {
            return null;
        }

        User user = workerProfile.getUser();
        UserDto userDto = (user != null) ? new UserDto(user.getId(), user.getUsername(), user.getName(), user.getLastname(), user.getEmail()) : null;

        List<String> jobTitles = workerProfile.getJobIds().stream()
                .map(jobId -> jobRepository.findById(jobId).map(Job::getTitle).orElse("Unknown Job"))
                .collect(Collectors.toList());

        List<String> imageUrls = (workerProfile.getImageUrls() != null) ? workerProfile.getImageUrls() : new ArrayList<>();


        return new WorkerProfileDto(workerProfile.getId(),
                        workerProfile.getDescription(),
                        workerProfile.getDni(),
                        workerProfile.getDireccion(),
                        workerProfile.getRating(),
                        workerProfile.getImageUrls(),
                        imageUrls,
                        userDto,
                        jobTitles,
                        workerProfile.getPhoneNumber());
    }

    public List<WorkerProfileDto> convertWorkersToDto(List<WorkerProfile> workers) {
        return workers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    public void rateWorker(String workerId, RatingDto ratingDto, String userId) {
        //logger.info("Rating worker with ID: {}", workerId);
        //logger.info("Rating DTO: Rating - {}, Comment - {}", ratingDto.getRating(), ratingDto.getComment());
        //logger.info("Rated by user ID: {}", userId);


        Optional<WorkerProfile> workerProfileOptional = workerRepository.findById(workerId);

        if (workerProfileOptional.isPresent()) {
            WorkerProfile workerProfile = workerProfileOptional.get();


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


            Rating rating = new Rating();
            rating.setRating(ratingDto.getRating());
            rating.setComment(ratingDto.getComment());
            rating.setRatedBy(ratedByUser);

            //logger.info("Adding rating: {}", rating.getRating());

            workerProfile.addRating(rating);


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
                ratingDto.setRatedByUserId(rating.getRatedBy().getUsername());
                ratingDtos.add(ratingDto);
            }

            return ratingDtos;
        } else {
            throw new RuntimeException("Worker not found");
        }
    }

    @Transactional
    public void addImageToWorkerProfile(String userId, String imageUrl) {
        WorkerProfile workerProfile = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        if (workerProfile.getImageUrls().size() >= 3) {
            throw new RuntimeException("Cannot add more than 3 images");
        }

        workerProfile.addImageUrl(imageUrl);
        workerRepository.save(workerProfile);
    }

    @Transactional
    public void removeImageFromWorkerProfile(String userId, String imageUrl) {
        WorkerProfile workerProfile = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        workerProfile.removeImageUrl(imageUrl);
        workerRepository.save(workerProfile);
    }



    public WorkerProfileDto updateWorkerProfile(String userId, WorkerProfileForEditDto updateData) {
        WorkerProfile workerProfile = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Worker profile not found for user ID: " + userId));


        workerProfile.setDescription(updateData.getDescription());;
        workerProfile.setDireccion(updateData.getDireccion());
        workerProfile.setPhoneNumber(updateData.getPhoneNumber());



        WorkerProfile updatedProfile = workerRepository.save(workerProfile);

        return convertToDto(updatedProfile);
    }


    public void deleteWorkerComment(String workerId, String commentId) {

        Optional<WorkerProfile> workerProfileOptional = workerRepository.findById(workerId);

        if (workerProfileOptional.isPresent()) {
            WorkerProfile workerProfile = workerProfileOptional.get();


            Rating ratingToDelete = workerProfile.getComments().stream()
                    .filter(rating -> rating.getId().equals(commentId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Comment not found"));


            workerProfile.getComments().remove(ratingToDelete);


            workerProfile.getRatings().removeIf(ratingScore -> ratingScore == ratingToDelete.getRating());

            workerProfile.updateAverageRating();




            workerRepository.save(workerProfile);
        } else {
            throw new RuntimeException("Worker not found");
        }





    }





}
