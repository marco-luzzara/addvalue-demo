package it.addvalue.demo;

import it.addvalue.demo.api.LocalstackApiCaller;
import it.addvalue.demo.stepfunctions.model.StateOutput;
import it.addvalue.demo.stepfunctions.model.StepFunctionInput;
import it.addvalue.demo.testcontainer.containers.AppContainer;
import it.addvalue.demo.testcontainer.containers.TerraformContainer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.util.List;

@EnabledIfSystemProperty(named = "IntegrationTestsEnabled", matches = "true")
@Testcontainers
public class StepFunctionIT {
    private static final String LOCALSTACK_SERVICE_NAME = "localstack";
    private static final String TERRAFORM_SERVICE_NAME = "terraform";
    private static final String DOCKER_COMPOSE_OVERRIDE_DIR = "src/test/resources/localstack/docker-compose";

    @Container
    public static ComposeContainer compose = new ComposeContainer(
            new File("docker-compose.yml"),
            new File(DOCKER_COMPOSE_OVERRIDE_DIR + "/docker-compose.override1.yml"),
            new File(DOCKER_COMPOSE_OVERRIDE_DIR + "/docker-compose.override2.yml")
            )
            .withExposedService(LOCALSTACK_SERVICE_NAME, 4566)
            .withServices(TERRAFORM_SERVICE_NAME)
            .withLocalCompose(true);

    private static TerraformContainer terraform;
    private static AppContainer app;

    private static LocalstackApiCaller localstackApiCaller;

    @BeforeAll
    static void initializeAll() throws IOException, InterruptedException {
        terraform = new TerraformContainer(
                compose.getContainerByServiceName(TERRAFORM_SERVICE_NAME).orElseThrow()
        );
        app = new AppContainer(
                compose.getContainerByServiceName(LOCALSTACK_SERVICE_NAME).orElseThrow()
        );
        localstackApiCaller = new LocalstackApiCaller(app);
        app.initialize(terraform);
    }

    @AfterEach
    void cleanupEach() throws IOException, InterruptedException {
        app.printCloudwatchLogs();
    }

    @AfterAll
    static void cleanupAll() throws IOException, InterruptedException {
        app.printMainInstanceLogs();
        app.logLambdaAndPossiblyDestroyThem();
    }

    @Test
    void runWorkflowWithSuccess() throws IOException, InterruptedException
    {
        var workflowResponse = localstackApiCaller.callStepFunctions(new StepFunctionInput("test", 3));

        Assertions.assertThat(workflowResponse.body()).containsAll(
                List.of(new StateOutput(0),
                        new StateOutput(2),
                        new StateOutput(4)
                )
        );
    }
}
