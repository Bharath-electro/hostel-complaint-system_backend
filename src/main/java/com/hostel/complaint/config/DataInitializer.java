package com.hostel.complaint.config;

import com.hostel.complaint.model.*;
import com.hostel.complaint.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

/** Creates default categories and demo accounts on first start. */
@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository users, CategoryRepository categories, PasswordEncoder encoder) {
        this.users = users; this.categories = categories; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (categories.count() == 0) {
            for (String[] c : List.of(
                    new String[]{"Electricity", "Lights, fans, sockets, wiring"},
                    new String[]{"Water Supply", "Taps, leaks, geysers, drinking water"},
                    new String[]{"Cleanliness", "Rooms, corridors, washrooms, mess"},
                    new String[]{"Furniture", "Beds, cupboards, tables, chairs"},
                    new String[]{"Wi-Fi", "Internet connectivity and speed"},
                    new String[]{"Maintenance", "Doors, windows, plumbing, painting"},
                    new String[]{"Other", "Anything else"})) {
                Category cat = new Category();
                cat.setName(c[0]); cat.setDescription(c[1]);
                categories.save(cat);
            }
        }
        seed("System Admin", "admin@hostel.com", Role.ADMIN, null, null);
        seed("Warden Rao", "warden@hostel.com", Role.WARDEN, "Block A", null);
        seed("Ravi (Maintenance)", "staff@hostel.com", Role.STAFF, "Block A", null);
        seed("Demo Student", "student@hostel.com", Role.STUDENT, "Block A", "204");
    }

    private void seed(String name, String email, Role role, String hostel, String room) {
        if (users.existsByEmail(email)) return;
        User u = new User();
        u.setName(name); u.setEmail(email); u.setRole(role);
        u.setHostelName(hostel); u.setRoomNo(room);
        u.setPassword(encoder.encode("Password@123"));
        users.save(u);
    }
}
