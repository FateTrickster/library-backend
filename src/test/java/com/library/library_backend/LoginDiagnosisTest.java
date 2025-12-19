package com.library.library_backend;

import com.library.library_backend.controller.TeacherController;
import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class LoginDiagnosisTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherController teacherController;

    @Value("${app.maintenance.enabled:false}")
    private boolean maintenanceMode;

    // 🔥🔥🔥 ENTER YOUR CREDENTIALS HERE 🔥🔥🔥
    private final String TEST_PHONE = "15725666312"; 
    private final String TEST_PASSWORD = "017641"; 

    @Test
    void diagnoseLoginProblem() {
        System.out.println("\n================ [LOGIN DIAGNOSIS START] ================");
        System.out.println("Target Phone: [" + TEST_PHONE + "]");
        System.out.println("Target Pass : [" + TEST_PASSWORD + "]");

        // 1. Environment Check
        System.out.println("\n--- [Step 1] Environment Check ---");
        System.out.println("Maintenance Mode (app.maintenance.enabled): " + maintenanceMode);
        if (maintenanceMode) {
            System.err.println("WARNING: System is in MAINTENANCE MODE. Logins might be blocked.");
        } else {
            System.out.println("OK: System is running normally.");
        }

        // 2. Database Check
        System.out.println("\n--- [Step 2] Database Record Check ---");
        List<Teacher> teachers = teacherRepository.findAllByPhone(TEST_PHONE.trim());
        
        if (teachers.isEmpty()) {
            System.err.println("CRITICAL ERROR: No record found in database for phone [" + TEST_PHONE + "]");
            System.out.println("   -> Action: Check your database connection URL in application.yml.");
            System.out.println("   -> Action: Verify if the user actually exists in the 'user_teacher' table.");
            return; // Stop execution
        }

        System.out.println("OK: Found " + teachers.size() + " record(s). Analyzing...");

        // 3. Password Simulation
        System.out.println("\n--- [Step 3] Password Matching Simulation ---");
        boolean passwordMatchFound = false;

        for (int i = 0; i < teachers.size(); i++) {
            Teacher t = teachers.get(i);
            String dbPassword = t.getPassword();
            String dbName = t.getName();
            Long dbId = t.getId();

            System.out.println(String.format("   >> Record #%d (ID: %d, Name: %s):", i + 1, dbId, dbName));
            
            if (dbPassword == null) {
                System.err.println("      ERROR: Password in DB is NULL! Login impossible.");
                continue;
            }

            System.out.println("      DB Password   : [" + dbPassword + "]");
            System.out.println("      Input Password: [" + TEST_PASSWORD + "]");

            // Check for hidden whitespace
            if (dbPassword.trim().length() != dbPassword.length()) {
                System.err.println("      WARNING: DB password contains hidden WHITESPACE! (Length: " + dbPassword.length() + ")");
            }

            if (dbPassword.equals(TEST_PASSWORD)) {
                System.out.println("      SUCCESS: Password matches!");
                passwordMatchFound = true;
            } else {
                System.err.println("      FAILURE: Password does NOT match.");
            }
        }

        if (!passwordMatchFound) {
            System.err.println("\nCONCLUSION: User exists, but password mismatch for all records.");
        } else {
            System.out.println("\nCONCLUSION: Data verification passed. Proceeding to controller...");
        }

        // 4. Controller Invocation
        System.out.println("\n--- [Step 4] Actual Controller Invocation ---");
        try {
            Map<String, String> request = new HashMap<>();
            request.put("phone", TEST_PHONE);
            request.put("password", TEST_PASSWORD);

            ResponseEntity<?> response = teacherController.login(request);
            
            System.out.println("HTTP Status Code: " + response.getStatusCode());
            System.out.println("Response Body   : " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("\n[FINAL RESULT] SUCCESS: Login API returned 200 OK.");
            } else {
                System.err.println("\n[FINAL RESULT] FAILURE: Login API rejected the request.");
            }
        } catch (Exception e) {
            System.err.println("EXCEPTION: Controller threw an error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("================ [DIAGNOSIS END] ===================");
    }
}