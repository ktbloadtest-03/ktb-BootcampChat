package com.ktb.chatapp.config.properties;

import org.springframework.beans.factory.annotation.Value;

public record S3Properties(
        String accessKey,
        String secretKey,
        String bucket
) {
}
