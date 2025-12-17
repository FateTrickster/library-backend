package com.library.library_backend.repository;

import com.library.library_backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; // 记得导入 List

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    
    // 兼容旧代码的方法 (如果数据有重复，调用这个可能会报错，暂时留着没关系)
    Teacher findByPhone(String phone);

    // 🔥🔥【关键新增】查找该手机号下的“所有”期数记录
    List<Teacher> findAllByPhone(String phone);

    // 🔥🔥【修改】找回账号：可能查出多条记录（多期），所以必须返回 List
    List<Teacher> findByNameAndIdCard(String name, String idCard);
}