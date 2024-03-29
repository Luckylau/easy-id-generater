package lucky.id.generator.server.service;

import lombok.extern.slf4j.Slf4j;
import lucky.id.generator.server.generator.IdGenerator;
import lucky.id.generator.server.generator.SnowflakeIdGenImpl;
import lucky.id.generator.server.util.NetUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author luckylau
 * @Date 2023/5/27
 */
@Service
@Slf4j
public class SnowflakeService implements InitializingBean {

    private IdGenerator idGenerator;

    @Value("${server.port}")
    private int port;

    @Value("${zookeeper.address}")
    private String zkAddress;

    public Long getId() {
        return idGenerator.getId();
    }

    public Long getIds(int range) {
        return idGenerator.getIds(range);
    }

    @Override
    public void afterPropertiesSet() {
        String ip = NetUtils.getIp();
        idGenerator = new SnowflakeIdGenImpl(zkAddress, ip, port);
    }
}
