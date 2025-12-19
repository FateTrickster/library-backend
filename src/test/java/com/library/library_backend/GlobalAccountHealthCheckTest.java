package com.library.library_backend;

import com.library.library_backend.controller.TeacherController;
import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootTest
class GlobalAccountHealthCheckTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherController teacherController;

    // Retrieve maintenance key from config to bypass the lock
    @Value("${app.maintenance.key:}")
    private String maintenanceKey;

    @Test
    void checkAllAccountsLoginStatus() {
        System.out.println("\n================ [GLOBAL ACCOUNT HEALTH CHECK STARTED] ================");
        
        // 1. Fetch all records
        List<Teacher> allTeachers = teacherRepository.findAll();
        System.out.println("DATA: Total Database Records: " + allTeachers.size());

        // 2. Group by Phone (as one person might have multiple records/certificates)
        Map<String, List<Teacher>> phoneMap = allTeachers.stream()
                .filter(t -> t.getPhone() != null) // Filter out dirty data
                .collect(Collectors.groupingBy(t -> t.getPhone().trim()));

        System.out.println("DATA: Unique Phone Numbers (Users): " + phoneMap.size());
        System.out.println("------------------------------------------------------");

        List<String> failedAccounts = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 3. Iterate and Test
        int index = 0;
        for (String phone : phoneMap.keySet()) {
            index++;
            List<Teacher> records = phoneMap.get(phone);
            
            // Strategy: Try logging in with the password from ANY of the user's records.
            // If ALL records fail to login, then the account is considered 'broken'.
            boolean loginSuccess = false;
            String failReason = "Password mismatch for all records";

            for (Teacher t : records) {
                try {
                    // Construct Mock Request
                    Map<String, String> request = new HashMap<>();
                    request.put("phone", phone);
                    // Self-check: use the password stored in DB
                    request.put("password", t.getPassword()); 
                    
                    // Inject Secret Key to bypass 403 Maintenance Lock
                    if (maintenanceKey != null && !maintenanceKey.isEmpty()) {
                        request.put("secretKey", maintenanceKey);
                    } else {
                        request.put("secretKey", "admin666"); // Fallback key
                    }

                    // Direct Controller Call
                    ResponseEntity<?> response = teacherController.login(request);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        loginSuccess = true;
                        break; // Success! Move to next user.
                    } else {
                        // Capture specific error message
                        Map<?, ?> body = (Map<?, ?>) response.getBody();
                        if (body != null && body.containsKey("message")) {
                            failReason = body.get("message").toString();
                        }
                    }
                } catch (Exception e) {
                    failReason = "System Exception: " + e.getMessage();
                }
            }

            // 4. Statistics
            if (loginSuccess) {
                successCount.incrementAndGet();
                if (index % 100 == 0) {
                    System.out.println("PROGRESS: Checked " + index + " accounts...");
                }
            } else {
                failCount.incrementAndGet();
                String errorMsg = String.format("FAILURE: Account [%s] | Records: %d | Reason: %s", phone, records.size(), failReason);
                System.err.println(errorMsg);
                failedAccounts.add(errorMsg);
            }
        }

        // 5. Final Report
        System.out.println("\n================ [HEALTH CHECK REPORT] ================");
        System.out.println("SUCCESS COUNT : " + successCount.get());
        System.out.println("FAILURE COUNT : " + failCount.get());
        System.out.println("FAILURE RATE  : " + String.format("%.2f", (failCount.get() * 100.0 / phoneMap.size())) + "%");
        
        if (!failedAccounts.isEmpty()) {
            System.out.println("\n[FAILURE LIST (Top 20)]:");
            failedAccounts.stream().limit(20).forEach(System.out::println);
            if (failedAccounts.size() > 20) {
                System.out.println("... (See logs above for more failures)");
            }
        } else {
            System.out.println("PERFECT! All accounts are healthy and can login.");
        }
        System.out.println("=======================================================");
    }
}