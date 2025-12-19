package com.library.library_backend;

import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MultiBatchDiagnosisTest {

    @Autowired
    private TeacherRepository teacherRepository;

    // 🔥 TARGET PHONE NUMBER (Change this if needed) 🔥
    private final String TEST_PHONE = "18952102668"; 

    // Mock OSS Base URL
    private static final String OSS_BASE_URL = "https://yangteam-files.oss-cn-hangzhou.aliyuncs.com/";

    @Test
    void diagnoseUrlGeneration() {
        System.out.println("\n========== [MULTI-BATCH URL GENERATION DIAGNOSIS] ==========");
        System.out.println("Target Phone: " + TEST_PHONE);

        // 1. Query Database
        List<Teacher> teachers = teacherRepository.findAllByPhone(TEST_PHONE.trim());
        
        if (teachers.isEmpty()) {
            System.err.println("FAILURE: No records found for this phone number.");
            return;
        }

        System.out.println("SUCCESS: Found " + teachers.size() + " record(s). Analyzing...\n");

        for (int i = 0; i < teachers.size(); i++) {
            Teacher t = teachers.get(i);
            
            System.out.println("--- Record #" + (i + 1) + " (ID: " + t.getId() + ") ---");
            // Check raw data from DB
            System.out.println("   Raw Sessions: [" + t.getSessions() + "]");
            System.out.println("   ID Card     : [" + t.getIdCard() + "]");

            // 2. Simulate Controller Batch Logic
            String batch = "7"; // Default
            String logicSource = "Default";

            if (t.getSessions() != null) {
                if (t.getSessions().contains("8")) {
                    batch = "8";
                    logicSource = "Matched '8'";
                } else if (t.getSessions().contains("7")) {
                    batch = "7";
                    logicSource = "Matched '7'";
                }
            }

            System.out.println("   Computed Batch: " + batch + " (Source: " + logicSource + ")");

            // 3. Generate URL
            String imgUrl = OSS_BASE_URL + "preview/" + batch + "_" + t.getIdCard() + "_img.png";
            System.out.println("   Generated IMG URL: " + imgUrl);
            System.out.println("----------------------------------------\n");
        }

        // 4. Duplicate Check Logic
        if (teachers.size() > 1) {
            Teacher t1 = teachers.get(0);
            Teacher t2 = teachers.get(1);
            String batch1 = getBatch(t1);
            String batch2 = getBatch(t2);
            
            // If both batch and ID card are the same, the URLs will be identical
            if (batch1.equals(batch2) && t1.getIdCard().equals(t2.getIdCard())) {
                System.err.println("CRITICAL WARNING: DUPLICATE URLS DETECTED!");
                System.err.println("Both records resulted in Batch [" + batch1 + "] with same ID Card.");
                System.err.println("Conclusion: The URLs are identical, so the frontend image will NOT change.");
                System.err.println("Action: Check if 'sessions' field in DB actually differs (e.g. do both say 'Session 7'?).");
            } else {
                System.out.println("SUCCESS: Backend logic is correct. Generated URLs are different.");
            }
        }
    }

    private String getBatch(Teacher t) {
        if (t.getSessions() != null) {
            if (t.getSessions().contains("8")) return "8";
            if (t.getSessions().contains("7")) return "7";
        }
        return "7";
    }
}
