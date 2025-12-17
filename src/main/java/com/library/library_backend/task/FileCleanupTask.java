package com.library.library_backend.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
public class FileCleanupTask {

    // 这里指向你的临时文件夹路径
    private final String PREVIEW_DIR_PATH = System.getProperty("user.dir") + "/src/python/preview_resources/";

    // ⏰ 设定每 30 分钟干一次活 (30 * 60 * 1000 = 1800000 毫秒)
    @Scheduled(fixedRate = 1800000)
    public void cleanupOldFiles() {
        File dir = new File(PREVIEW_DIR_PATH);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        int count = 0;
        long currentTime = System.currentTimeMillis();
        // 只删除 10 分钟前的文件 (避免误删正在生成的文件)
        long expireTime = 10 * 60 * 1000; 

        for (File file : files) {
            // 只要是 preview_ 或 download_ 开头的临时文件
            if ((file.getName().startsWith("preview_") || file.getName().startsWith("download_")) 
                && (currentTime - file.lastModified() > expireTime)) {
                
                if (file.delete()) {
                    count++;
                }
            }
        }
        
        if (count > 0) {
            System.out.println("🧹 [自动保洁] 已清理 " + count + " 个残留文件");
        }
    }
}
