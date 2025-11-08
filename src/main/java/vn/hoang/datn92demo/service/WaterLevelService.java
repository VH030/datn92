package vn.hoang.datn92demo.service;

import org.springframework.stereotype.Service;
import vn.hoang.datn92demo.dto.request.WaterLevelRequestDTO;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.repository.WaterLevelRepository;

import java.util.List;

@Service
public class WaterLevelService {

    private final WaterLevelRepository repository;

    public WaterLevelService(WaterLevelRepository repository) {
        this.repository = repository;
    }

    public WaterLevel save(WaterLevelRequestDTO dto) {
        WaterLevel wl = new WaterLevel();
        wl.setLevel(dto.getLevel());
        wl.setTimestamp(dto.getTimestamp());
        return repository.save(wl);
    }

    public List<WaterLevel> getAll() {
        return repository.findAll();
    }
}

