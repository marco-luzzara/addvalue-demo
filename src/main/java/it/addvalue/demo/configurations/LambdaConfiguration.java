package it.addvalue.demo.configurations;

import it.addvalue.demo.model.fill.FillInput;
import it.addvalue.demo.model.fill.FillOutput;
import it.addvalue.demo.model.process.ProcessInput;
import it.addvalue.demo.model.process.ProcessOutput;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.nio.spi.s3.config.S3NioSpiConfiguration;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class LambdaConfiguration {
    @Bean
    public Function<FillInput, FillOutput> fill() {
        return (input) -> {
            try (var fs = getNewFileSystem(input.bucketName())) {
                for (var i = 0; i < input.keyCount(); i++) {
                    Files.write(
                            fs.getPath(Integer.toString(i)),
                            new byte[]{(byte) i});
                }

                return new FillOutput(input.bucketName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    public Function<ProcessInput, ProcessOutput> process() {
        return (input) -> {
            try (var fs = getNewFileSystem(input.bucketName())) {
                var keyContent = Files.readAllBytes(fs.getPath(input.key()));

                return new ProcessOutput(new BigInteger(keyContent).intValue() * 2);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private SdkHttpClient getSdkHttpClient() {
        final var attributeMap = AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                .build();
        return new DefaultSdkHttpClientBuilder().buildWithDefaults(attributeMap);
    }

    private FileSystem getNewFileSystem(String bucketName) {
        try {
            return FileSystems.newFileSystem(URI.create("s3x://%s/%s".formatted("localstackmaindemo:4566", bucketName)),
                    new S3NioSpiConfiguration(),
                    Thread.currentThread().getContextClassLoader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
