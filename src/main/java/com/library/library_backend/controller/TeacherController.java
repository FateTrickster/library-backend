package com.library.library_backend.controller;

import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.HashMap;

@RestController
@RequestMapping("/teacher")
@CrossOrigin
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    // ✅ 配置 OSS 根路径 (根据你的 Bucket 设置)
    private static final String OSS_BASE_URL = "https://yangteam-files.oss-cn-hangzhou.aliyuncs.com/";

    // 正则表达式常量
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern IDCARD_PATTERN = Pattern.compile("(^\\d{15}$)|(^\\d{18}$)|(^\\d{17}(\\d|X|x)$)");

    // 维护模式开关
    @Value("${app.maintenance.enabled:false}")
    private boolean maintenanceMode;

    @Value("${app.maintenance.key:}")
    private String maintenanceKey;

    // ==========================================
    // 1. 登录接口 (核心接口)
    // 功能：验证账号，返回用户信息 + OSS 图片/PDF 链接
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        // 1. 维护模式校验
        if (maintenanceMode) {
            String inputKey = loginRequest.get("secretKey");
            if (inputKey == null || !inputKey.equals(maintenanceKey)) {
                return ResponseEntity.status(403).body(Collections.singletonMap("message", "系统正在维护中..."));
            }
        }

        String phone = loginRequest.get("phone");
        String password = loginRequest.get("password");

        // 2. 基础校验
        if (phone == null || password == null) {
            return ResponseEntity.badRequest().body("账号或密码不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "账号格式不正确"));
        }

        // 3. 查库逻辑
        String cleanPhone = phone.trim();
        List<Teacher> teachers = teacherRepository.findAllByPhone(cleanPhone);
        
        if (teachers == null || teachers.isEmpty()) {
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "账号或密码错误"));
        }

        // 4. 验证密码 (找到匹配的一条)
        Teacher matched = null;
        for (Teacher t : teachers) {
            if (t.getPassword() != null && t.getPassword().equals(password)) {
                matched = t;
                break;
            }
        }
        
        if (matched == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "账号或密码错误"));
        }

        // ================== 🔥 核心修改：只拼链接，不生成文件 ==================
        
        // A. 获取身份证 (对应文件名中的 ID)
        String idCard = matched.getIdCard();

        // B. 处理期数 (数据库存的是"第七期"，OSS文件名用的是"7")
        // 逻辑：如果包含 "8" 则是第8期，否则默认第7期 (根据你的实际情况调整)
        String batch = "7"; 
        if (matched.getSessions() != null) {
             if (matched.getSessions().contains("8")) {
                 batch = "8";
             } else if (matched.getSessions().contains("7")) {
                 batch = "7";
             }
        }

        // C. 拼接 OSS 永久链接
        // 规则: 根路径 + 目录 + 批次_身份证_后缀
        String imgUrl = OSS_BASE_URL + "preview/" + batch + "_" + idCard + "_img.png";
        String pdfUrl = OSS_BASE_URL + "certs/" + batch + "_" + idCard + "_pdf.pdf";

        // ===================================================================

        // 5. 返回结果
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("msg", "登录成功");
        resp.put("user", matched);
        resp.put("imgUrl", imgUrl); // 前端直接展示
        resp.put("pdfUrl", pdfUrl); // 前端直接下载

        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 2. 找回账号接口 (保留)
    // ==========================================
    @PostMapping("/findAccount")
    public String findAccount(@RequestBody Map<String, String> request) {
        if (maintenanceMode) {
            String inputKey = request.get("secretKey");
            if (inputKey == null || !inputKey.equals(maintenanceKey)) {
                throw new RuntimeException("系统维护中");
            }
        }

        String name = request.get("name");
        String idCard = request.get("idCard");

        if (name == null || idCard == null) {
            throw new RuntimeException("输入不能为空");
        }
        
        if (!IDCARD_PATTERN.matcher(idCard.trim()).matches()) {
            throw new RuntimeException("身份证号码格式不正确");
        }

        List<Teacher> teachers = teacherRepository.findByNameAndIdCard(name.trim(), idCard.trim());

        if (teachers == null || teachers.isEmpty()) {
            throw new RuntimeException("未找到匹配的教师信息");
        }

        return teachers.get(0).getPhone();
    }
    
    // ❌ 已删除 previewCertificate 方法 (不再需要在服务器生成预览)
    // ❌ 已删除 downloadCertificate 方法 (不再需要在服务器生成PDF)
}