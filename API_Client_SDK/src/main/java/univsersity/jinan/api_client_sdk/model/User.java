package univsersity.jinan.api_client_sdk.model;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Data
@Component
public class User {
    private String userName;
    private Long id;
}
