package com.lifeshop.controller;

import com.lifeshop.common.BusinessException;
import com.lifeshop.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传（评价晒图、商品图都用它）
 * 文件落到 lifeshop.upload.dir 目录，通过 /uploads/** 静态访问
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final String[] ALLOWED = {".jpg", ".jpeg", ".png", ".webp", ".gif"};

    @Value("${lifeshop.upload.dir:./uploads}")
    private String uploadDir;

    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot).toLowerCase(Locale.ROOT);
        boolean allowed = false;
        for (String item : ALLOWED) {
            if (item.equals(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new BusinessException("只支持 jpg / png / webp / gif 格式");
        }

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        // 必须用绝对路径：MultipartFile#transferTo 对相对路径会解析到 Tomcat 临时目录
        File dir = new File(new File(uploadDir).getAbsoluteFile(), month);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("上传目录创建失败");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File target = new File(dir, fileName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("图片保存失败：" + e.getMessage());
        }
        Map<String, String> data = new HashMap<>();
        data.put("url", "/uploads/" + month + "/" + fileName);
        return Result.success("上传成功", data);
    }
}
