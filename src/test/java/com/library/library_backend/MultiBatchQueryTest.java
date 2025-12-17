package com.library.library_backend;

import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MultiBatchQueryTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void testFindMultiBatchUser() {
        // 输出日志改为英文，防止乱码
        System.out.println("========== Multi-Session Query Test Start ==========");
        
        // ⚠️ 请将此处修改为你数据库中真实存在的、有多条记录的手机号
        String targetPhone = "18952102668"; 
        
        System.out.println("Target Phone: " + targetPhone);
        
        // 调用新接口 findAllByPhone
        List<Teacher> results = teacherRepository.findAllByPhone(targetPhone);
        
        System.out.println("Total records found: " + results.size());
        
        if (results.size() > 0) {
            System.out.println("SUCCESS! Details below:");
            for (Teacher t : results) {
                // 打印关键信息：ID, Name, Session (期数)
                System.out.println("   --------------------------------");
                System.out.println("   ID:      " + t.getId());
                System.out.println("   Name:    " + t.getName());
                // 这里调用 getSessions() 读取 teacherSessions 字段
                System.out.println("   Session: " + t.getSessions()); 
                System.out.println("   CertNo:  " + t.getCertificateNo());
            }
        } else {
            System.err.println("FAILED: No data found.");
            System.err.println("   1. Check if the phone number is correct.");
            System.err.println("   2. Check if the database has imported data.");
        }
        System.out.println("====================================================");
    }
}