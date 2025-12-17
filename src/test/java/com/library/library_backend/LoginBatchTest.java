package com.library.library_backend;

import com.library.library_backend.controller.TeacherController;
import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest // start Spring context to enable injection of Controller and Repository
class LoginBatchTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherController teacherController;

    @Test
    void testLoginForAllUsers() {
        System.out.println("========== Starting batch login test ==========");

        // 1. fetch all users from database
        List<Teacher> allTeachers = teacherRepository.findAll();
        System.out.println("Total records: " + allTeachers.size());

        int successCount = 0;
        int failCount = 0;
        List<String> errorLogs = new ArrayList<>();

        // 2. iterate each user and simulate login
        // use index-based loop to help locate problematic records
        for (int i = 0; i < allTeachers.size(); i++) {
            Teacher t = allTeachers.get(i);

            // Fix: add null check to avoid NPE
            if (t == null) {
                String error = "ERROR: record [" + i + "] is NULL (data anomaly)";
                System.err.println(error);
                errorLogs.add(error);
                failCount++;
                continue; // skip bad record
            }

            // prepare basic info for logs
            String idStr = (t.getId() != null) ? t.getId().toString() : "NULL_ID";
            String nameStr = (t.getName() != null) ? t.getName() : "Anonymous";
            String phoneStr = (t.getPhone() != null) ? t.getPhone() : "NoPhone";

            String logPrefix = "index[" + i + "] ID[" + idStr + "] name[" + nameStr + "] phone[" + phoneStr + "]";

            try {
                // build simulated request map
                Map<String, String> loginRequest = new HashMap<>();
                // defensive: use empty string if null
                loginRequest.put("username", phoneStr);
                loginRequest.put("password", (t.getPassword() != null) ? t.getPassword() : "");

                // call controller login
                ResponseEntity<?> response = teacherController.login(loginRequest);

                // 3. evaluate result
                if (response.getStatusCode().is2xxSuccessful()) {
                    successCount++;
                } else {
                    failCount++;
                    Map<?, ?> body = (Map<?, ?>) response.getBody();
                    String msg = body != null ? body.get("message").toString() : "unknown error";
                    String error = "FAIL: " + logPrefix + " -> " + msg;
                    System.err.println(error);
                    errorLogs.add(error);
                }

            } catch (Exception e) {
                // capture serious exceptions
                failCount++;
                String error = "EXCEPTION: " + logPrefix + " -> " + e.getMessage();
                System.err.println(error);
                errorLogs.add(error);
                e.printStackTrace();
            }
        }

        // 5. print final report
        System.out.println("\n========== Test report ==========");
        System.out.println("Total: " + allTeachers.size());
        System.out.println("Success: " + successCount);
        System.out.println("Fail: " + failCount);

        if (!errorLogs.isEmpty()) {
            System.out.println("\n--- Failure details (please screenshot) ---");
            for (String log : errorLogs) {
                System.out.println(log);
            }
        }
        System.out.println("==================================");
    }
}
