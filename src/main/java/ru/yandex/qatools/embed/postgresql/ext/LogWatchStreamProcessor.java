package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.io.IStreamProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * @author Ilya Sadykov
 */
public class LogWatchStreamProcessor extends de.flapdoodle.embed.process.io.LogWatchStreamProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchStreamProcessor.class);
    private final StringBuilder output = new StringBuilder();
    private final Object mutex = new Object();
    private final String success;
    private final Set<String> failures;
    private volatile boolean found = false;
    private volatile boolean initWithSuccess = false;

    public LogWatchStreamProcessor(String success, Set<String> failures, IStreamProcessor destination) {
        super(success, failures, destination);
        this.success = ofNullable(success).orElse("");
        this.failures = failures;
    }

    @Override
    public void process(String block) {
        LOGGER.debug(block);
        output.append(block).append("\n");
        initWithSuccess = containsSuccess(block);
        if (initWithSuccess || containsFailure(block)) {
            synchronized (mutex) {
                found = true;
                mutex.notifyAll();
            }
        } else {
            super.process(block);
        }
    }

    private boolean containsSuccess(String block) {
        return block.contains(success);
    }

    private boolean containsFailure(String block) {
        for (String failure : failures) {
            if (block.contains(failure)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void waitForResult(long timeout) {
        synchronized (mutex) {
            try {
                if (!found) {
                    mutex.wait(timeout);
                }
            } catch (InterruptedException e) {
                LOGGER.error("Failed to wait for the result: '{}' not found in: \n{}", success, output);
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isInitWithSuccess() {
        return initWithSuccess || getOutput().contains(success);
    }

    @Override
    public String getOutput() {
        return output.toString();
    }
}
