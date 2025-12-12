package com.ktb.chatapp.service;

import com.ktb.chatapp.config.properties.S3Properties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

@Service
public class S3Service {
    private static final String IMAGE_FOLDER = "image";
    private static final String FILE_FOLDER = "file";

    private final S3Properties s3Properties;
    private final S3Presigner preSigner;
    private final S3Client s3Client;

    S3Service(S3Properties s3Properties, S3Presigner preSigner, S3Client s3Client) {
        this.s3Properties = s3Properties;
        this.preSigner = preSigner;
        this.s3Client = s3Client;
    }

    public String getFile(String fileId) {
        return getPreSignedUrl(FILE_FOLDER, fileId);
    }


    public String putFile(MultipartFile file, String fileId) throws IOException {
        PutObjectRequest objectRequest = putObjectRequest(file, FILE_FOLDER, fileId);
        s3Client.putObject(objectRequest, RequestBody.fromBytes(file.getBytes()));
        return s3Client.utilities().getUrl(builder -> builder.bucket(s3Properties.bucket()).key(String.join("/", FILE_FOLDER))).toExternalForm();
    }


    public void deleteFile(String fileId) {
        DeleteObjectRequest objectRequest = deleteObjectRequest(FILE_FOLDER, fileId);
        s3Client.deleteObject(objectRequest);
    }

    private String getPreSignedUrl(String folder, String filename) {
        return preSigner
                .presignGetObject(getObjectPresignRequest(folder, filename))
                .url()
                .toString();
    }

    private GetObjectPresignRequest getObjectPresignRequest(String folder, String filename) {
        return GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(1))
                .getObjectRequest(objectRequest ->
                        objectRequest
                                .bucket(s3Properties.bucket())
                                .key(String.join("/", folder, filename)))
                .build();
    }

    private PutObjectRequest putObjectRequest(MultipartFile uploadFile, String folder, String filename) {
        return PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(String.join("/", folder, filename))
                .contentType(uploadFile.getContentType())
                .contentLength(uploadFile.getSize())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
    }


    private DeleteObjectRequest deleteObjectRequest(String folder, String filename) {
        return DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(String.join("/", folder, filename))
                .build();
    }
}
