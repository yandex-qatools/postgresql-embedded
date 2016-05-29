package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.io.IStreamProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isEmpty;

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

    public LogWatchStreamProcessor(String success, Set<String> failures, IStreamProcessor destination) {
        super(success, failures, destination);
        this.success = success;
        this.failures = failures;
    }

    @Override
    public void process(String block) {
        LOGGER.debug(block);
        output.append(block).append("\n");
        if (containsSuccess(block) || containsFailure(block)) {
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

    public void waitForResult(long timeout) {
        synchronized (mutex) {
            try {
                if (!found) {
                    mutex.wait(timeout);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getOutput() {
        String res = output.toString();
        return isEmpty(res) ? null : res;
    }
}
