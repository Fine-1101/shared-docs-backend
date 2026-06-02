package org.example.shareddocs.service;

import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.exception.BusinessException;
import org.example.shareddocs.config.AliyunOssConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunOssService {
    
    private final OSS ossClient;
    private final AliyunOssConfig ossConfig;
    
    /**
     * 上传文件到OSS
     *
     * @param file 文件
     * @param objectName 对象名称（路径）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String objectName) {
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            
            // 获取文件大小
            long fileSize = file.getSize();
            log.info("开始上传文件: objectName={}, size={} bytes", objectName, fileSize);
            
            // 上传到OSS
            ossClient.putObject(ossConfig.getBucketName(), objectName, inputStream);
            
            String fileUrl = getFileUrl(objectName);
            log.info("文件上传成功: objectName={}, url={}", objectName, fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("文件上传失败: objectName={}, error={}", objectName, e.getMessage());
            
            // 判断是否为网络异常
            if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                log.warn("网络连接异常，建议检查：1. OSS配置是否正确 2. 网络连接是否稳定 3. Bucket权限设置");
            }
            
            throw new BusinessException("文件上传失败: " + e.getMessage());
        } finally {
            // 关闭输入流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.warn("关闭输入流失败", e);
                }
            }
        }
    }
    
    /**
     * 上传字节数组到OSS
     *
     * @param bytes 文件字节数组
     * @param objectName 对象名称（路径）
     * @return 文件访问URL
     */
    public String uploadBytes(byte[] bytes, String objectName) {
        try {
            InputStream inputStream = new ByteArrayInputStream(bytes);
            ossClient.putObject(ossConfig.getBucketName(), objectName, inputStream);
            
            String fileUrl = getFileUrl(objectName);
            log.info("文件上传成功: objectName={}, url={}", objectName, fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("文件上传失败: objectName={}", objectName, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除OSS文件
     *
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) {
        try {
            ossClient.deleteObject(ossConfig.getBucketName(), objectName);
            log.info("文件删除成功: objectName={}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败: objectName={}", objectName, e);
            // 删除失败不影响主流程，只记录日志
        }
    }
    
    /**
     * 获取文件访问URL
     *
     * @param objectName 对象名称
     * @return 文件访问URL
     */
    public String getFileUrl(String objectName) {
        // 如果配置了自定义域名，使用自定义域名
        if (ossConfig.getDomain() != null && !ossConfig.getDomain().isEmpty()) {
            return ossConfig.getDomain() + "/" + objectName;
        }
        
        // 否则使用默认OSS域名
        return "https://" + ossConfig.getBucketName() + "." + 
               ossConfig.getEndpoint() + "/" + objectName;
    }
    
    /**
     * 生成唯一的对象名称
     *
     * @param originalFilename 原始文件名
     * @return 唯一的对象名称
     */
    public String generateObjectName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
