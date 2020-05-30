package liveproject.m2k8s.controller;

import liveproject.m2k8s.model.Profile;
import liveproject.m2k8s.repository.ProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping(path = "/profile")
public class ProfileController {

    private ProfileRepository profileRepository;

    @Value("${images.directory:/tmp}")
    private String uploadFolder;

    @Value("classpath:ghost.jpg")
    private Resource defaultImage;

    @Autowired
    public ProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping(value = "/{username}", produces="application/json")
    public ResponseEntity<Profile> getProfile(@PathVariable String username) {
        log.debug("Reading model for: "+username);
        Optional<Profile> optionalProfile = profileRepository.findByUsername(username);
        if(optionalProfile.isPresent())
        {
            return new ResponseEntity<>(optionalProfile.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @PostMapping(consumes="application/json")
    public ResponseEntity<Profile> createProfile (@Valid @RequestBody Profile newProfile) {
        Profile profile = profileRepository.save(newProfile);
        return new ResponseEntity<>(profile, HttpStatus.CREATED);
    }

    @PutMapping(consumes="application/json")
    @Transactional
    public ResponseEntity<Profile> updateProfile(@Valid @RequestBody Profile toBeUpdatedProfile) {
        Optional<Profile> optionalProfile = profileRepository.findByUsername(toBeUpdatedProfile.getUsername());
        if(!optionalProfile.isPresent())
        {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        Profile existingProfile = optionalProfile.get();
        boolean dirty = false;
        if (!StringUtils.isEmpty(toBeUpdatedProfile.getEmail())
                && !toBeUpdatedProfile.getEmail().equals(existingProfile.getEmail())) {
            existingProfile.setEmail(toBeUpdatedProfile.getEmail());
            dirty = true;
        }
        if (!StringUtils.isEmpty(toBeUpdatedProfile.getFirstName())
                && !toBeUpdatedProfile.getFirstName().equals(existingProfile.getFirstName())) {
            existingProfile.setFirstName(toBeUpdatedProfile.getFirstName());
            dirty = true;
        }
        if (!StringUtils.isEmpty(toBeUpdatedProfile.getLastName())
                && !toBeUpdatedProfile.getLastName().equals(existingProfile.getLastName())) {
            existingProfile.setLastName(toBeUpdatedProfile.getLastName());
            dirty = true;
        }
        if (dirty) {
            profileRepository.save(existingProfile);
        }
        return new ResponseEntity<>(existingProfile, HttpStatus.OK);
    }

    @GetMapping(value = "/{username}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public byte[] displayImage(@PathVariable String username) throws IOException {
        log.debug("Reading image for: "+username);
        InputStream in = null;
        try {
            Optional<Profile> profile = profileRepository.findByUsername(username);
            if ((!profile.isPresent() || StringUtils.isEmpty(profile.get().getImageFileName()))) {
                in = defaultImage.getInputStream();
            } else {
                in = new FileInputStream(profile.get().getImageFileName());
            }
            return IOUtils.toByteArray(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @PostMapping(value = "/{username}/image")
    @Transactional
    public ResponseEntity uploadImage(@PathVariable String username, @RequestParam("file") MultipartFile file) {
        log.debug("Updating image for: "+username);
        if (file.isEmpty()) {
            log.warn("Empty file - please select a file to upload");
            new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        String fileName = file.getOriginalFilename();
        if (!(fileName.endsWith("jpg") || fileName.endsWith("JPG"))) {
            log.warn("JPG files only - please select a file to upload");
            new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        try {
            final String contentType = file.getContentType();
            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            Path path = Paths.get(uploadFolder, username+".jpg");
            Files.write(path, bytes);
            Optional<Profile> profile = profileRepository.findByUsername(username);
            if (!profile.isPresent())
            {
                log.warn(username + " not found");
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
            profile.get().setImageFileName(path.toString());
            profile.get().setImageFileContentType(contentType);
            profileRepository.save(profile.get());
            return new ResponseEntity<>(profile.get(), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

}
