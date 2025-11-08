package vn.hoang.datn92demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.request.WaterLevelRequestDTO;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.service.WaterLevelService;
import java.util.List;

@RestController
@RequestMapping("/api/water-levels")
@Tag(name = "Water Level", description = "API giám sát mực nước")
@CrossOrigin(origins = "*")
public class WaterLevelController {

    private final WaterLevelService service;

    public WaterLevelController(WaterLevelService service) {
        this.service = service;
    }

    @Operation(summary = "Lấy danh sách mực nước hiện tại")
    @GetMapping
    public List<WaterLevel> getAll() {
        return service.getAll();
    }


    @Operation(summary = "Thêm dữ liệu mực nước mới")
    @PostMapping
    public WaterLevel addLevel(@Valid @RequestBody WaterLevelRequestDTO dto) {
        return service.save(dto);
    }
}
