package com.github.yuanrw.im.rest.web.filter;

import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Date: 2019-02-12
 * Time: 10:37
 *
 * @author yrw
 */

/**
 * 有了@Component后，而且RestStarter中有@ComponentScan(basePackages = {"com.github.yuanrw.im.rest"})，
 * 则GlobalErrorWebExceptionHandler会被自动扫描创建相应对象，但是在自动创建对象时，
 * 发现GlobalErrorWebExceptionHandler只有一个带参数构造方法，
 * 所以会传入相应的对象然后生成GlobalErrorWebExceptionHandler对象。
 * 这样的流程也是非常自然的，不用再纠结@Autowired注解了。
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(
        ErrorAttributes errorAttributes, // 这个注入的是GlobalErrorAttributes对象，GlobalErrorAttributes类上有@Configuration注解。
        ResourceProperties resourceProperties,
        ApplicationContext applicationContext,
        ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(errorAttributes, resourceProperties, applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
        this.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {

        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, false);

        return ServerResponse.status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(errorPropertiesMap));
    }
}
