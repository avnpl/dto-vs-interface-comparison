package com.example.devicememory.service;

import com.example.devicememory.dto.DeviceDTO;
import com.example.devicememory.projection.DeviceProjection;
import com.example.devicememory.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public List<DeviceProjection> getDevicesUsingInterfaceProjection() {
        log.info("Starting interface projection request");
        List<DeviceProjection> devices = deviceRepository.findAllProjectedBy();
        log.info("Completed interface projection request, number of records returned: {}", devices.size());
        return devices;
    }

    public List<DeviceDTO> getDevicesUsingDTOProjection() {
        log.info("Starting DTO projection request");
        List<DeviceDTO> devices = deviceRepository.findAllAsDto();
        log.info("Completed DTO projection request, number of records returned: {}", devices.size());
        return devices;
    }
}
