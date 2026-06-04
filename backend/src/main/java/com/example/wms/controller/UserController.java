package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.common.BizException;
import com.example.wms.domain.AppUser;
import com.example.wms.dto.WmsDtos.LoginRequest;
import com.example.wms.repository.AppUserRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AppUserRepository userRepository;

    public UserController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<AppUser>> list() {
        return ApiResponse.ok(userRepository.findAll());
    }

    @PostMapping
    public ApiResponse<AppUser> create(@RequestBody AppUser user) {
        return ApiResponse.ok(userRepository.save(user));
    }

    @PostMapping("/login")
    public ApiResponse<AppUser> login(@RequestBody LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BizException("账号或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled()) || !String.valueOf(user.getPassword()).equals(request.password())) {
            throw new BizException("账号或密码错误");
        }
        user.setPassword(null);
        return ApiResponse.ok(user);
    }

    @PutMapping("/{id}")
    public ApiResponse<AppUser> update(@PathVariable Long id, @RequestBody AppUser user) {
        user.setId(id);
        return ApiResponse.ok(userRepository.save(user));
    }
}
