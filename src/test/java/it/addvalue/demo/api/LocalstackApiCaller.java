package it.addvalue.demo.api;

import com.google.gson.Gson;
import it.addvalue.demo.api.bodyhandlers.StepFunctionResponseBodyHandler;
import it.addvalue.demo.stepfunctions.model.StateOutput;
import it.addvalue.demo.stepfunctions.model.StepFunctionInput;
import it.addvalue.demo.testcontainer.containers.AppContainer;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class LocalstackApiCaller {
    private final AppContainer appContainer;

    private static final Gson gson = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public LocalstackApiCaller(AppContainer appContainer) {
        this.appContainer = appContainer;
    }

    public HttpResponse<List<StateOutput>> callStepFunctions(StepFunctionInput input) throws IOException, InterruptedException {
        var strBody = gson.toJson(input, StepFunctionInput.class);
        return HTTP_CLIENT.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(strBody))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(100))
                        .uri(this.appContainer.buildApiUrl("run_sf"))
                        .build(), new StepFunctionResponseBodyHandler());
    }
}
