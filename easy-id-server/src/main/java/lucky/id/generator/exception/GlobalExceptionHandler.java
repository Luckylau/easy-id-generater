package lucky.id.generator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @Author luckylau
 * @Date 2023/7/15
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IdGeneratorException.class)
    public ResponseEntity<String> handleException(IdGeneratorException ex) {
        // 处理异常逻辑
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
