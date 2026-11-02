package com.divistay.repo;

import com.divistay.domain.Hotel;

import java.util.List;
import java.util.Optional;

public interface HotelRepository {

    void save(Hotel hotel);

    Optional<Hotel> findById(String id);

    List<Hotel> findAll();

    List<Hotel> findByCity(String city);
}
