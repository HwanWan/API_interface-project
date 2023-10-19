package University.Jinan.controller;


import University.Jinan.mapper.UserMapper;
import University.Jinan.model.entity.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import univsersity.jinan.api_client_sdk.utils.SignUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/name")
public class UserController {
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    @Autowired
    private UserMapper userMapper;


    @GetMapping("/")
    public String getuserNameByGet(@RequestParam String userName){
        System.out.println("调用了 GET 方法: "+userName);
        return "GET 你的名字是："+userName;
    }


    @PostMapping("/")
    public String getuserNameByPost(@RequestParam String userName){
        System.out.println("调用了 POST 方法: "+userName);
        return "POST 你的名字是："+userName;
    }

    @PostMapping("/userName")
    public String getNameByPost(@RequestBody univsersity.jinan.api_client_sdk.model.User user, HttpServletRequest request){
        System.out.println("调用了 POST JSON 方法");
        String accessKey = request.getHeader("accessKey");
        String timeStamp = request.getHeader("timeStamp");
        String nonce = request.getHeader("nonce");
        String body = request.getHeader("body");
        String sign = request.getHeader("sign");
        String userID = request.getHeader("id");
        if(userID==null) throw new RuntimeException("userID为空");
        Long id = Long.valueOf(userID);
        LoginUser loginUser = userMapper.selectById(id);
        SignUtils signUtils = new SignUtils();
        String utilsSign = signUtils.getSign(loginUser.getSecretKey()+"."+body);
        String UserAccessKey=loginUser.getAccessKey();

        if(!sign.equals(utilsSign) )
            throw new RuntimeException("权限不足");
        if(Long.valueOf(timeStamp)<System.currentTimeMillis()/1000)
            throw new RuntimeException("权限不足");
        if(Integer.valueOf(nonce)>10000)
            throw new RuntimeException("权限不足");
        if(!accessKey.equals(UserAccessKey)){
            throw new RuntimeException("权限不足");
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        HttpServletResponse response = ((ServletRequestAttributes) requestAttributes).getResponse();
        System.out.println("完成了！");
        return "POST 你的名字是："+user.getUserName();
    }
}
