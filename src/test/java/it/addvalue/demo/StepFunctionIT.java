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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

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
        var workflowResponse = localstackApiCaller.callStepFunctions(new StepFunctionInput("test", 3));

        Assertions.assertThat(workflowResponse.body()).containsAll(
                List.of(new StateOutput(0),
                        new StateOutput(2),
                        new StateOutput(4)
                )
        );
    }
}
