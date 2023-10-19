package univsersity.jinan.api_client_sdk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import univsersity.jinan.api_client_sdk.client.ApiClient;

@SpringBootApplication
@EnableConfigurationProperties
public class ApiClientSdkApplication {

    @Autowired
    private static ApiClient apiClient;
    public static void main(String[] args) {
        System.out.println(apiClient);
        SpringApplication.run(ApiClientSdkApplication.class, args);
    }

}
