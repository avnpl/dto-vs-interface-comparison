package com.example.devicememory.controller;

import com.example.devicememory.dto.DeviceDTO;
import com.example.devicememory.projection.DeviceProjection;
import com.example.devicememory.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/interface")
    public List<DeviceProjection> getDevicesUsingInterfaceProjection() {
        return deviceService.getDevicesUsingInterfaceProjection();
    }

    @GetMapping("/dto")
    public List<DeviceDTO> getDevicesUsingDTOProjection() {
        return deviceService.getDevicesUsingDTOProjection();
    }
}
