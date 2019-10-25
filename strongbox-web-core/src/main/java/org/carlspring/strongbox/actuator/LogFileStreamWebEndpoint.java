package org.carlspring.strongbox.actuator;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.logging.LogFile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Przemyslaw Fusik
 * @see LogFileWebEndpoint
 */
@WebEndpoint(id = "logfilestream")
public class LogFileStreamWebEndpoint
{

    private static final String SSE_TIMEOUT_PROPERTY_NAME = "strongbox.sse.timeoutMillis";

    private static final Long TIMEOUT_MILLIS = Long.valueOf(3600000l);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Environment environment;

    private File externalFile;

    public LogFileStreamWebEndpoint(Environment environment,
                                    File externalFile)
    {
        this.environment = environment;
        this.externalFile = externalFile;
    }

    @ReadOperation(produces = "text/plain; charset=UTF-8")
    public SseEmitter logFileStream()
            throws IOException
    {
        final SseEmitter sseEmitter = new SseEmitter(
                environment.getProperty(SSE_TIMEOUT_PROPERTY_NAME, Long.class, TIMEOUT_MILLIS));
        Resource logFileResource = getLogFileResource();
        if (logFileResource == null)
        {
            sseEmitter.completeWithError(
                    new IllegalStateException("Missing 'logging.file' or 'logging.path' properties"));
            return sseEmitter;
        }

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        forkJoinPool.execute(
                Tailer.create(logFileResource.getFile(),
                              new SseEmitterAwareTailerListenerAdapter(sseEmitter),
                              1000,
                              true));

        return sseEmitter;
    }

    /**
     * @see LogFileWebEndpoint#getLogFileResource()
     */
    private Resource getLogFileResource()
    {
        if (this.externalFile != null)
        {
            return new FileSystemResource(this.externalFile);
        }
        LogFile logFile = LogFile.get(this.environment);
        if (logFile == null)
        {
            logger.debug("Missing 'logging.file' or 'logging.path' properties");
            return null;
        }
        return new FileSystemResource(logFile.toString());
    }
}
