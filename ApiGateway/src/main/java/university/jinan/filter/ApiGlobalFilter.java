package university.jinan.filter;



import com.yupi.yuapicommon.model.entity.InterfaceInfo;
import com.yupi.yuapicommon.model.entity.User;
import com.yupi.yuapicommon.service.InnerInterfaceInfoService;
import com.yupi.yuapicommon.service.InnerUserInterfaceInfoService;
import com.yupi.yuapicommon.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import univsersity.jinan.api_client_sdk.utils.SignUtils;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 全局过滤器
 * */
@Slf4j
@Component //要让springboot扫描到才生效
public class ApiGlobalFilter implements GlobalFilter, Ordered {



    @DubboReference
    private InnerInterfaceInfoService interfaceInfoService;
    @DubboReference
    private InnerUserInterfaceInfoService userInterfaceInfoService;
    @DubboReference
    private InnerUserService userService;

    private List<String> IP_WHITE_LIST= Arrays.asList("f6c6ada8-1");
    private final String PORT="http://localhost:8091";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.用户发送请求到API网关 ==》 application配置中实现
        // 2.请求日志
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        LogRequestInfo(request);
        // 3.白名单 ==> 在名单内的用户才能往下进行
//        if(!IP_WHITE_LIST.contains(request.getId())){
//            // 设置状态码并设置请求结束【不在白名单内的用户不允许继续执行请求】
//            return refuseRequest(response, HttpStatus.FORBIDDEN);
//        }
        // 4.权鉴定(判断accessKey、secretKey否合法)
        if(CheckAuth(exchange)==response.setComplete())
            return refuseRequest(response, HttpStatus.FORBIDDEN);

        InterfaceInfo interfaceInfo;
        // 5.请求的模拟接口是否存在，以及查看剩余可调用次数，如果大于0
        try {
            interfaceInfo = interfaceInfoService.getInterfaceInfo(PORT + request.getPath(), String.valueOf(request.getMethod()));
            if(interfaceInfo==null)
                return refuseRequest(response, HttpStatus.FORBIDDEN);
            //System.out.println("接口信息==》》》"+interfaceInfo);
        }catch (Exception e){
            throw new RuntimeException("测试请求的模拟接口是否存在失败");
        }



        // todo 是否还有调用次数
        // 5. 请求转发，调用模拟接口 + 响应日志
       //Mono<Void> filter = chain.filter(exchange);
        //return refuseRequest(response,HttpStatus.FORBIDDEN);
        return handleResponse(exchange, chain,interfaceInfo.getId(),interfaceInfo.getUserId());
    }

    /**
     * 处理响应
     *
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain,long interfaceInfoId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 缓存数据的工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 拿到响应码
            HttpStatus statusCode = originalResponse.getStatusCode();


            if (statusCode == HttpStatus.OK) {
                // 装饰，增强能力
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    // 等调用完转发的接口后才会执行

                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        if (body instanceof Flux) {


                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 往返回值里写数据
                            // 拼接字符串
                            return super.writeWith(
                                    fluxBody.map(dataBuffer -> {
                                        // 7. 调用成功，接口调用次数 + 1 invokeCount
                                        try {
                                            boolean invokeCount = userInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                            if(!invokeCount)
                                                throw new RuntimeException("接口调用次数 + 1 失败");
                                        }catch (Exception e){
                                            throw new RuntimeException("接口调用次数 + 1 失败");
                                        }

                                        byte[] content = new byte[dataBuffer.readableByteCount()];
                                        dataBuffer.read(content);
                                        DataBufferUtils.release(dataBuffer);//释放掉内存
                                        // 构建日志
                                        StringBuilder sb2 = new StringBuilder(200);
                                        List<Object> rspArgs = new ArrayList<>();
                                        rspArgs.add(originalResponse.getStatusCode());
                                        String data = new String(content, StandardCharsets.UTF_8); //data
                                        sb2.append(data);
                                        // 打印日志
                                        log.info("响应结果：" + data);
                                        return bufferFactory.wrap(content);
                                    }));
                        } else {
                            // 8. 调用失败，返回一个规范的错误码
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 设置 response 对象为装饰过的
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            return chain.filter(exchange); // 降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常" + e);
            return chain.filter(exchange);
        }
    }


    /**
     * 拒绝继续往下处理请求
     */
    public Mono<Void> refuseRequest(ServerHttpResponse response,HttpStatus status){
        response.setStatusCode(status);
        return response.setComplete();
    }

    /**
     * 权限校验
     * */
    public Mono<Void> CheckAuth(ServerWebExchange exchange){
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String timeStamp =headers.getFirst ("timeStamp");
        String nonce = headers.getFirst("nonce");
        String body = ((HttpHeaders) headers).getFirst("body");
        String sign = headers.getFirst("sign");
        String userID = headers.getFirst("id");
        if(userID==null) throw new RuntimeException("userID为空");
        Long id = Long.valueOf(userID);
        // 从数据库获取用户信息
        User invokeUser;
        try {
            invokeUser = userService.getInvokeUser(accessKey);
            if(invokeUser==null)
                return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        }catch (Exception e){
            throw new RuntimeException("从数据库获取用户信息失败");
        }

        //System.out.println("用户信息==》》》"+invokeUser);
        SignUtils signUtils = new SignUtils();
        String utilsSign = signUtils.getSign(invokeUser.getSecretKey()+"."+body);
        String UserAccessKey=invokeUser.getAccessKey();

        if(!sign.equals(utilsSign) )
            return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        if(Long.valueOf(timeStamp)<System.currentTimeMillis()/1000)
            return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        if(Integer.valueOf(nonce)>10000)
            return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        if(!accessKey.equals(UserAccessKey))
            return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        return null;

    }

    /**
     * 打印请求信息
     * */
    public void LogRequestInfo(ServerHttpRequest request){
        log.info("请求ID: "+request.getId());
        log.info("请求方式: "+request.getMethod());
        log.info("请求路径: "+request.getPath().value());
        log.info("目标: "+request.getRemoteAddress());
        log.info("请求参数: "+request.getQueryParams());
        log.info("请求主机名: "+request.getLocalAddress());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
