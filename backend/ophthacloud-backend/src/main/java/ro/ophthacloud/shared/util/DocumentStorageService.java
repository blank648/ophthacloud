package ro.ophthacloud.shared.util;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around MinIO client for object storage operations.
 * Used by PdfGenerationService to store and serve generated PDF documents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final MinioClient minioClient;

    /**
     * Uploads data to MinIO. Creates the bucket if it does not exist.
     *
     * @param bucket      the bucket name
     * @param objectPath  the object key (e.g. "{tenantId}/generated-documents/prescriptions/{id}.pdf")
     * @param data        the file bytes
     * @param contentType the MIME type (e.g. "application/pdf")
     * @return the object path that was stored
     */
    public String upload(String bucket, String objectPath, byte[] data, String contentType) {
        try {
            ensureBucketExists(bucket);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());

            log.info("Uploaded object to MinIO: bucket={}, path={}, size={} bytes",
                    bucket, objectPath, data.length);
            return objectPath;
        } catch (Exception e) {
            log.error("Failed to upload to MinIO: bucket={}, path={}", bucket, objectPath, e);
            throw new RuntimeException("MinIO upload failed", e);
        }
    }

    /**
     * Generates a presigned GET URL for downloading an object.
     *
     * @param bucket      the bucket name
     * @param objectPath  the object key
     * @param expiryHours number of hours until the URL expires
     * @return the presigned download URL
     */
    public String generatePresignedUrl(String bucket, String objectPath, int expiryHours) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectPath)
                    .expiry(expiryHours, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: bucket={}, path={}", bucket, objectPath, e);
            throw new RuntimeException("MinIO presigned URL generation failed", e);
        }
    }

    /**
     * Checks whether an object exists in MinIO.
     */
    public boolean objectExists(String bucket, String objectPath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new RuntimeException("MinIO stat failed", e);
        } catch (Exception e) {
            throw new RuntimeException("MinIO stat failed", e);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build());
            log.info("Created MinIO bucket: {}", bucket);
        }
    }
}
