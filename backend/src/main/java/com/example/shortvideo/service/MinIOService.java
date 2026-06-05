package com.example.shortvideo.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class MinIOService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.url}")
    private String minioUrl;
    
    @Value("${minio.bucket.videos}")
    private String videoBucket;
    
    @Value("${minio.bucket.covers}")
    private String coverBucket;
    
    @Value("${minio.bucket.avatars}")
    private String avatarBucket;
    
    public MinIOService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void ensureBucketsExist() throws Exception {
        createBucketIfMissing(videoBucket);
        createBucketIfMissing(coverBucket);
        createBucketIfMissing(avatarBucket);
    }
    
    public String uploadVideo(MultipartFile file) throws IOException, MinioException, InvalidKeyException, NoSuchAlgorithmException {
        String filename = generateFileName("mp4");
        String bucket = videoBucket;
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        }
        
        return minioUrl + "/" + bucket + "/" + filename;
    }
    
    public String uploadCover(MultipartFile file) throws IOException, MinioException, InvalidKeyException, NoSuchAlgorithmException {
        String filename = generateFileName("jpg");
        String bucket = coverBucket;
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        }
        
        return minioUrl + "/" + bucket + "/" + filename;
    }
    
    private String generateFileName(String extension) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return datePath + "/" + uuid + "." + extension;
    }

    private void createBucketIfMissing(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build()
        );

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }
}
