package com.github.yuanrw.im.user.status.service;

/**
 * Date: 2019-06-09
 * Time: 15:55
 *
 * @author yrw
 */
public interface UserStatusService {

    /**
     * user online
     * 用户上线，存储userId与机器id的关系
     *
     * @param userId
     * @param connectorId
     * @return the user's previous connection id, if don't exist then return null
     */
    String online(String userId, String connectorId);

    /**
     * user offline
     *
     * @param userId
     */
    void offline(String userId);

    /**
     * get connector id by user id
     * 通过用户id查找他当前连接的机器id
     *
     * @param userId
     * @return
     */
    String getConnectorId(String userId);
}
