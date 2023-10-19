package univsersity.jinan.api_client_sdk;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import univsersity.jinan.api_client_sdk.client.ApiClient;

@Configuration
@Data
@ConfigurationProperties(prefix = "api.client")
@ComponentScan //确保启动类能扫描到该类
public class ApiClientConfig {

    @Value("accessKey")
    private String accessKey;
    @Value("secretKey")
    private String secretKey;

    @Bean
    public ApiClient getApiClient(){
        return new ApiClient(accessKey,secretKey);
    }
}
