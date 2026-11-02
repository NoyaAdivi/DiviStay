package com.divistay.hotel;

import com.divistay.domain.DateRange;

import java.util.List;

/** The hotel's property management system (PMS), where the final reservation is made. */
public interface HotelPmsClient {

    record ReservationRequest(String groupId, String hotelId, List<String> roomIds, DateRange stay, String guestName) {
    }

    /**
     * @return the hotel's confirmation number
     * @throws PmsUnavailableException if the hotel can't be reached or refuses for now (worth retrying)
     */
    String confirmReservation(ReservationRequest request) throws PmsUnavailableException;

    class PmsUnavailableException extends Exception {
        public PmsUnavailableException(String message) {
            super(message);
        }
    }
}
