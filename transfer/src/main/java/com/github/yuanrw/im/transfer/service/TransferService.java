package com.github.yuanrw.im.transfer.service;

import com.github.yuanrw.im.common.domain.conn.Conn;
import com.github.yuanrw.im.common.domain.conn.ConnectorConn;
import com.github.yuanrw.im.common.domain.constant.ImConstant;
import com.github.yuanrw.im.common.domain.constant.MsgVersion;
import com.github.yuanrw.im.common.util.IdWorker;
import com.github.yuanrw.im.protobuf.generate.Ack;
import com.github.yuanrw.im.protobuf.generate.Chat;
import com.github.yuanrw.im.protobuf.generate.Internal;
import com.github.yuanrw.im.transfer.domain.ConnectorConnContext;
import com.github.yuanrw.im.transfer.start.TransferMqProducer;
import com.github.yuanrw.im.transfer.start.TransferStarter;
import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.rabbitmq.client.MessageProperties;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

/**
 * Date: 2019-05-04
 * Time: 13:47
 *
 * @author yrw
 */
public class TransferService {

    private ConnectorConnContext connContext;
    private TransferMqProducer producer;

    @Inject
    public TransferService(ConnectorConnContext connContext) {
        this.connContext = connContext;
        this.producer = TransferStarter.producer;
    }

    // 检测是否某个connector上存储着这个登录用户的连接，
    // 如果有的话，就转发给对应connector，
    // （注意：这个消息是connector转给transfer，然后transfer再转给其他connector的，这个消息在transfer里并没有做修改），
    // 在connector的ConnectorService.doChatToClientAndFlush中，再在自己的clientConnContext中判断是否有此用户，
    // 如果有的话，就发送给目的用户，如果没有就报错，其实作者在报错那里有个to do，需要优化一下。

    // 注意一点：connector与transfer之间，也是通过正常的conn来连接的，这个conn与用户跟connector的conn是同类型的连接。
    // connContext.getConnByUserId中是先判断用户id在哪个connector上，然后取出连接此connector的conn，
    // transfer通过这个conn，将消息发送给connector。
    public void doChat(Chat.ChatMsg msg) throws IOException {
        ConnectorConn conn = connContext.getConnByUserId(msg.getDestId());

        if (conn != null) {
            conn.getCtx().writeAndFlush(msg);
        } else {
            doOffline(msg); // 如果找不到的话，就写入到mq，保存起来。
        }
    }

    public void doSendAck(Ack.AckMsg msg) throws IOException {
        ConnectorConn conn = connContext.getConnByUserId(msg.getDestId());

        if (conn != null) {
            conn.getCtx().writeAndFlush(msg);
        } else {
            doOffline(msg); // 如果找不到的话，就写入到mq，保存起来。
        }
    }

    // 当connector上线后，向transfer发送greet消息后，
    // transfer就调用doGreet将此connector的conn保存到connContext里。
    public void doGreet(Internal.InternalMsg msg, ChannelHandlerContext ctx) {
        ctx.channel().attr(Conn.NET_ID).set(msg.getMsgBody());
        ConnectorConn conn = new ConnectorConn(ctx);
        connContext.addConn(conn);

        ctx.writeAndFlush(getInternalAck(msg.getId()));
    }

    // 这个是得到源是transfer、目的是connector的ack消息
    private Internal.InternalMsg getInternalAck(Long msgId) {
        return Internal.InternalMsg.newBuilder()
            .setVersion(MsgVersion.V1.getVersion())
            .setId(IdWorker.genId())
            .setFrom(Internal.InternalMsg.Module.TRANSFER)
            .setDest(Internal.InternalMsg.Module.CONNECTOR)
            .setCreateTime(System.currentTimeMillis())
            .setMsgType(Internal.InternalMsg.MsgType.ACK)
            .setMsgBody(msgId + "")
            .build();
    }

    private void doOffline(Message msg) throws IOException {
        producer.basicPublish(ImConstant.MQ_EXCHANGE, ImConstant.MQ_ROUTING_KEY,
            MessageProperties.PERSISTENT_TEXT_PLAIN, msg);
    }
}