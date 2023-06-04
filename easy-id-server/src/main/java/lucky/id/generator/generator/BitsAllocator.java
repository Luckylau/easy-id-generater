package lucky.id.generator.generator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * The unique id has 64bits (long), default allocated as blow:<br>
 * <li>sign: The highest bit is 0
 * <li>delta seconds: The next 41 bits, represents delta seconds since a customer epoch(2023-05-27 00:00:00.000).
 * Supports about 69 years until to 2089-05-27
 * <li>worker id: The next 10 bits, represents the worker's id which assigns based on hostname
 * <li>sequence: The next 12 bits, represents a sequence within the same timestamp
 * <p>
 * +------+----------------------+----------------+-----------+
 * | sign |     delta seconds    | worker node id | sequence  |
 * +------+----------------------+----------------+-----------+
 * 1bit          41bits              10bits         12bits
 *
 * @Author luckylau
 * @Date 2023/5/27
 */
@Slf4j
@Data
public class BitsAllocator {

    public static final int TOTAL_BITS = 1 << 6;
    /**
     * Bits for [sign-> timestamp-> workId-> sequence]
     */
    private final int signBits = 1;
    private final int timestampBits;
    private final int workerIdBits;
    private final int sequenceBits;

    /**
     * Max value for workId & sequence
     */
    private final long maxDeltaTimestamp;
    private final long maxWorkerId;
    private final long maxSequence;

    private final int timestampShift;
    private final int workerIdShift;

    public static BitsAllocator defaultAllocator() {
        return new BitsAllocator(41, 10, 12);
    }

    public BitsAllocator(int timestampBits, int workerIdBits, int sequenceBits) {
        // make sure allocated 64 bits
        int allocateTotalBits = signBits + timestampBits + workerIdBits + sequenceBits;
        Assert.isTrue(allocateTotalBits == TOTAL_BITS, "allocate not enough 64 bits");

        // initialize bits
        this.timestampBits = timestampBits;
        this.workerIdBits = workerIdBits;
        this.sequenceBits = sequenceBits;

        // initialize max value
        this.maxDeltaTimestamp = ~(-1L << timestampBits);
        this.maxWorkerId = ~(-1L << workerIdBits);
        this.maxSequence = ~(-1L << sequenceBits);

        // initialize shift
        this.timestampShift = workerIdBits + sequenceBits;
        this.workerIdShift = sequenceBits;
    }


    public long allocate(long deltaTimestamp, long workerId, long sequence) {
        return (deltaTimestamp << timestampShift) | (workerId << workerIdShift) | sequence;

    }


}
