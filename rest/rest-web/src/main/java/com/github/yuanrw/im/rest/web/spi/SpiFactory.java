package com.github.yuanrw.im.rest.web.spi;

import com.github.yuanrw.im.common.exception.ImException;
import com.github.yuanrw.im.rest.spi.UserSpi;
import com.github.yuanrw.im.rest.spi.domain.UserBase;
import com.github.yuanrw.im.rest.web.spi.impl.DefaultUserSpiImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Date: 2019-07-03
 * Time: 17:50
 *
 * @author yrw
 */

/**
 * 有了@Component后，而且RestStarter中有@ComponentScan(basePackages = {"com.github.yuanrw.im.rest"})，
 * 则SpiFactory会被自动扫描创建相应对象，但是在自动创建对象时，发现SpiFactory只有一个带参数构造方法，
 * 所以会传入相应的对象然后生成SpiFactory对象。
 * 这样的流程也是非常自然的，不用再纠结@Autowired注解了。
 *
 * applicationContext.getBean时，也能得到UserSpi对象，由于DefaultUserSpiImpl和LdapUserSpiImpl上都有@Service注解，
 * spring不知道要自动注入哪个，所以这里用getBean来主动选择其中一个。
 */
@Component
public class SpiFactory implements ApplicationContextAware {

    private UserSpi<? extends UserBase> userSpi;
    private ApplicationContext applicationContext;

    @Value("${spi.user.impl.class}")
    private String userSpiImplClassName;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public UserSpi<? extends UserBase> getUserSpi() {
        if (StringUtils.isEmpty(userSpiImplClassName)) {
            return applicationContext.getBean(DefaultUserSpiImpl.class);
        }
        try {
            if (userSpi == null) {
                Class<?> userSpiImplClass = Class.forName(userSpiImplClassName);
                userSpi = (UserSpi<? extends UserBase>) applicationContext.getBean(userSpiImplClass);
            }
            return userSpi;
        } catch (ClassNotFoundException e) {
            throw new ImException("can not find class: " + userSpiImplClassName);
        }
    }
}