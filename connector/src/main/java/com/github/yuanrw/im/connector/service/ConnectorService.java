package com.github.yuanrw.im.connector.service;

import com.github.yuanrw.im.common.domain.conn.Conn;
import com.github.yuanrw.im.common.domain.constant.MsgVersion;
import com.github.yuanrw.im.common.util.IdWorker;
import com.github.yuanrw.im.connector.domain.ClientConnContext;
import com.github.yuanrw.im.connector.handler.ConnectorTransferHandler;
import com.github.yuanrw.im.protobuf.generate.Ack;
import com.github.yuanrw.im.protobuf.generate.Chat;
import com.google.inject.Inject;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * process msg the connector received
 * Date: 2019-04-08
 * Time: 21:05
 *
 * @author yrw
 */
public class ConnectorService {
    private static Logger logger = LoggerFactory.getLogger(ConnectorService.class);

    private ClientConnContext clientConnContext;

    @Inject
    public ConnectorService(ClientConnContext clientConnContext) {
        this.clientConnContext = clientConnContext;
    }

    // 接收到transfer发来的信息，connector转给client
    public void doChatToClientAndFlush(Chat.ChatMsg msg) {
        Conn conn = clientConnContext.getConnByUserId(msg.getDestId());
        if (conn == null) {
            //todo: if not on the machine
            logger.error("[send ack to client] not one the machine, userId: {}, connectorId: {}",
                msg.getDestId(), ConnectorTransferHandler.CONNECTOR_ID);
            return;
        }
        conn.getCtx().writeAndFlush(msg);
        doSendAckToClientOrTransferAndFlush(getDelivered(msg));
    }

    // 接收来自client的消息，然后发送ack给client
    public void doSendAckToClientAndFlush(Ack.AckMsg ackMsg) {
        Conn conn = clientConnContext.getConnByUserId(ackMsg.getDestId());
        if (conn == null) {
            //todo: if not on the machine
            logger.error("[send msg to client] not one the machine, userId: {}, connectorId: {}",
                ackMsg.getDestId(), ConnectorTransferHandler.CONNECTOR_ID);
            return;
        }
        conn.getCtx().writeAndFlush(ackMsg);
    }

    // 这个是connector返回给client信息或者connector将信息传给transfer
    public void doChatToClientOrTransferAndFlush(Chat.ChatMsg msg) {
        Conn conn = clientConnContext.getConnByUserId(msg.getDestId());
        boolean onTheMachine = sendMsg(conn, msg.getId(), msg, (c, m) -> conn.getCtx().writeAndFlush(msg));
        if (onTheMachine) {
            doSendAckToClientOrTransferAndFlush(getDelivered(msg));
        }
    }

    // 这个是connector给client或者transfer发送ack信息
    public void doSendAckToClientOrTransferAndFlush(Ack.AckMsg ackMsg) {
        logger.debug("[send ack] {}", ackMsg.toString());

        Conn conn = clientConnContext.getConnByUserId(ackMsg.getDestId());
        sendMsg(conn, ackMsg.getId(), ackMsg, (c, m) -> conn.getCtx().writeAndFlush(ackMsg));
    }

    // 这个getDelivered是指生成指定消息已传送的ack消息，感觉取名很容易让人不理解
    public Ack.AckMsg getDelivered(Chat.ChatMsg msg) {
        return Ack.AckMsg.newBuilder()
            .setId(IdWorker.genId())
            .setVersion(MsgVersion.V1.getVersion())
            .setFromId(msg.getDestId())
            .setDestId(msg.getFromId()) // 是对源头的ack答复，所以destId是源id
            .setDestType(msg.getDestType() == Chat.ChatMsg.DestType.SINGLE ? Ack.AckMsg.DestType.SINGLE : Ack.AckMsg.DestType.GROUP)
            .setCreateTime(System.currentTimeMillis())
            .setMsgType(Ack.AckMsg.MsgType.DELIVERED)
            .setAckMsgId(msg.getId())
            .build();
    }

    private boolean sendMsg(Conn conn, Long msgId, Message msg, BiConsumer<Conn, Message> ifOnTheMachine) {
        if (conn == null) {
            ConnectorTransferHandler.getOneOfTransferCtx(msgId).writeAndFlush(msg);
            return false;
        } else {
            //the user is connected to this machine
            //won 't save chat histories
            ifOnTheMachine.accept(conn, msg);
            return true;
        }
    }
}