package com.library.library_backend.controller;

import com.library.library_backend.entity.Teacher;
import com.library.library_backend.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
// import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/teacher") // 接口前缀改为 /teacher
@CrossOrigin // 允许跨域
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    // 登录接口
    // POST /teacher/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        // 1. 打印日志，证明新代码生效了
        System.out.println("========== 正在执行新版 Login 方法 ==========");

        String rawPhone = loginRequest.get("username");
        String rawPassword = loginRequest.get("password");
        
        String phone = (rawPhone != null) ? rawPhone.trim() : "";
        String password = (rawPassword != null) ? rawPassword.trim() : "";

        Teacher teacher = null;
        try {
            // 尝试找人
            teacher = teacherRepository.findByPhone(phone);
        } catch (Exception e) { 
            // 🚨 修改点：捕获 Exception (所有异常)，防止漏网
            e.printStackTrace(); // 打印报错给开发看

            Map<String, String> errorResponse = new HashMap<>();
            
            // 判断是不是“重号”问题
            // 只要报错信息里包含 "unique result" 或 "IncorrectResultSize"，就说明是重号
            if (e.toString().contains("unique result") || e.toString().contains("IncorrectResultSize")) {
                 errorResponse.put("message", "【系统提示】检测到您的手机号绑定了多个账号，系统无法自动识别。请截图此提示，并联系管理员（电话：138-xxxx-xxxx）手动合并数据。");
            } else {
                 // 其他未知错误
                 errorResponse.put("message", "服务器异常: " + e.getMessage());
            }
            
            return ResponseEntity.status(500).body(errorResponse);
        }

        if (teacher == null) {
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("message", "该手机号未注册"));
        }

        if (teacher.getPassword() == null || !teacher.getPassword().equals(password)) {
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("message", "密码错误"));
        }

        teacher.setPassword(null);
        return ResponseEntity.ok(teacher);
    }

    // === 这是一个临时测试接口，测完可以删掉 ===
    // 访问方式: GET http://localhost:8080/teacher/debug
    @GetMapping("/debug")
    public String debug() {
        try {
            // 1. 查出所有数据
            var teachers = teacherRepository.findAll();
            
            if (teachers.isEmpty()) {
                return "数据库是空的！没查到任何人。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<h3>数据库检查报告</h3>");
            sb.append("总记录数: ").append(teachers.size()).append(" 条<br><hr>");

            // 2. 遍历前 10 条
            for (int i = 0; i < Math.min(teachers.size(), 10); i++) {
                var t = teachers.get(i);
                
                // 【关键修复】如果取出来是 null，直接跳过并记录，防止报错
                if (t == null) {
                    sb.append("<span style='color:red;'>第 ").append(i).append(" 条数据是 NULL (读取异常)</span><br><hr>");
                    continue;
                }

                // 安全获取字段 (防止字段本身是 null)
                String name = (t.getName() == null) ? "NULL" : t.getName();
                String phone = (t.getPhone() == null) ? "NULL" : t.getPhone();
                // 注意：这里我们检查的是 password 字段
                String password = (t.getPassword() == null) ? "NULL" : t.getPassword();

                sb.append("<b>索引[").append(i).append("]</b><br>")
                  .append("ID: ").append(t.getId()).append("<br>")
                  .append("姓名 (teacherReal): ").append(name).append("<br>")
                  .append("手机号 (teacherTel): [").append(phone).append("]<br>")
                  .append("密码 (teacherlDNu): [").append(password).append("]<br>")
                  .append("-----------------------<br>");
            }
            
            return sb.toString();
        } catch (Exception e) {
            // 如果还有其他错，直接打印出来，别崩
            e.printStackTrace();
            return "调试接口出错: " + e.getMessage();
        }
    }

    // 【新增】找回账号接口
    // POST /teacher/findAccount
    @PostMapping("/findAccount")
    public String findAccount(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String idCard = request.get("idCard");

        // 1. 查询数据库
        Teacher teacher = teacherRepository.findByNameAndIdCard(name, idCard);

        // 2. 判断结果
        if (teacher == null) {
            throw new RuntimeException("未找到匹配的教师信息，请检查姓名和身份证号是否正确");
        }

        // 3. 返回手机号 (账号)
        return teacher.getPhone();
    }

    // 【新增】下载证书接口
    // GET /teacher/downloadCertificate?phone=138xxxx
    @GetMapping("/downloadCertificate")
    public void downloadCertificate(@RequestParam String phone, HttpServletResponse response) {
        try {
            // 1. 查出用户数据
            Teacher teacher = teacherRepository.findByPhone(phone);
            if (teacher == null) {
                throw new RuntimeException("用户不存在");
            }

            // 2. 准备 Python 脚本需要的参数
            // 假设 python 脚本就在项目根目录下
            String pythonScriptPath = "src\\python\\cert_generator.py"; // 请修改为你脚本的真实路径
            String outputPath = "D:/Projects/library/temp_cert.pdf"; // 临时生成文件的路径

            // 3. 构建命令行命令
            // 格式: python 脚本名 姓名 教师类型 证书编号 成绩等级 输出路径
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python", 
                pythonScriptPath,
                teacher.getName(),
                teacher.getCategory(),      // 对应 teachertype
                teacher.getCertificateNo(), // 对应 certificateno
                teacher.getLevel(),         // 对应 rank (成绩等级: 优秀/合格)
                outputPath
            );
            
            // // 合并错误流，方便调试
            // processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 1. 异步读取标准输出 (Stdout) - 防止缓冲区满导致死锁
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Python Stdout]: " + line);
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }).start();

            // 2. 重点：读取标准错误 (Stderr) - 这里是报错信息的来源
            StringBuilder errorMsg = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Python Stderr]: " + line);
                    errorMsg.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // 根据 Python 返回的错误信息抛出具体异常
                String err = errorMsg.toString();
                if (err.contains("Pillow")) {
                    throw new RuntimeException("服务器缺少图片处理库，请联系管理员");
                } else if (err.contains("模板图片")) {
                    throw new RuntimeException("证书模板图片缺失，无法生成");
                } else if (err.contains("字体")) {
                    throw new RuntimeException("证书字体缺失，无法生成");
                } else {
                    throw new RuntimeException("证书生成失败: " + err);
                }
            }

            // 5. 将生成的文件发送给前端下载
            File file = new File(outputPath);
            if (!file.exists()) {
                throw new RuntimeException("生成的文件不存在");
            }

            // 设置响应头，强制浏览器下载
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(teacher.getName() + "_证书.pdf", "UTF-8"));
            
            // 读取文件流并写入响应
            FileInputStream in = new FileInputStream(file);
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            // (可选) 删除临时文件
            // file.delete();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(500, "下载失败: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    // 【新增】预览证书接口
    // GET /teacher/previewCertificate?phone=138xxxx
    @GetMapping("/previewCertificate")
    public void previewCertificate(@RequestParam String phone, HttpServletResponse response) {
        try {
            Teacher teacher = teacherRepository.findByPhone(phone);
            if (teacher == null) throw new RuntimeException("用户不存在");

            // 1. 获取项目根目录 (D:/Projects/library/library-backend)
            String projectDir = System.getProperty("user.dir");
            
            // 2. 脚本路径
            String pythonScriptPath = projectDir + "/src/python/cert_generator.py";
            
            // 3. 【修正】资源目录 (输入：从这里拿模板和字体)
            // 对应路径: src/python/resources/
            String resourceDir = projectDir + "/src/python/resources/";
            
            // 4. 【修正】输出目录 (输出：生成的图片放这里)
            // 对应路径: src/python/preview_resources/
            String outputDir = projectDir + "/src/python/preview_resources/";
            
            // 自动创建输出目录
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 最终图片路径
            String tempFileName = "preview_" + teacher.getPhone() + "_" + System.currentTimeMillis() + ".png";
            String outputPath = outputDir + tempFileName;

            // --- 调试打印 (在控制台看看路径对不对) ---
            System.out.println("脚本路径: " + pythonScriptPath);
            System.out.println("资源输入: " + resourceDir);
            System.out.println("图片输出: " + outputPath);
            // -------------------------------------

            ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pythonScriptPath,
                teacher.getName(), teacher.getCategory(),
                teacher.getCertificateNo(), teacher.getLevel(),
                outputPath, resourceDir
            );
            
            Process process = processBuilder.start();

            // 读取标准日志 (UTF-8)
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = inputReader.readLine()) != null) {
                System.out.println("[Python Info]: " + line);
            }

            // 读取错误日志 (UTF-8)
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            StringBuilder errorMsg = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                System.err.println("🚨 [Python Error]: " + line); 
                errorMsg.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python脚本执行失败: \n" + errorMsg.toString());
            }

            File file = new File(outputPath);
            if (!file.exists()) throw new RuntimeException("预览文件未生成: " + outputPath);

            response.setContentType("image/png");
            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }

            // 可选：删除临时文件
            // file.delete();

        } catch (Exception e) {
            e.printStackTrace();
            try { response.sendError(500, "Preview Error: " + e.getMessage()); } catch (IOException ex) {}
        }
    }

    // 【新增】环境自检测试接口
    // GET /teacher/test-env
    @GetMapping("/test-env")
    public String systemHealthCheck() {
        StringBuilder report = new StringBuilder();
        report.append("<h1>系统环境自检报告</h1><hr>");

        // 1. 检查 Python 环境
        report.append("<h3>1. Python 环境检查</h3>");
        try {
            Process process = new ProcessBuilder("python", "--version").start();
            String version = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                report.append("<p style='color:green'>✅ Python 已安装: ").append(version).append("</p>");
            } else {
                report.append("<p style='color:red'>❌ Python 执行失败，请检查环境变量。</p>");
            }
        } catch (Exception e) {
            report.append("<p style='color:red'>❌ 无法调用 python 命令: ").append(e.getMessage()).append("</p>");
        }

        // 2. 检查脚本文件是否存在
        report.append("<h3>2. 脚本文件检查</h3>");
        String scriptPath = "src\\python\\cert_generator.py"; // 你的脚本路径
        File scriptFile = new File(scriptPath);
        if (scriptFile.exists()) {
            report.append("<p style='color:green'>✅ 脚本文件存在: ").append(scriptPath).append("</p>");
        } else {
            report.append("<p style='color:red'>❌ 脚本文件缺失！请确认路径: ").append(scriptPath).append("</p>");
        }

        // 3. 检查资源目录
        report.append("<h3>3. 资源目录检查</h3>");
        String resourceDir = "src\\python\\resources"; // 你的资源路径
        File resDir = new File(resourceDir);
        if (resDir.exists() && resDir.isDirectory()) {
             report.append("<p style='color:green'>✅ 资源目录存在</p>");
             // 检查关键文件
             String[] requiredFiles = {"SIMLI.TTF", "timesbd.ttf", "潍坊-优秀.png"}; // 举例
             for (String f : requiredFiles) {
                 if (new File(resDir, f).exists()) {
                     report.append("<p style='color:green'>&nbsp;&nbsp;&nbsp;&nbsp;✅ 发现文件: ").append(f).append("</p>");
                 } else {
                     report.append("<p style='color:red'>&nbsp;&nbsp;&nbsp;&nbsp;❌ 缺失文件: ").append(f).append("</p>");
                 }
             }
        } else {
            report.append("<p style='color:red'>❌ 资源目录不存在: ").append(resourceDir).append("</p>");
        }

        // 4. 模拟一次真实调用 (冒烟测试)
        report.append("<h3>4. 模拟生成测试</h3>");
        try {
            String testOutput = "D:/Projects/library/test_cert.png";
            ProcessBuilder pb = new ProcessBuilder(
                "python", scriptPath,
                "测试用户", "潍坊市参培教师", "TEST-001", "优秀", testOutput
            );
            pb.redirectErrorStream(true); // 把错误合并到输出流一起看
            Process p = pb.start();
            
            // 读取所有输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "GBK")); // 注意编码
            StringBuilder outputLog = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputLog.append(line).append("<br>");
            }
            int code = p.waitFor();

            if (code == 0) {
                report.append("<p style='color:green'>✅ 模拟生成成功！</p>");
                report.append("<p>脚本输出日志: <br><pre>").append(outputLog).append("</pre></p>");
            } else {
                report.append("<p style='color:red'>❌ 模拟生成失败，退出码: ").append(code).append("</p>");
                report.append("<p>错误日志: <br><pre style='color:red'>").append(outputLog).append("</pre></p>");
            }
        } catch (Exception e) {
             report.append("<p style='color:red'>❌ 测试过程抛出异常: ").append(e.getMessage()).append("</p>");
        }

        return report.toString();
    }
}