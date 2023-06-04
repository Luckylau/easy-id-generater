package lucky.id.generator.service;

import lombok.extern.slf4j.Slf4j;
import lucky.id.generator.generator.IdGenerator;
import lucky.id.generator.generator.SnowflakeIdGenImpl;
import lucky.id.generator.util.NetUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author luckylau
 * @Date 2023/5/27
 */
@Service
@Slf4j
public class SnowflakeService {

    private final IdGenerator idGenerator;

    @Value("${server.port}")
    private int port;

    @Value("${zookeeper.address}")
    private String zkAddress;


    public SnowflakeService() {
        String ip = NetUtils.getIp();
        idGenerator = new SnowflakeIdGenImpl(zkAddress, ip, port);
    }

    public Long getId() {
        return idGenerator.getId();
    }

    public Long getIds(int range) {
        return idGenerator.getIds(range);
    }


}
