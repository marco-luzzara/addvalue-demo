package it.addvalue.demo.configurations;

import it.addvalue.demo.model.fill.FillInput;
import it.addvalue.demo.model.fill.FillOutput;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.AttributeMap;

import java.net.URI;
import java.util.function.Function;

@Configuration
public class LambdaConfiguration {
    @Bean
    public Function<FillInput, FillOutput> fill() {
        return (input) -> {
            try (var s3Client = S3Client.builder()
                    .forcePathStyle(true)
                    .httpClient(getSdkHttpClient())
                    .endpointOverride(URI.create("http://localstackmaindemo:4566"))
                    .build()) {
                for (var i = 0; i < input.keyCount(); i++) {
                    var objectRequest = PutObjectRequest.builder()
                            .bucket(input.bucketName())
                            .key(Integer.toString(i))
                            .build();

                    s3Client.putObject(objectRequest, RequestBody.fromBytes(new byte[] { (byte) i }));
                }
            }

            return new FillOutput(input.bucketName());
        };
    }

    private SdkHttpClient getSdkHttpClient() {
        final var attributeMap = AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                .build();
        return new DefaultSdkHttpClientBuilder().buildWithDefaults(attributeMap);
    }
}
