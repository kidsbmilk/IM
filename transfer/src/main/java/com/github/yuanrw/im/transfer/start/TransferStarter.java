package com.github.yuanrw.im.transfer.start;

import com.github.yuanrw.im.common.exception.ImException;
import com.github.yuanrw.im.transfer.config.TransferConfig;
import com.github.yuanrw.im.transfer.config.TransferModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Date: 2019-05-07
 * Time: 20:39
 *
 * @author yrw
 */
public class TransferStarter {
    public static TransferConfig TRANSFER_CONFIG = new TransferConfig();
    public static TransferMqProducer producer;
    /**
     * 这里的依赖实现太复杂了，guice还是侵入代码了，耦合度高。
     *
     * TransferModule使得需要userStatusService对象的地方可以通过自动注入UserStatusServiceFactory对象然后再通过UserStatusServiceFactory.createService的方式来生成，
     * 在ConnectorConnContext的构造函数中，就依赖了userStatusService对象，就是通过上述方式来实现的。
     * 代码中没有直接生成ConnectorConnContext对象的地方，是TransferConnectorHandler构造函数中需要TransferConnectorHandler对象，也是通过依赖注入自动生成的，
     * 具体开始注入的时机是：TransferServer.startTransferServer中的TransferStarter.injector.getInstance(TransferConnectorHandler.class)。
     * 两次注入地点：ConnectorConnContext的构造函数、TransferConnectorHandler构造函数上都有@Inject注解。
     */
    static Injector injector = Guice.createInjector(new TransferModule());

    public static void main(String[] args) {
        try {
            //parse start parameter
            TransferStarter.TRANSFER_CONFIG = parseConfig();

            //start rabbitmq server
            producer = new TransferMqProducer(TRANSFER_CONFIG.getRabbitmqHost(), TRANSFER_CONFIG.getRabbitmqPort(),
                TRANSFER_CONFIG.getRabbitmqUsername(), TRANSFER_CONFIG.getRabbitmqPassword());

            //start transfer server
            TransferServer.startTransferServer(TRANSFER_CONFIG.getPort());
        } catch (Exception e) {
            LoggerFactory.getLogger(TransferStarter.class).error("[transfer] start failed", e);
        }
    }

    private static TransferConfig parseConfig() throws IOException {
        Properties properties = getProperties();

        TransferConfig transferConfig = new TransferConfig();
        try {
            transferConfig.setPort(Integer.parseInt((String) properties.get("port")));
            transferConfig.setRedisHost(properties.getProperty("redis.host"));
            transferConfig.setRedisPort(Integer.parseInt(properties.getProperty("redis.port")));
            transferConfig.setRedisPassword(properties.getProperty("redis.password"));
            transferConfig.setRabbitmqHost(properties.getProperty("rabbitmq.host"));
            transferConfig.setRabbitmqUsername(properties.getProperty("rabbitmq.username"));
            transferConfig.setRabbitmqPassword(properties.getProperty("rabbitmq.password"));
            transferConfig.setRabbitmqPort(Integer.parseInt(properties.getProperty("rabbitmq.port")));
        } catch (Exception e) {
            throw new ImException("there's a parse error, check your config properties");
        }

        System.setProperty("log.path", properties.getProperty("log.path"));
        System.setProperty("log.level", properties.getProperty("log.level"));

        return transferConfig;
    }

    private static Properties getProperties() throws IOException {
        InputStream inputStream;
        String path = System.getProperty("config");
        if (path == null) {
            throw new ImException("transfer.properties is not defined");
        } else {
            inputStream = new FileInputStream(path);
        }

        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }
}
