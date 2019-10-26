package com.github.yuanrw.im.rest.spi.domain;

/**
 * Date: 2019-07-07
 * Time: 13:15
 *
 * @author yrw
 */

// Java - 什么是领域模型(domain model)？贫血模型(anaemic domain model)和充血模型(rich domain model)有什么区别？
// https://blog.csdn.net/troubleshooter/article/details/78479984
public class UserBase {

    private String id;
    private String username;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
