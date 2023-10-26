package it.addvalue.demo.api.bodyhandlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.addvalue.demo.stepfunctions.model.StateOutput;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.io.InputStream;
import java.util.List;

public class StepFunctionResponseBodyHandler implements HttpResponse.BodyHandler<List<StateOutput>> {
    private static final Gson gson = new Gson();

    @Override
    public HttpResponse.BodySubscriber<List<StateOutput>> apply(HttpResponse.ResponseInfo responseInfo) {
        return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofInputStream(),
                this::parseJson);
    }

    private List<StateOutput> parseJson(InputStream inputStream) {
        Type statesOutputType = new TypeToken<List<StateOutput>>() {}.getType();
        return gson.fromJson(new InputStreamReader(inputStream), statesOutputType);
    }
}
