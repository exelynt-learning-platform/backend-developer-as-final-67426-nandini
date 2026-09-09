package com.example.demo.dto.reservation;

import com.example.demo.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateRequest {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
}