package it.addvalue.demo.testcontainer.containers;

import com.google.gson.Gson;
import it.addvalue.demo.helpers.AssertionHelper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TerraformContainer {
    private Map<String, Object> outputVars = new HashMap<>();

    private ContainerState containerState;
    public TerraformContainer(ContainerState containerState) {
        this.containerState = containerState;
    }

    public void initialize() {
        try {
            this.copyLambdaZipToContainer();
            this.copyTerraformFilesToContainer();

            this.containerState.execInContainer("terraform", "init");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void apply(TfVariables tfVariables) {
        try {
            createTfOverrideFileForLocalstackProvider(tfVariables);

            var applyCmdExecution = this.containerState.execInContainer("sh", "-c", "terraform apply -auto-approve");
            AssertionHelper.assertContainerCmdSuccessful(applyCmdExecution);

            this.populateOutputVarFromTerraform();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public enum OutputVar {
        REST_API_ID("demo_apigw_rest_api_id")
        ;

        private final String varName;
        OutputVar(String varName) {
            this.varName = varName;
        }
    }

    public String getOutputVar(OutputVar outputVar) {
        return ((Map<String, Object>) this.outputVars.get(outputVar.varName)).get("value").toString();
    }

    private void copyLambdaZipToContainer() throws IOException {
        var zipPath = Path.of("").resolve("build").resolve("dist").resolve("general_lambda.zip");
        if (Files.notExists(zipPath))
            throw new IllegalStateException("Make sure the buildZip task is executed");
        this.containerState.copyFileToContainer(MountableFile.forHostPath(zipPath, 444), "/app/general_lambda.zip");
    }

    private void populateOutputVarFromTerraform() throws IOException, InterruptedException {
        var outputVarJson = this.containerState.execInContainer("terraform", "output", "-json").getStdout();
        var gson = new Gson();
        this.outputVars = gson.fromJson(outputVarJson, this.outputVars.getClass());
    }

    private void createTfOverrideFileForLocalstackProvider(TfVariables tfVariables) throws IOException {
        var providerOverrideTemplateTf = new PathMatchingResourcePatternResolver().getResource("terraform/provider_override.tf.template");
        var providerOverrideTf = providerOverrideTemplateTf.getContentAsString(StandardCharsets.UTF_8)
                .formatted(tfVariables.accessKey(),
                        tfVariables.secretKey(),
                        "http://%s:%s".formatted(tfVariables.localstackHostname(), tfVariables.localstackPort()));
        this.containerState.copyFileToContainer(Transferable.of(providerOverrideTf), "/app/provider_override.tf");
    }

    /**
     * copy the terraform files in the container, with base path "/app".
     * The directory structure will be:
     * /app
     * - main.tf
     * - module1/
     * - - main.tf
     * - - variables.tf
     * - localstack.auto.tfvars
     * ...
     */
    private void copyTerraformFilesToContainer() throws IOException {
        // all .tf files are copied, but only the .tfvars files for the Localstack env are included
        var terraformFiles = new PathMatchingResourcePatternResolver().getResources("classpath*:terraform/**/*.tf");
        var terraformVars = new PathMatchingResourcePatternResolver().getResources("classpath*:terraform/**/localstack*.auto.tfvars");

        var terraformResources = new ArrayList<Resource>();
        terraformResources.addAll(Arrays.stream(terraformFiles).toList());
        terraformResources.addAll(Arrays.stream(terraformVars).toList());

        terraformResources.stream().map(tr -> {
            try {
                var path = tr.getURL().getPath();
                var fromResourcesPathIndex = path.lastIndexOf("/terraform/");
                return path.substring(fromResourcesPathIndex + 1); // + 1 to exclude the leading /
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).forEach(tfResourcePath -> { // I receive paths like terraform/module/file.tf
            this.containerState.copyFileToContainer(MountableFile.forClasspathResource(tfResourcePath),
                    "/app" + tfResourcePath.substring("terraform".length()));
        });
    }

//    private ExecResult execInContainerWithLogs(String cmd) throws IOException, InterruptedException {
//        return this.execInContainer("sh", "-c", cmd + " &> /proc/1/fd/1");
//    }

    public record TfVariables(String accessKey,
                              String secretKey,
                              String localstackHostname,
                              int localstackPort) {}
}
