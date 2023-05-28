package lucky.id.generator.exception;

/**
 * @Author luckylau
 * @Date 2023/5/27
 */
public class IdGeneratorException extends RuntimeException {
    public IdGeneratorException() {
    }

    public IdGeneratorException(String message) {
        super(message);
    }

    public IdGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdGeneratorException(String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
    }
}
