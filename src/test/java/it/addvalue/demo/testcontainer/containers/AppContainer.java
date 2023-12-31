package it.addvalue.demo.testcontainer.containers;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import it.addvalue.demo.helpers.AssertionHelper;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class AppContainer extends LocalStackContainer {
    public final String NETWORK_ALIAS = "localstack";
    private static final System.Logger LOGGER = System.getLogger(AppContainer.class.getName());
    public final Network NETWORK = Network.SHARED;
    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:latest");
    private static final String DEPLOYMENT_STAGE_NAME = "demo";
    private String restApiId;
    private final LocalstackConfig localstackConfig;

    private static final String GET_LOGS_FROM_CW_SCRIPT_NAME = "aws-get-last-logs.sh";

    public AppContainer()
    {
        this(new LocalstackConfig(false, "info"));
    }

    public AppContainer(LocalstackConfig localstackConfig)
    {
        super(localstackImage);

        this.localstackConfig = localstackConfig;

        // https://joerg-pfruender.github.io/software/testing/2020/09/27/localstack_and_lambda.html#1-networking
        withNetwork(NETWORK);
        withNetworkAliases(NETWORK_ALIAS);
        // 4566 - standard port
        withExposedPorts(4566);
        withServices(
                Service.LAMBDA,
                Service.API_GATEWAY,
                Service.S3,
                Service.CLOUDWATCHLOGS);
        withEnv(Map.of(
                "LAMBDA_DOCKER_NETWORK", ((Network.NetworkImpl) NETWORK).getName(),
                "PROVIDER_OVERRIDE_STEPFUNCTIONS", "v2",
                "LS_LOG", this.localstackConfig.logLevel
                ));
    }

    /**
     * run the terraform apply to create all the necessary resources
     */
    public void initialize(TerraformContainer terraform) throws IOException, InterruptedException {
        terraform.initialize();
        terraform.apply(new TerraformContainer.TfVariables(
                this.getAccessKey(),
                this.getSecretKey(),
                NETWORK_ALIAS,
                4566
        ));
        this.restApiId = terraform.getOutputVar(TerraformContainer.OutputVar.REST_API_ID);

        this.copyScriptToContainer("localstack/scripts/%s".formatted(GET_LOGS_FROM_CW_SCRIPT_NAME));

        this.followOutput(outFrame ->
                LOGGER.log(System.Logger.Level.INFO,
                        "%s - %s".formatted(outFrame.getType(), outFrame.getUtf8String())));
    }

    public void printCloudwatchLogs() throws IOException, InterruptedException {
        this.execScriptInContainer(GET_LOGS_FROM_CW_SCRIPT_NAME);
    }

    public void logAndPossiblyDestroyLambda() {
        var thisNetworkId = Objects.requireNonNull(this.getNetwork()).getId();
        // lambda containers are not removed after the test because spawned by the localstack
        // container, and not directly by testcontainers. use docker api to
        // remove all lambda containers connected to the same network as localstack

        final String LAMBDA_IMAGE = "public.ecr.aws/lambda/java";
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

    private void copyScriptToContainer(String scriptResourcePath) throws IOException, InterruptedException {
        var scriptResource = new PathMatchingResourcePatternResolver().getResource(scriptResourcePath);
        this.copyFileToContainer(Transferable.of(scriptResource.getContentAsByteArray()), "/" + scriptResource.getFilename());
        this.execInContainer("chmod", "+x", "/" + scriptResource.getFilename());
    }

    private void execScriptInContainer(String scriptName) throws IOException, InterruptedException {
        var executeScriptCmd = this.execInContainer("/" + scriptName);
        AssertionHelper.assertContainerCmdSuccessful(executeScriptCmd);
    }

    public record LocalstackConfig(boolean keepLambdasOpenedAfterExit,
                                   String logLevel) {}
}
