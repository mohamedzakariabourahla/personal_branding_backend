package saas.personal_branding.api.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saas.personal_branding.api.application.service.UserService;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.presentation.dto.request.UserRequest;
import saas.personal_branding.api.presentation.dto.response.UserResponse;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest userRequest) {
        User user = userService.creatUser(userRequest.getName(), userRequest.getEmail(), userRequest.getPassword());
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getFullName(), user.getEmail()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(e -> ResponseEntity.ok(new UserResponse(e.getId(), e.getFullName(), e.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(e -> new UserResponse(e.getId(), e.getFullName(), e.getEmail()))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest loginRequest) {
        User user = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        UserResponse response = new UserResponse(user.getId(), user.getFullName(), user.getEmail());
        return ResponseEntity.ok(response);
    }
}
