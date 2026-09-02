package com.fintechathon.silvershield.agent.application;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskExecutor {

    private final ExecutorService executor = Executors.newFixedThreadPool(8, new AgentThreadFactory());

    public <T> T call(Callable<T> task, Duration timeout, String stage) {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new AgentStageTimeoutException(stage, exception);
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new AgentStageTimeoutException(stage, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(stage + " failed", cause);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public static final class AgentStageTimeoutException extends RuntimeException {
        private final String stage;

        private AgentStageTimeoutException(String stage, Throwable cause) {
            super(stage + " timed out", cause);
            this.stage = stage;
        }

        public String stage() { return stage; }
    }

    private static final class AgentThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "banking-agent-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
