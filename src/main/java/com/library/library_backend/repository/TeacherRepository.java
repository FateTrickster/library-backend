package com.library.library_backend.repository;

import com.library.library_backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    
    // 关键方法：根据手机号查询用户
    // Spring Data JPA 会自动生成 SQL: select * from user_teacher where teacherTel = ?
    Teacher findByPhone(String phone);

    // 【新增】根据 姓名 和 身份证号 查找用户
    // SQL: select * from user_teacher where teacherReal = ? and teacherIDCa = ?
    Teacher findByNameAndIdCard(String name, String idCard);
}