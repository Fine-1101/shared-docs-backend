package org.example.shareddocs.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置类
 */
@Data
@Configuration
public class AliyunOssConfig {
    
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    
    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;
    
    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;
    
    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;
    
    @Value("${aliyun.oss.domain:}")
    private String domain;
    
    /**
     * 创建OSS客户端Bean（带超时和重试配置）
     */
    @Bean
    public OSS ossClient() {
        // 配置客户端参数
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        
        // 设置连接超时时间（毫秒）
        conf.setConnectionTimeout(5000);
        
        // 设置Socket超时时间（毫秒）
        conf.setSocketTimeout(30000);
        
        // 设置最大连接数
        conf.setMaxConnections(100);
        
        // 设置最大错误重试次数
        conf.setMaxErrorRetry(3);
        
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
    }
}
