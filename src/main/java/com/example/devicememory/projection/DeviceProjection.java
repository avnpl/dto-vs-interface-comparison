package com.example.devicememory.projection;

/**
 * Spring Data JPA interface projection.
 *
 * For every row returned, Spring Data creates a JDK dynamic proxy
 * (jdk.proxy / $Proxy classes) backed by a TupleBackedMap. These
 * intermediary objects are what show up in heap dumps.
 */
public interface DeviceProjection {

    String getMake();

    String getModel();

    String getEsimCompatibility();

    String getFivegCompatibility();
}
