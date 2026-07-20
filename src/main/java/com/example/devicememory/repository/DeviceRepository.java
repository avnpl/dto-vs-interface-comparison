package com.example.devicememory.repository;

import com.example.devicememory.dto.DeviceDTO;
import com.example.devicememory.entity.Device;
import com.example.devicememory.projection.DeviceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * Interface projection: Spring Data returns one dynamic proxy per row.
     */
    List<DeviceProjection> findAllProjectedBy();

    /**
     * Class-based DTO projection: Hibernate calls the DeviceDTO constructor
     * directly for each row.
     */
    @Query("SELECT new com.example.devicememory.dto.DeviceDTO(" +
            "d.make, d.model, d.esimCompatibility, d.fivegCompatibility) FROM Device d")
    List<DeviceDTO> findAllAsDto();
}
