package com.library.library_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@SpringBootTest
class LibraryBackendApplicationTests {

    @Test
    void debugDirectoryContent() {
        System.out.println("====== 🕵️‍♂️ 目录侦探模式启动 ======");

        String projectDir = System.getProperty("user.dir");
        
        // 1. 检查资源目录
        String resourceDir = projectDir + "/src/python/resources/";
        File resDirFile = new File(resourceDir);
        
        System.out.println("📂 正在检查资源目录: " + resourceDir);
        
        if (!resDirFile.exists()) {
            System.err.println("❌ 目录根本不存在！请检查文件夹是否创建。");
            return;
        } else {
            System.out.println("✅ 目录存在。里面的文件如下：");
            File[] files = resDirFile.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    System.out.println("   📄 " + f.getName());
                }
            } else {
                System.err.println("⚠️ 目录是空的！");
            }
        }
        
        // 2. 尝试运行 Python (让 Python 自己去找中文文件)
        String pythonScriptPath = projectDir + "/src/python/cert_generator.py";
        String outputDir = projectDir + "/src/python/preview_resources/";
        new File(outputDir).mkdirs();
        String outputPath = outputDir + "test_debug_" + System.currentTimeMillis() + ".png";
        
        System.out.println("\n🚀 正在尝试调用 Python 生成图片...");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "python", pythonScriptPath,
                "测试王", "潍坊市参培教师", "TEST-001", "优秀",
                outputPath, resourceDir
            );
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "GBK")); // 防止乱码
            String line;
            while ((line = reader.readLine()) != null) System.out.println("[Python]: " + line);
            
            int exit = p.waitFor();
            System.out.println("🏁 退出码: " + exit);
            
            if (new File(outputPath).exists()) {
                System.out.println("✅ 成功！图片已生成: " + outputPath);
            } else {
                System.err.println("❌ 失败：未生成图片");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}