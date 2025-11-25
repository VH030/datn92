package vn.hoang.datn92demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.request.AreaRequestDTO;
import vn.hoang.datn92demo.dto.response.AreaResponseDTO;
import vn.hoang.datn92demo.model.Area;
import vn.hoang.datn92demo.repository.AreaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/areas")
@PreAuthorize("hasRole('ADMIN')")
public class AreaController {

    private final AreaRepository areaRepository;

    public AreaController(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    // 🔍 Lấy danh sách toàn bộ khu vực
    @Operation(summary = "Lấy danh sách tất cả khu vực")
    @GetMapping
    public ResponseEntity<List<AreaResponseDTO>> getAll() {
        List<AreaResponseDTO> dtos = areaRepository.findAll().stream()
                .map(a -> new AreaResponseDTO(a.getId(), a.getCode(), a.getName(), a.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    //  Lấy chi tiết 1 khu vực theo ID
    @Operation(summary = "Lấy thông tin chi tiết khu vực theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<AreaResponseDTO> getById(@PathVariable Long id) {
        Optional<Area> opt = areaRepository.findById(id);
        return opt.map(a -> new AreaResponseDTO(a.getId(), a.getCode(), a.getName(), a.getDescription()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //  Tạo mới 1 khu vực
    @Operation(summary = "Tạo mới khu vực")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AreaRequestDTO dto) {
        // check unique code
        if (dto.getCode() != null && areaRepository.findByCode(dto.getCode()).isPresent()) {
            return ResponseEntity.badRequest().body("code đã tồn tại");
        }

        Area a = new Area();
        a.setCode(dto.getCode());
        a.setName(dto.getName());
        a.setDescription(dto.getDescription());
        Area saved = areaRepository.save(a);
        AreaResponseDTO resp = new AreaResponseDTO(saved.getId(), saved.getCode(), saved.getName(), saved.getDescription());
        return ResponseEntity.ok(resp);
    }

    // ️ Cập nhật thông tin khu vực
    @Operation(summary = "Cập nhật thông tin khu vực")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody AreaRequestDTO dto) {
        Optional<Area> opt = areaRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Area a = opt.get();

        // nếu đổi code, kiểm tra unique (không tính bản ghi hiện tại)
        if (dto.getCode() != null && !dto.getCode().equals(a.getCode())) {
            if (areaRepository.findByCode(dto.getCode()).isPresent()) {
                return ResponseEntity.badRequest().body("code đã tồn tại");
            }
            a.setCode(dto.getCode());
        }

        if (dto.getName() != null) a.setName(dto.getName());
        a.setDescription(dto.getDescription()); // có thể null để clear

        Area saved = areaRepository.save(a);
        AreaResponseDTO resp = new AreaResponseDTO(saved.getId(), saved.getCode(), saved.getName(), saved.getDescription());
        return ResponseEntity.ok(resp);
    }

    //  Xóa khu vực theo ID
    @Operation(summary = "Xóa khu vực theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!areaRepository.existsById(id)) return ResponseEntity.notFound().build();
        areaRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
