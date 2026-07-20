package com.example.devicememory.dto;

import lombok.Getter;

/**
 * Class-based DTO projection.
 *
 * Instantiated directly by Hibernate via a JPQL constructor expression
 * (SELECT new ...). Each row becomes one plain Java object with four
 * String references — no proxies, no backing maps.
 */
@Getter
public class DeviceDTO {

    private final String make;
    private final String model;
    private final String esimCompatibility;
    private final String fivegCompatibility;

    public DeviceDTO(String make, String model, String esimCompatibility, String fivegCompatibility) {
        this.make = make;
        this.model = model;
        this.esimCompatibility = esimCompatibility;
        this.fivegCompatibility = fivegCompatibility;
    }
}
