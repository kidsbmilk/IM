package com.github.yuanrw.im.transfer.config;

import com.github.yuanrw.im.user.status.factory.UserStatusServiceFactory;
import com.github.yuanrw.im.user.status.service.UserStatusService;
import com.github.yuanrw.im.user.status.service.impl.RedisUserStatusServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Date: 2019-06-09
 * Time: 15:52
 *
 * @author yrw
 */
// Google Guice 一个轻量级的依赖注入框架：https://www.jianshu.com/p/7fba7b43146a
// 慕课网《使用Google Guice实现依赖注入》学习总结：http://www.imooc.com/article/20679
// google（轻量级依赖注入框架）Guice学习 (二) 绑定、Module的关系：https://blog.csdn.net/CoffeeAndIce/article/details/80303757
// Elasticsearch源码分析之一——使用Guice进行依赖注入与模块化系统：https://blog.csdn.net/u011939899/article/details/84795873
public class TransferModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
            .implement(UserStatusService.class, RedisUserStatusServiceImpl.class)
            .build(UserStatusServiceFactory.class));
    }
}