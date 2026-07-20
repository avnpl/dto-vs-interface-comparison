package com.example.devicememory.controller;

import com.example.devicememory.dto.DeviceDTO;
import com.example.devicememory.projection.DeviceProjection;
import com.example.devicememory.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repeatedly invokes the projection queries and retains every returned list
 * in memory until the request completes. This inflates the heap so that
 * heap dumps taken during the request are interesting to analyze.
 */
@RestController
@RequestMapping("/api/stress")
@RequiredArgsConstructor
@Slf4j
public class StressController {

    private final DeviceService deviceService;

    @GetMapping("/interface/{count}")
    public Map<String, Object> stressInterfaceProjection(@PathVariable int count) {
        log.info("Stress test with interface projection, iterations: {}", count);
        List<List<DeviceProjection>> retained = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            retained.add(deviceService.getDevicesUsingInterfaceProjection());
        }
        return summary("interface-projection", retained.size(), totalRecords(retained));
    }

    @GetMapping("/dto/{count}")
    public Map<String, Object> stressDtoProjection(@PathVariable int count) {
        log.info("Stress test with DTO projection, iterations: {}", count);
        List<List<DeviceDTO>> retained = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            retained.add(deviceService.getDevicesUsingDTOProjection());
        }
        return summary("dto-projection", retained.size(), totalRecords(retained));
    }

    private long totalRecords(List<? extends List<?>> lists) {
        long total = 0;
        for (List<?> list : lists) {
            total += list.size();
        }
        return total;
    }

    private Map<String, Object> summary(String mode, int iterations, long totalRecords) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("mode", mode);
        summary.put("iterations", iterations);
        summary.put("totalRecordsRetained", totalRecords);
        return summary;
    }
}
