package lucky.id.generator.server.generator;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import lucky.id.generator.server.exception.IdGeneratorException;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Author luckylau
 * @Date 2023/5/27
 */
@Slf4j
public class SnowflakeIdGenImpl implements IdGenerator {

    private static final Random RANDOM = new Random();
    private final long EPOCH_TIMESTAMP = TimeUnit.MILLISECONDS.toMillis(1685145600000L);
    private long lastTimestamp = -1L;
    private final BitsAllocator bitsAllocator;
    private long sequence = 0L;
    private final long workerId;


    public SnowflakeIdGenImpl(String zkAddress, String ip, int port) {
        bitsAllocator = BitsAllocator.defaultAllocator();
        SnowflakeZookeeperHolder holder = new SnowflakeZookeeperHolder(zkAddress, ip, port);
        holder.init();
        workerId = holder.getWorkerId();
        Preconditions.checkArgument(workerId >= 0 && workerId <= bitsAllocator.getMaxWorkerId(), "workerID must gte 0 and lte " + bitsAllocator.getMaxWorkerId());
    }

    @Override
    public synchronized Long getId() {
        return getIds(1);
    }

    @Override
    public synchronized Long getIds(int range) {
        if (range <= 0 || range >= bitsAllocator.getMaxSequence()) {
            throw new IdGeneratorException("Range %d is illegal", range);
        }
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            //如果在5毫秒以内则等待双倍时间
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new IdGeneratorException("Clock moved backwards");
                    }
                } catch (InterruptedException e) {
                    log.error("wait interrupted");
                    throw new IdGeneratorException("wait interrupted");
                }
            } else {
                throw new IdGeneratorException("Clock moved backwards");
            }
        }
        if (lastTimestamp == timestamp) {
            //如果达到最大，等下一个毫秒
            if (sequence > bitsAllocator.getMaxSequence()) {
                timestamp = tilNextMillis(lastTimestamp);
                sequence = 0L;
            }
        } else {
            //如果是新的毫秒，sequence随机
            sequence = RANDOM.nextInt(100);
        }
        sequence += range;
        lastTimestamp = timestamp;
        return bitsAllocator.allocate(timestamp - EPOCH_TIMESTAMP, workerId, sequence);
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
}
