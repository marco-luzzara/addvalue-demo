package it.addvalue.demo.testcontainer.containers;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import it.addvalue.demo.helpers.AssertionHelper;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;

public class AppContainer {
    private final ContainerState containerState;
    public final String accessKey = "accesskey";
    public final String secretKey = "secretkey";
    public final int localstackPort = 4566;
    private static final System.Logger LOGGER = System.getLogger(AppContainer.class.getName());
    private static final String DEPLOYMENT_STAGE_NAME = "demo";
    private String restApiId;
    private final LocalstackConfig localstackConfig;

    private static final String GET_LOGS_FROM_CW_SCRIPT_NAME = "aws-get-last-logs.sh";

    public AppContainer(ContainerState containerState)
    {
        this(containerState, new LocalstackConfig(false, "info"));
    }

    public AppContainer(ContainerState containerState, LocalstackConfig localstackConfig)
    {
        this.containerState = containerState;
        this.localstackConfig = localstackConfig;
    }

    /**
     * run the terraform apply to create all the necessary resources
     */
    public void initialize(TerraformContainer terraform) throws IOException, InterruptedException {
        terraform.initialize();
        terraform.apply(new TerraformContainer.TfVariables(
                this.accessKey,
                this.secretKey,
                this.containerState.getContainerId(),
                this.localstackPort
        ));
        this.restApiId = terraform.getOutputVar(TerraformContainer.OutputVar.REST_API_ID);

        this.copyScriptToContainer("localstack/scripts/%s".formatted(GET_LOGS_FROM_CW_SCRIPT_NAME));
    }

    public void printCloudwatchLogs() throws IOException, InterruptedException {
        this.execScriptInContainer(GET_LOGS_FROM_CW_SCRIPT_NAME);
    }

    public void printMainInstanceLogs() {
        LOGGER.log(System.Logger.Level.INFO, this.containerState.getLogs());
    }

    public void logLambdaAndPossiblyDestroyThem() {
        var thisNetworkId = this.containerState.getCurrentContainerInfo()
                        .getNetworkSettings().getNetworks().values().stream()
                        .findFirst().orElseThrow().getNetworkID();
        // lambda containers are not removed after the test because spawned by the localstack
        // container, and not directly by testcontainers. use docker api to
        // remove all lambda containers connected to the same network as localstack

        final String LAMBDA_IMAGE = "public.ecr.aws/lambda/java";
        var dockerClient = this.containerState.getDockerClient();
        dockerClient.listContainersCmd()
                .exec()
                .stream()
                .filter(c -> Objects.requireNonNull(c.getNetworkSettings())
                        .getNetworks()
                        .values()
                        .stream()
                        .anyMatch(network -> Objects.equals(network.getNetworkID(),
                                thisNetworkId))
                        && c.getImage().startsWith(LAMBDA_IMAGE))
                .forEach(c ->
                {
                    var containerName = dockerClient.inspectContainerCmd(c.getId())
                            .exec()
                            .getName();
                    var sb = new StringBuilder("Logs from %s%n".formatted(containerName));
                    sb.append("**************************").append(System.lineSeparator());
                    try {
                        dockerClient.logContainerCmd(c.getId())
                                .withStdOut(true)
                                .withStdErr(true)
                                .withTailAll()
                                .exec(new ResultCallback.Adapter<>() {
                                    @Override
                                    public void onNext(Frame frame) {
                                        sb.append(new String(frame.getPayload()));
                                    }
                                }).awaitCompletion();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    sb.append("**************************").append(System.lineSeparator());
                    LOGGER.log(System.Logger.Level.INFO, sb.toString());

                    if (!localstackConfig.keepLambdasOpenedAfterExit()) {
                        dockerClient.stopContainerCmd(c.getId()).exec();
                        dockerClient.removeContainerCmd(c.getId()).exec();
                    }
                });
    }

    public URI buildApiUrl(String pathPart) {
        Objects.requireNonNull(this.restApiId);

// http://localhost:4566/restapis/$REST_API_ID/$DEPLOYMENT_NAME/_user_request_/{pathPart}
        return URI.create("%s/restapis/%s/%s/_user_request_/%s".formatted(
                this.getEndpoint(),
                this.restApiId,
                DEPLOYMENT_STAGE_NAME,
                pathPart));
    }

    public URI buildBaseUrl(String urlPart) {
        return URI.create("%s/%s".formatted(this.getEndpoint(), urlPart));
    }

    // copied from the LocalstackContainer class, available through the Localstack module for testcontainers
    private URI getEndpoint() {
        try {
            final String address = this.containerState.getHost();
            // resolve IP address and use that as the endpoint so that path-style access is automatically used for S3
            String ipAddress = InetAddress.getByName(address).getHostAddress();
            return new URI("http://" + ipAddress + ":" + this.containerState.getMappedPort(this.localstackPort));
        } catch (UnknownHostException | URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }

    private void copyScriptToContainer(String scriptResourcePath) throws IOException, InterruptedException {
        var scriptResource = new PathMatchingResourcePatternResolver().getResource(scriptResourcePath);
        this.containerState.copyFileToContainer(Transferable.of(scriptResource.getContentAsByteArray()),
                "/" + scriptResource.getFilename());
        this.containerState.execInContainer("chmod", "+x", "/" + scriptResource.getFilename());
    }

    private void execScriptInContainer(String scriptName) throws IOException, InterruptedException {
        var executeScriptCmd = this.containerState.execInContainer("/" + scriptName);
        AssertionHelper.assertContainerCmdSuccessful(executeScriptCmd);
    }

    public record LocalstackConfig(boolean keepLambdasOpenedAfterExit,
                                   String logLevel) {}
}
