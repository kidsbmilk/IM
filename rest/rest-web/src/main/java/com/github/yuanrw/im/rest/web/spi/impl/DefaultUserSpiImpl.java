package com.github.yuanrw.im.rest.web.spi.impl;

import com.github.yuanrw.im.common.domain.po.User;
import com.github.yuanrw.im.rest.spi.UserSpi;
import com.github.yuanrw.im.rest.spi.domain.UserBase;
import com.github.yuanrw.im.rest.web.service.UserService;
import org.springframework.stereotype.Service;

/**
 * Date: 2019-07-03
 * Time: 17:49
 *
 * @author yrw
 */

/**
 * 有了@Service后，而且RestStarter中有@ComponentScan(basePackages = {"com.github.yuanrw.im.rest"})，
 * 则DefaultUserSpiImpl会被自动扫描创建相应对象，但是在自动创建对象时，发现DefaultUserSpiImpl只有一个带参数构造方法，
 * 所以会传入相应的对象然后生成DefaultUserSpiImpl对象。
 * 这样的流程也是非常自然的，不用再纠结@Autowired注解了。
 *
 * 再看UserService的实现类UserServiceImpl上有@Service注解，
 * 所以这里的构造函数能得到对应的对象而顺利构造DefaultUserSpiImpl对象。
 *
 * 这个DefaultUserSpiImpl其实就是走数据库的实现，见UserServiceImpl里的具体操作。
 */
@Service
public class DefaultUserSpiImpl implements UserSpi<UserBase> {

    private UserService userService;

    public DefaultUserSpiImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserBase getUser(String username, String pwd) {
        User user = userService.verifyAndGet(username, pwd);
        if (user == null) {
            return null;
        }

        UserBase userBase = new UserBase();
        userBase.setId(user.getId() + "");
        userBase.setUsername(user.getUsername());
        return userBase;
    }

    @Override
    public UserBase getById(String id) {
        User user = userService.getById(Long.parseLong(id));
        if (user == null) {
            return null;
        }

        UserBase userBase = new UserBase();
        userBase.setId(userBase.getId());
        userBase.setUsername(userBase.getUsername());
        return userBase;
    }
}
