package com.github.yuanrw.im.rest.web.task;

import com.github.yuanrw.im.common.domain.constant.ImConstant;
import com.github.yuanrw.im.common.parse.ParseService;
import com.github.yuanrw.im.protobuf.constant.MsgTypeEnum;
import com.github.yuanrw.im.protobuf.generate.Ack;
import com.github.yuanrw.im.protobuf.generate.Chat;
import com.github.yuanrw.im.rest.web.service.OfflineService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Date: 2019-05-15
 * Time: 22:58
 *
 * @author yrw
 */

/**
 * 有了@Component后，而且RestStarter中有@ComponentScan(basePackages = {"com.github.yuanrw.im.rest"})，
 * 则OfflineListen会被自动扫描创建相应对象，但是在自动创建对象时，发现OfflineListen只有一个带参数构造方法，
 * 所以会传入相应的对象然后生成OfflineListen对象。
 * 这样的流程也是非常自然的，不用再纠结@Autowired注解了。
 *
 * 同时，可以发现OfflineListen带参数构造方法的实现，可以更加灵活地结合spring的自动注入与手工new对象。
 * ParseService是common包下的类，不会自动扫描，所以这里手工new了。
 *
 * 对于OfflineService对象的自动生成是这样的：OfflineServiceImpl类上有@Service注解，所以会自动创建OfflineService对象。
 */
@Component
public class OfflineListen implements ChannelAwareMessageListener {
    private Logger logger = LoggerFactory.getLogger(OfflineListen.class);

    private ParseService parseService;
    private OfflineService offlineService;

    public OfflineListen(OfflineService offlineService) {
        this.parseService = new ParseService(); // ParseService是common包下的类，不会自动扫描，所以这里手工new了。
        this.offlineService = offlineService;
    }

    @PostConstruct
    public void init() {
        logger.info("[OfflineConsumer] Start listening Offline queue......");
    }

    @Override
    @RabbitHandler
    @RabbitListener(queues = ImConstant.MQ_OFFLINE_QUEUE, containerFactory = "listenerFactory")
    public void onMessage(Message message, Channel channel) throws Exception {
        logger.info("[OfflineConsumer] getUserSpi msg: {}", message.toString());
        try {
            int code = message.getBody()[0];

            byte[] msgBody = new byte[message.getBody().length - 1];
            System.arraycopy(message.getBody(), 1, msgBody, 0, message.getBody().length - 1);

            com.google.protobuf.Message msg = parseService.getMsgByCode(code, msgBody);
            if (code == MsgTypeEnum.CHAT.getCode()) {
                offlineService.saveChat((Chat.ChatMsg) msg);
            } else {
                offlineService.saveAck((Ack.AckMsg) msg);
            }

        } catch (Exception e) {
            logger.error("[OfflineConsumer] has error", e);
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
