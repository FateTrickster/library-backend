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
    // 1. 登录接口 (已修复：支持 7, 8, 9 期)
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        
        // 1. 维护模式校验
        String inputKey = loginRequest.get("secretKey");
        if (maintenanceKey != null && !maintenanceKey.isEmpty()) {
             if (inputKey == null || !inputKey.equals(maintenanceKey)) {
                 return ResponseEntity.status(403).body(Collections.singletonMap("message", "当前系统正在维护中，请输入正确的测试密钥！"));
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

        // 4. 验证密码
        boolean passwordCorrect = false;
        for (Teacher t : teachers) {
            if (t.getPassword() != null && t.getPassword().equals(password)) {
                passwordCorrect = true;
                break;
            }
        }
        
        if (!passwordCorrect) {
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "账号或密码错误"));
        }

        // ================== 🔥 核心修复：添加第 9 期的判断逻辑 🔥 ==================
        
        List<Map<String, Object>> resultList = new java.util.ArrayList<>();

        for (Teacher t : teachers) {
            Map<String, Object> item = new HashMap<>();
            // 复制基础属性
            item.put("id", t.getId());
            item.put("name", t.getName());
            item.put("phone", t.getPhone());
            item.put("idCard", t.getIdCard());
            item.put("category", t.getCategory());
            item.put("score", t.getScore());
            item.put("certificateNo", t.getCertificateNo());
            item.put("level", t.getLevel());
            item.put("sessions", t.getSessions());

            // 🔥🔥🔥 升级后的 Batch 判断逻辑 🔥🔥🔥
            String batch = "7"; // 默认兜底是 7
            String sessions = t.getSessions();
            
            if (sessions != null) {
                // 必须处理乱码或中文情况，同时匹配 "9" 和 "九"
                if (sessions.contains("9") || sessions.contains("九")) {
                    batch = "9";
                } else if (sessions.contains("8") || sessions.contains("八")) {
                    batch = "8";
                } else if (sessions.contains("7") || sessions.contains("七")) {
                    batch = "7";
                }
            }
            
            // 调试日志 (可选，上线可删)
            System.out.println("Processing ID: " + t.getId() + " | Session Raw: " + sessions + " | Result Batch: " + batch);

            // 拼接链接
            String idCard = t.getIdCard();
            String imgUrl = OSS_BASE_URL + "preview/" + batch + "_" + idCard + "_img.png";
            String pdfUrl = OSS_BASE_URL + "certs/" + batch + "_" + idCard + "_pdf.pdf";

            item.put("imgUrl", imgUrl);
            item.put("pdfUrl", pdfUrl);

            resultList.add(item);
        }

        // ===================================================================

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("msg", "登录成功");
        resp.put("userList", resultList); 

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