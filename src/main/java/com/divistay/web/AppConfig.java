package com.divistay.web;

import com.divistay.catalog.HotelCatalog;
import com.divistay.domain.RoomFactory;
import com.divistay.hotel.SimulatedHotelPms;
import com.divistay.inventory.RoomInventory;
import com.divistay.notify.NotificationService;
import com.divistay.notify.OutboxNotificationService;
import com.divistay.payment.MockPayClient;
import com.divistay.payment.MockPayGatewayAdapter;
import com.divistay.payment.PaymentGateway;
import com.divistay.repo.BookingGroupRepository;
import com.divistay.repo.HotelRepository;
import com.divistay.repo.InMemoryBookingGroupRepository;
import com.divistay.repo.InMemoryHotelRepository;
import com.divistay.service.BookingService;
import com.divistay.service.BookingSettings;
import com.divistay.service.GroupLifecycleService;
import com.divistay.service.HotelSearch;
import com.divistay.split.SplitStrategyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Wires the plain-Java core together. The core has no Spring dependencies, which keeps it
 * easy to test and lets each piece (payment provider, hotel system, storage) be swapped here.
 */
@Configuration
public class AppConfig {


    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Asia/Jerusalem"));
    }

    @Bean
    BookingSettings bookingSettings(@Value("${divistay.base-url}") String baseUrl,
                                    @Value("${divistay.payment-window-minutes:30}") long windowMinutes) {
        return new BookingSettings(Duration.ofMinutes(windowMinutes), 5, Duration.ofSeconds(30), baseUrl);
    }

    @Bean
    RoomFactory roomFactory() {
        return new RoomFactory();
    }

    /** Real hotels from OpenStreetMap; their rooms, prices and reviews are simulated (see HotelCatalog). */
    @Bean
    HotelRepository hotelRepository(RoomFactory rooms) {
        InMemoryHotelRepository repo = new InMemoryHotelRepository();
        HotelCatalog.load(rooms).forEach(repo::save);
        return repo;
    }

    @Bean
    HotelSearch hotelSearch(HotelRepository hotels, RoomInventory inventory, Clock clock) {
        return new HotelSearch(hotels, inventory, clock);
    }

    @Bean
    BookingGroupRepository bookingGroupRepository() {
        return new InMemoryBookingGroupRepository();
    }

    @Bean
    RoomInventory roomInventory() {
        return new RoomInventory();
    }

    @Bean
    SplitStrategyFactory splitStrategyFactory() {
        return new SplitStrategyFactory();
    }

    @Bean
    MockPayClient mockPayClient() {
        return new MockPayClient();
    }

    @Bean
    PaymentGateway paymentGateway(MockPayClient client) {
        return new MockPayGatewayAdapter(client);
    }

    @Bean
    SimulatedHotelPms hotelPms() {
        return new SimulatedHotelPms();
    }

    @Bean
    NotificationService notificationService() {
        return new OutboxNotificationService();
    }

    @Bean
    GroupLifecycleService groupLifecycleService(BookingGroupRepository groups, HotelRepository hotels,
                                                RoomInventory inventory, PaymentGateway gateway,
                                                SimulatedHotelPms pms, NotificationService notifications,
                                                BookingSettings settings, Clock clock) {
        return new GroupLifecycleService(groups, hotels, inventory, gateway, pms, notifications, settings, clock);
    }

    @Bean
    BookingService bookingService(HotelRepository hotels, BookingGroupRepository groups, RoomInventory inventory,
                                  SplitStrategyFactory splits, PaymentGateway gateway,
                                  NotificationService notifications, GroupLifecycleService lifecycle,
                                  BookingSettings settings, Clock clock) {
        return new BookingService(hotels, groups, inventory, splits, gateway, notifications, lifecycle, settings, clock);
    }
}
