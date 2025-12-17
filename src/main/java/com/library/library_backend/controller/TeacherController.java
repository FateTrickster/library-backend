package com.library.library_backend.controller;

import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
@CrossOrigin
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    // ==========================================
    // 1. 登录接口 (升级版)
    // 解决“查出两条记录导致无法登录”的问题
    // 返回：List<Teacher> (包含该手机号下的所有期数记录)
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String phone = loginRequest.get("username");
        String password = loginRequest.get("password"); // 身份证后六位

        if (phone == null) return ResponseEntity.badRequest().body("手机号不能为空");

        // 🔥 关键修改：调用 findAllByPhone 获取列表，而不是报错
        List<Teacher> teachers = teacherRepository.findAllByPhone(phone.trim());

        if (teachers == null || teachers.isEmpty()) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "该手机号未注册"));
        }

        // 🔥 密码验证逻辑：
        // 因为是同一个人，理论上所有记录的密码（身份证后六位）都一样。
        // 我们只要发现其中任意一条记录密码匹配，就允许登录。
        boolean passwordMatch = false;
        for (Teacher t : teachers) {
            String dbPwd = t.getPassword();
            // 防止数据库里密码是 null 导致报错
            if (dbPwd != null && dbPwd.equals(password)) {
                passwordMatch = true;
                break;
            }
        }

        if (!passwordMatch) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "密码错误"));
        }

        // 登录成功！直接返回列表给前端
        // 前端会收到类似: [{id:33, sessions:"第七期"...}, {id:315, sessions:"第八期"...}]
        return ResponseEntity.ok(teachers);
    }

    // ==========================================
    // 2. 预览证书接口 (升级版)
    // 参数改动：必须接收 id (Long)，因为手机号不再唯一
    // ==========================================
    @GetMapping("/previewCertificate")
    public void previewCertificate(@RequestParam Long id, HttpServletResponse response) {
        try {
            // 通过 ID 精准查找唯一的一条记录
            Teacher teacher = teacherRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("未找到该证书记录"));

            String projectDir = System.getProperty("user.dir");
            String pythonScriptPath = projectDir + "/src/python/cert_generator.py";
            String resourceDir = projectDir + "/src/python/resources/";
            String outputDir = projectDir + "/src/python/preview_resources/";
            
            File dir = new File(outputDir);
            if (!dir.exists()) dir.mkdirs();

            String tempFileName = "preview_" + teacher.getId() + "_" + System.currentTimeMillis() + ".png";
            String outputPath = outputDir + tempFileName;

            ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pythonScriptPath,
                teacher.getName(), 
                teacher.getCategory(),
                teacher.getCertificateNo(), 
                teacher.getLevel(),
                teacher.getSessions(), // 👈 新增：传入期数 (例如 "第八期")
                outputPath, 
                resourceDir
            );
            
            Process process = processBuilder.start();
            if (process.waitFor() != 0) {
                 throw new RuntimeException("证书生成失败，请检查后台日志");
            }

            File file = new File(outputPath);
            if (!file.exists()) throw new RuntimeException("预览文件未生成");

            response.setContentType("image/png");
            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
            }
            // file.delete(); 

        } catch (Exception e) {
            e.printStackTrace();
            try { response.sendError(500, "Preview Error: " + e.getMessage()); } catch (IOException ex) {}
        }
    }

    // ==========================================
    // 3. 下载证书接口 (文件名带期数 + 不删除文件)
    // ==========================================
    @GetMapping("/downloadCertificate")
    public void downloadCertificate(@RequestParam Long id, HttpServletResponse response) {
        try {
            Teacher teacher = teacherRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("未找到该证书记录"));

            String projectDir = System.getProperty("user.dir");
            String pythonScriptPath = projectDir + "/src/python/cert_generator.py";
            String resourceDir = projectDir + "/src/python/resources/";
            
            // 统一放在 preview_resources 目录下，保持整洁
            String outputDir = projectDir + "/src/python/preview_resources/";
            File dir = new File(outputDir);
            if (!dir.exists()) dir.mkdirs();

            // 1. 服务器上的物理文件名 (用 ID + 时间戳，防止服务器内部覆盖)
            String tempFileName = "download_" + teacher.getId() + "_" + System.currentTimeMillis() + ".pdf";
            String outputPath = outputDir + tempFileName;

            // 2. 调用 Python 生成
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pythonScriptPath,
                teacher.getName(),
                teacher.getCategory(),
                teacher.getCertificateNo(),
                teacher.getLevel(),
                teacher.getSessions(), // 传入期数
                outputPath,
                resourceDir
            );
            
            if (processBuilder.start().waitFor() != 0) {
                throw new RuntimeException("生成PDF失败");
            }

            File file = new File(outputPath);
            response.setContentType("application/pdf");
            
            // 🔥🔥【核心修改】浏览器下载时的文件名
            // 最终效果：张三_第八期_证书.pdf
            // 做一个非空判断，防止 null
            String sessionName = (teacher.getSessions() != null) ? teacher.getSessions() : "";
            String downloadName = teacher.getName() + "_" + sessionName + "_证书.pdf";
            
            // 使用 URLEncoder 处理中文文件名，防止乱码
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(downloadName, "UTF-8"));
            
            // 3. 发送文件流给前端
            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }
            
            // ⚠️ 注意：这里去掉了 delete() 代码，文件会保留在服务器文件夹里方便检查

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ==========================================
    // 4. 找回账号接口 (升级版)
    // ==========================================
    @PostMapping("/findAccount")
    public String findAccount(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String idCard = request.get("idCard");

        // 🔥 改动：获取列表
        List<Teacher> teachers = teacherRepository.findByNameAndIdCard(name, idCard);

        // 判断列表是否为空
        if (teachers == null || teachers.isEmpty()) {
            throw new RuntimeException("未找到匹配的教师信息，请检查姓名和身份证号是否正确");
        }

        // 既然是同一个人，手机号肯定是一样的，取第一条即可
        return teachers.get(0).getPhone();
    }
}