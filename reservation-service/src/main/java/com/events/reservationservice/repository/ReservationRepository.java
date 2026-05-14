package com.events.reservationservice.repository;

import com.events.reservationservice.entity.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserEmail(String userEmail);

    List<Reservation> findByEventId(Long eventId);
}
