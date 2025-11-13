//package vn.hoang.datn92demo.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import vn.hoang.datn92demo.model.User;
//import vn.hoang.datn92demo.service.UserService;
//
//@RestController
//@RequestMapping("/api/user")
//@Tag(name = "User", description = "API dành cho user bình thường")
//public class UserController {
//
//    private final UserService userService;
//
//    public UserController(UserService userService) {
//        this.userService = userService;
//    }
//
//    @Operation(summary = "Xem thông tin của chính user theo ID")
//    @GetMapping("/{id}")
//    public ResponseEntity<User> getUser(@PathVariable Long id) {
//        return userService.getUserById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @Operation(summary = "Tìm user theo số điện thoại")
//    @GetMapping("/find")
//    public ResponseEntity<User> findByPhone(@RequestParam String phone) {
//        User user = userService.findByPhone(phone);
//        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
//    }
//}
