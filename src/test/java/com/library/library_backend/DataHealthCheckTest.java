package com.library.library_backend;

import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
class DataHealthCheckTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void analyzeDatabaseHealth() {
        System.out.println("========== DATABASE HEALTH CHECK START ==========");
        
        List<Teacher> allTeachers;
        try {
            allTeachers = teacherRepository.findAll();
        } catch (Exception e) {
            System.err.println("FATAL ERROR: Cannot read from database. " + e.getMessage());
            e.printStackTrace();
            return;
        }

        int totalRecords = allTeachers.size();
        System.out.println("Total records scanned: " + totalRecords);
        System.out.println("-------------------------------------------------");

        // Lists to store log messages
        List<String> errorLogs = new ArrayList<>();
        List<String> warningLogs = new ArrayList<>();
        
        // Statistical Counters
        int countNullEntities = 0;      // Entire row is null
        int countMissingPhone = 0;      // Phone is null/empty
        int countMissingPassword = 0;   // Password is null/empty
        int countMissingIdCard = 0;     // ID Card is null/empty
        int countDuplicatePhone = 0;    // Duplicate phone groups
        int countDuplicateIdCard = 0;   // Duplicate ID Card groups

        // Maps for duplicate detection
        Map<String, List<Long>> phoneMap = new HashMap<>();
        Map<String, List<Long>> idCardMap = new HashMap<>();

        // --- PHASE 1: Row-by-Row Analysis ---
        for (int i = 0; i < totalRecords; i++) {
            Teacher t = allTeachers.get(i);

            // 1. Check if the entire object is null (Ghost Data)
            if (t == null) {
                countNullEntities++;
                errorLogs.add("[CRITICAL] Row Index " + i + ": Entire Entity is NULL (Database mapping error or ghost row)");
                continue; 
            }

            Long id = t.getId();
            String name = (t.getName() == null) ? "Unknown_Name" : t.getName();
            String phone = t.getPhone();
            String idCard = t.getIdCard();
            String password = t.getPassword();

            String ref = "ID " + id + " (" + name + ")";

            // 2. Check Phone (Account ID)
            if (phone == null || phone.trim().isEmpty()) {
                countMissingPhone++;
                errorLogs.add("[ERROR] " + ref + ": Field 'teacherTel' (Phone) is NULL or Empty");
            } else {
                phoneMap.computeIfAbsent(phone.trim(), k -> new ArrayList<>()).add(id);
            }

            // 3. Check Password
            if (password == null || password.trim().isEmpty()) {
                countMissingPassword++;
                errorLogs.add("[ERROR] " + ref + ": Field 'teacherIDNu' (Password) is NULL or Empty");
            }

            // 4. Check ID Card
            if (idCard == null || idCard.trim().isEmpty()) {
                countMissingIdCard++;
                warningLogs.add("[WARN] " + ref + ": Field 'teacherIDCa' (ID Card) is NULL or Empty");
            } else {
                idCardMap.computeIfAbsent(idCard.trim(), k -> new ArrayList<>()).add(id);
            }
        }

        // --- PHASE 2: Duplicate Analysis ---
        
        // Check duplicate phones
        for (Map.Entry<String, List<Long>> entry : phoneMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                countDuplicatePhone++;
                errorLogs.add("[DUPLICATE ERROR] Phone " + entry.getKey() + " is used by IDs: " + entry.getValue());
            }
        }

        // Check duplicate ID cards
        for (Map.Entry<String, List<Long>> entry : idCardMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                countDuplicateIdCard++;
                warningLogs.add("[DUPLICATE WARN] ID Card " + entry.getKey() + " is used by IDs: " + entry.getValue());
            }
        }

        // --- PHASE 3: Output Logs ---
        
        if (!errorLogs.isEmpty()) {
            System.err.println("\n--- DETAILED ERROR LOGS (Action Required) ---");
            for (String log : errorLogs) {
                System.err.println(log);
            }
        }

        if (!warningLogs.isEmpty()) {
            System.out.println("\n--- WARNING LOGS (Review Recommended) ---");
            for (String log : warningLogs) {
                System.out.println(log);
            }
        }

        // --- PHASE 4: Final Summary ---
        System.out.println("\n=================================================");
        System.out.println("             FINAL DIAGNOSTIC SUMMARY            ");
        System.out.println("=================================================");
        System.out.println("Total Rows Scanned      : " + totalRecords);
        System.out.println("Valid Objects           : " + (totalRecords - countNullEntities));
        System.out.println("-------------------------------------------------");
        System.out.println("NULL Row Count          : " + countNullEntities + (countNullEntities > 0 ? "  <-- CRITICAL FIX NEEDED" : "  (OK)"));
        System.out.println("Missing Phone Count     : " + countMissingPhone + (countMissingPhone > 0 ? "  <-- Login Blocker" : "  (OK)"));
        System.out.println("Missing Password Count  : " + countMissingPassword + (countMissingPassword > 0 ? "  <-- Login Blocker" : "  (OK)"));
        System.out.println("Duplicate Phone Sets    : " + countDuplicatePhone + (countDuplicatePhone > 0 ? "  <-- Login Crash Risk" : "  (OK)"));
        System.out.println("Duplicate ID Card Sets  : " + countDuplicateIdCard);
        System.out.println("-------------------------------------------------");
        
        System.out.println("\n>>> RECOMMENDED ACTIONS:");
        
        boolean clean = true;

        if (countNullEntities > 0) {
            clean = false;
            System.out.println("1. [CLEANUP] Execute SQL to remove ghost rows:");
            System.out.println("   DELETE FROM user_teacher WHERE teacherTel IS NULL AND teacherReal IS NULL;");
        }

        if (countDuplicatePhone > 0) {
            clean = false;
            System.out.println("2. [DEDUPLICATE] You have " + countDuplicatePhone + " groups of users with the same phone number.");
            System.out.println("   You must delete the redundant IDs listed in [DUPLICATE ERROR] above.");
        }

        if (countMissingPassword > 0) {
            clean = false;
            System.out.println("3. [UPDATE] Some users have no password. Execute SQL to set default:");
            System.out.println("   UPDATE user_teacher SET teacherIDNu = '123456' WHERE teacherIDNu IS NULL;");
        }

        if (clean) {
            System.out.println("   PASSED! Database integrity looks excellent.");
        }

        System.out.println("=================================================");
    }
}