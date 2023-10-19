package univsersity.jinan.api_client_sdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import univsersity.jinan.api_client_sdk.model.User;
import univsersity.jinan.api_client_sdk.utils.SignUtils;

import java.util.HashMap;

@Data
public class ApiClient {


    private String accessKey;
    private String secretKey;

    public ApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getNameByGet(String url, String userName){
        //GET请求
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("userName",userName);
        String content = HttpUtil.get(url,paramMap);
        return content;
    }


    public String getNameByPost(String url,String userName){
        //POST请求
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("userName",userName);
        String result = HttpUtil.post(url, paramMap);
        return result;
    }

    public HashMap<String,String> getHeaderMap(String data){
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("accessKey",accessKey);
        Gson gson = new Gson();
        User user = gson.fromJson(data, User.class);
        hashMap.put("Id",String.valueOf(user.getId()));
        //签名=secretKey+body数据的加密
        SignUtils signUtils = new SignUtils();
        hashMap.put("sign",signUtils.getSign(secretKey+"."+data));
        Long delayTime = 100000L;
        hashMap.put("timeStamp", String.valueOf(System.currentTimeMillis()/1000+delayTime));
        hashMap.put("nonce", String.valueOf(RandomUtil.randomInt(4)));
        hashMap.put("body",data);
        return hashMap;
    }

    public HttpResponse getNameByJSONPost(String url, User user){
        //POST请求
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post(url)
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        System.out.println("结果："+httpResponse);
        return httpResponse;
    }

}
