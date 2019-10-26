package com.github.yuanrw.im.rest.web.filter;

import com.github.yuanrw.im.common.exception.ImException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Date: 2019-04-21
 * Time: 15:51
 *
 * @author yrw
 */

/**
 * 有了@Component后，而且RestStarter中有@ComponentScan(basePackages = {"com.github.yuanrw.im.rest"})，
 * 则HeaderFilter会被自动扫描创建相应对象，但是在自动创建对象时，发现HeaderFilter只有一个带参数构造方法，
 * 所以会传入相应的对象然后生成HeaderFilter对象。
 * 这样的流程也是非常自然的，不用再纠结@Autowired注解了。
 */
@Component
public class HeaderFilter implements WebFilter {

    private TokenManager tokenManager;

    public HeaderFilter(TokenManager tokenManager) { // 这个注入的是同包下的TokenManager对象，TokenManager类上有@Service注解。
        this.tokenManager = tokenManager;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        String path = serverWebExchange.getRequest().getPath().value();

        if ("/user/login".equals(path) || path.startsWith("/offline")) {
            return webFilterChain.filter(serverWebExchange); // 原来spring内部就使用了Mono
        }
        if (!serverWebExchange.getRequest().getHeaders().containsKey("token")) {
            return Mono.error(new ImException("[rest] user is not login"));
        }

        String token = serverWebExchange.getRequest().getHeaders().getFirst("token");

        return tokenManager.validateToken(token).flatMap(b -> b != null ? webFilterChain.filter(serverWebExchange) :
            Mono.error(new ImException("[rest] user is not login")));
    }
}
