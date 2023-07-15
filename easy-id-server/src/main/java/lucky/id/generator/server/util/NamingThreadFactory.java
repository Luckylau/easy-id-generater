package lucky.id.generator.server.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author luckylau
 * @Date 2023/6/4
 */
@Slf4j
public class NamingThreadFactory implements ThreadFactory {
    /**
     * Sequences for multi thread name prefix
     */
    private final AtomicLong sequence;
    /**
     * Thread name pre
     */
    private String name;
    /**
     * Is daemon thread
     */
    private boolean daemon;
    /**
     * UncaughtExceptionHandler
     */
    private UncaughtExceptionHandler uncaughtExceptionHandler;

    public NamingThreadFactory(String name, boolean daemon, UncaughtExceptionHandler handler) {
        this.name = name;
        this.daemon = daemon;
        this.uncaughtExceptionHandler = handler;
        this.sequence = new AtomicLong(0);
    }

    public NamingThreadFactory(String name, boolean daemon) {
        this(name, daemon, null);
    }


    public NamingThreadFactory(String name) {
        this(name, false, null);
    }

    public static NamingThreadFactory create(String name) {
        return NamingThreadFactory.create(name, false);
    }

    public static NamingThreadFactory create(String name, boolean daemon) {
        return new NamingThreadFactory(name, daemon);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(this.daemon);
        thread.setName(this.name + "-" + sequence.incrementAndGet());

        // no specified uncaughtExceptionHandler, just do logging.
        if (this.uncaughtExceptionHandler != null) {
            thread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        } else {
            thread.setUncaughtExceptionHandler((t, e) -> log.error("unhandled exception in thread: " + t.getId() + ":" + t.getName(), e));
        }

        return thread;
    }
}
