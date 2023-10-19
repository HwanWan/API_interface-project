package univsersity.jinan.api_client_sdk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import univsersity.jinan.api_client_sdk.client.ApiClient;
import univsersity.jinan.api_client_sdk.model.User;

@SpringBootTest
class ApiClientSdkApplicationTests {
    ///
    @Autowired
    private ApiClient apiClient;
    @Test
    void contextLoads() {

        System.out.println(apiClient.getNameByGet("http://localhost:8080/api/name/", "huanWW"));
        System.out.println(apiClient.getNameByPost("http://localhost:8080/api/name/", "huanWW"));
        User user = new User();
        user.setUserName("huanWW");
        System.out.println(apiClient.getNameByJSONPost("http://localhost:8080/api/name/userName/", user).getStatus());
    }

}
