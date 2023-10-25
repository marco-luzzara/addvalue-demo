package it.addvalue.demo;

import it.addvalue.demo.api.LocalstackApiCaller;
import it.addvalue.demo.testcontainer.containers.AppContainer;
import it.addvalue.demo.testcontainer.containers.TerraformContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

@EnabledIfSystemProperty(named = "IntegrationTestsEnabled", matches = "true")
@Testcontainers
public class StepFunctionIT {
    @Container
    private static final AppContainer app = new AppContainer();

    @Container
    private static final TerraformContainer terraform = new TerraformContainer().withNetwork(app.NETWORK);
    private final LocalstackApiCaller localstackApiCaller = new LocalstackApiCaller(app);

    @BeforeAll
    static void initializeAll() throws IOException, InterruptedException {
        app.initialize(terraform);
    }

    @AfterEach
    void cleanupEach() throws IOException, InterruptedException {
        app.printCloudwatchLogs();
    }

    @AfterAll
    static void cleanupAll() throws IOException, InterruptedException {
        app.logAndPossiblyDestroyLambda();
    }

    @Test
    void runWorkflowWithSuccess() throws IOException, InterruptedException
    {
        var workflowResponse = localstackApiCaller.callStepFunctions("test-bucket");

        System.out.println("workflowResponse = " + workflowResponse);
    }
}
