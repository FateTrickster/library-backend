package com.library.library_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_teacher") // 确认表名是 user_teacher
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 教师名字
    @Column(name = "teacherReal")
    private String name;

    // 2. 教师电话 (账号)
    @Column(name = "teacherTel")
    private String phone;

    // 3. 教师身份证
    @Column(name = "teacherIDCa")
    private String idCard;

    // 4. 教师种类 (注意是 Clas 不是 Class)
    @Column(name = "teacherClas")
    private String category;

    // 5. 成绩
    @Column(name = "teacherGrad")
    private String score;

    // 6. 证书编号
    @Column(name = "teacherCert")
    private String certificateNo;
    
    // 7. 教师等级
    @Column(name = "teacherLeve")
    private String level;

    // 8. 教师身份证后六位 (密码)
    @Column(name = "teacherIDNu")
    private String password; 

    // 9. 证书编号后面的数字
    @Column(name = "teacherCert1")
    private String certSuffix;
    
    // 🔥🔥【新增】对应数据库里的 teacherSessions 字段 (期数)
    @Column(name = "teacherSessions")
    private String sessions; 
}