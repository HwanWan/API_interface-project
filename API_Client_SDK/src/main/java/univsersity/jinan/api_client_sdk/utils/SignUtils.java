package univsersity.jinan.api_client_sdk.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import org.springframework.stereotype.Component;

@Component
public class SignUtils {
    public String getSign(String Str){
        Digester md5 = new Digester(DigestAlgorithm.MD5);
        return md5.digestHex(Str);
    }
}
