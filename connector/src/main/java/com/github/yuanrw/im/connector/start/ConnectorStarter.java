package com.github.yuanrw.im.connector.start;

import com.github.yuanrw.im.common.exception.ImException;
import com.github.yuanrw.im.connector.config.ConnectorConfig;
import com.github.yuanrw.im.connector.config.ConnectorModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Date: 2019-05-02
 * Time: 17:59
 *
 * @author yrw
 */
public class ConnectorStarter {
    public static ConnectorConfig CONNECTOR_CONFIG = new ConnectorConfig();
    /**
     * connector既要连接客户端，又要连接transfer，这里的业务更复杂，依赖注入也更复杂，
     * 只要把transfer里的看明白了，这里的也就没什么疑惑了，不用再纠结于对象是如何生成的，只要业务需要，拿来对象直接用就行。
     */
    public static Injector injector = Guice.createInjector(new ConnectorModule());

    public static void main(String[] args) throws IOException {
        //parse start parameter
        ConnectorStarter.CONNECTOR_CONFIG = parseConfig();

        //connector to transfer
        // 见connector/src/main/resources/connector.properties，transfer.urls是指transfer模块的机器地址
        ConnectorClient.start(CONNECTOR_CONFIG.getTransferUrls());

        //start connector server
        ConnectorServer.start(CONNECTOR_CONFIG.getPort());
    }

    private static ConnectorConfig parseConfig() throws IOException {
        Properties properties = getProperties();

        ConnectorConfig connectorConfig = new ConnectorConfig();
        try {
            connectorConfig.setPort(Integer.parseInt(properties.getProperty("port")));
            connectorConfig.setTransferUrls((properties.getProperty("transfer.url")).split(","));
            connectorConfig.setRestUrl(properties.getProperty("rest.url"));
            connectorConfig.setRedisHost(properties.getProperty("redis.host"));
            connectorConfig.setRedisPort(Integer.parseInt(properties.getProperty("redis.port")));
            connectorConfig.setRedisPassword(properties.getProperty("redis.password"));
        } catch (Exception e) {
            throw new ImException("there's a parse error, check your config properties", e);
        }

        System.setProperty("log.path", properties.getProperty("log.path"));
        System.setProperty("log.level", properties.getProperty("log.level"));

        return connectorConfig;
    }

    private static Properties getProperties() throws IOException {
        InputStream inputStream;
        String path = System.getProperty("config");
        if (path == null) {
            throw new ImException("connector.properties is not defined");
        } else {
            inputStream = new FileInputStream(path);
        }

        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }
}
