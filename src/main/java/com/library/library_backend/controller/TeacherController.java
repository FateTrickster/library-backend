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
import java.util.concurrent.Semaphore; // 1. 导入这个包
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern; // 导入正则包

@RestController
@RequestMapping("/teacher")
@CrossOrigin
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    // 🔥 2. 定义一个全局“通行证”，只允许 20 个人同时进入生成环节
    // 如果你的服务器配置很高(8核16G)，可以改成 50；如果配置低(1核2G)，建议改 5 或 10
    private static final Semaphore SEMAPHORE = new Semaphore(20);

    // 🟢 定义正则表达式常量 (预编译，提高性能)
    // 手机号正则：1开头，后面跟10位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    // 身份证正则：简单的15或18位校验 (防止输入特殊字符)
    private static final Pattern IDCARD_PATTERN = Pattern.compile("(^\\d{15}$)|(^\\d{18}$)|(^\\d{17}(\\d|X|x)$)");

    // ==========================================
    // 1. 登录接口 (升级版)
    // 解决“查出两条记录导致无法登录”的问题
    // 返回：List<Teacher> (包含该手机号下的所有期数记录)
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String phone = loginRequest.get("username");
        String password = loginRequest.get("password"); // 身份证后六位

        // 🛡️ 防御层 1: 非空检查
        if (phone == null || password == null) {
            return ResponseEntity.badRequest().body("账号或密码不能为空");
        }

        // 去除首尾空格并做格式校验
        String cleanPhone = phone.trim();
        // 🛡️ 防御层 2: 正则格式校验 (关键！)
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "账号格式不正确或包含非法字符"));
        }

        // 只有格式干净的手机号，才允许进入数据库查询
        List<Teacher> teachers = teacherRepository.findAllByPhone(cleanPhone);

        if (teachers == null || teachers.isEmpty()) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "该手机号未注册"));
        }

        boolean passwordMatch = false;
        for (Teacher t : teachers) {
            String dbPwd = t.getPassword();
            if (dbPwd != null && dbPwd.equals(password)) {
                passwordMatch = true;
                break;
            }
        }

        if (!passwordMatch) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "密码错误"));
        }

        return ResponseEntity.ok(teachers);
    }

    // ==========================================
    // 2. 预览证书接口 (升级版)
    // 参数改动：必须接收 id (Long)，因为手机号不再唯一
    // ==========================================
    @GetMapping("/previewCertificate")
    public void previewCertificate(@RequestParam Long id, HttpServletResponse response) {
        File file = null;
        boolean permitAcquired = false; // 标记是否拿到了通行证

        try {
            // 🔥 A. 尝试获取通行证 (和下载接口共用 SEMAPHORE)
            // 设定 15 秒超时：预览一般用户耐心较差，如果 15 秒排不到队，直接提示繁忙
            permitAcquired = SEMAPHORE.tryAcquire(15, TimeUnit.SECONDS);

            if (!permitAcquired) {
                // 如果没抢到，抛出异常，前端会提示“预览失败”或显示繁忙
                throw new RuntimeException("系统繁忙，生成预览需排队，请稍后再试");
            }

            // === 拿到通行证，开始干活 ===

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
                teacher.getSessions(),
                outputPath, 
                resourceDir
            );
            
            Process process = processBuilder.start();
            if (process.waitFor() != 0) {
                 throw new RuntimeException("证书生成失败");
            }

            file = new File(outputPath);
            if (!file.exists()) throw new RuntimeException("预览文件未生成");

            response.setContentType("image/png");
            
            // 发送图片流
            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
            // 预览接口报错时，尝试返回一个错误状态码
            try { response.sendError(503, "Server Busy: " + e.getMessage()); } catch (IOException ex) {}
        } finally {
            // 🔥 B. 归还通行证 (一定要还！)
            if (permitAcquired) {
                SEMAPHORE.release();
            }

            // 🔥 C. 阅后即焚
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    // ==========================================
    // 3. 下载证书接口 (文件名带期数 + 不删除文件)
    // ==========================================
    // ==========================================
    // 3. 下载证书接口 (最终抗压版：限流 + 阅后即焚)
    // ==========================================
    @GetMapping("/downloadCertificate")
    public void downloadCertificate(@RequestParam Long id, HttpServletResponse response) {
        File file = null;
        boolean permitAcquired = false; // 标记是否拿到了通行证
        
        try {
            // 🔥 A. 尝试获取通行证
            // 如果目前已有 20 人在生成，这里会阻塞等待，直到有人释放
            // 设置 30 秒超时，如果 30 秒还排不到队，就报错（防止永久卡死）
            permitAcquired = SEMAPHORE.tryAcquire(30, TimeUnit.SECONDS);
            
            if (!permitAcquired) {
                throw new RuntimeException("服务器繁忙，请稍后重试");
            }

            // === 拿到通行证后，才开始执行下面的重资源操作 ===
            
            Teacher teacher = teacherRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("未找到该证书记录"));

            String projectDir = System.getProperty("user.dir");
            String pythonScriptPath = projectDir + "/src/python/cert_generator.py";
            String resourceDir = projectDir + "/src/python/resources/";
            
            String outputDir = projectDir + "/src/python/preview_resources/";
            File dir = new File(outputDir);
            if (!dir.exists()) dir.mkdirs();

            String tempFileName = "download_" + teacher.getId() + "_" + System.currentTimeMillis() + ".pdf";
            String outputPath = outputDir + tempFileName;

            ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pythonScriptPath,
                teacher.getName(),
                teacher.getCategory(),
                teacher.getCertificateNo(),
                teacher.getLevel(),
                teacher.getSessions(),
                outputPath,
                resourceDir
            );
            
            if (processBuilder.start().waitFor() != 0) {
                throw new RuntimeException("生成PDF失败");
            }

            file = new File(outputPath);
            response.setContentType("application/pdf");
            
            String sessionName = (teacher.getSessions() != null) ? teacher.getSessions() : "";
            String downloadName = teacher.getName() + "_" + sessionName + "_证书.pdf";
            
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(downloadName, "UTF-8"));
            
            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }

        } catch (InterruptedException e) {
            // 线程中断异常
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
            // 如果是下载流错误，通常无法返回 JSON，只能记录日志
        } finally {
            // 🔥 B. 归还通行证 (这一步至关重要！不还的话后面的人永远进不来)
            if (permitAcquired) {
                SEMAPHORE.release();
            }

            // 🔥 C. 阅后即焚
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }
    
    // ==========================================
    // 4. 找回账号接口 (升级版)
    // ==========================================
    @PostMapping("/findAccount")
    public String findAccount(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String idCard = request.get("idCard");

        if (name == null || idCard == null) {
            throw new RuntimeException("输入不能为空");
        }
        
        // 🛡️ 防御层：校验身份证格式
        if (!IDCARD_PATTERN.matcher(idCard.trim()).matches()) {
            throw new RuntimeException("身份证号码格式不正确");
        }

        // 名字虽然难校验正则(可能有生僻字)，但 JPA 底层会处理好转义
        // 我们只需要 trim 一下即可
        List<Teacher> teachers = teacherRepository.findByNameAndIdCard(name.trim(), idCard.trim());

        if (teachers == null || teachers.isEmpty()) {
            throw new RuntimeException("未找到匹配的教师信息");
        }

        return teachers.get(0).getPhone();
    }
}