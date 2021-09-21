package com.happymoney.productionobservability.logging;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Custom implementation of JSON layout for Log4j2.
 */
@Plugin(name = "JsonLogLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class JsonLogLayout extends AbstractStringLayout {
    private static final String EOL = "\r\n";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    private final ObjectMapper mapper;
    private final boolean includeThread;
    private final boolean includeAllGit;
    private final KeyValuePair[] additionalFields;
    private final Map<String, String> gitValues = new HashMap<>();
    private final String runtimeId = UUID.randomUUID().toString();

    protected JsonLogLayout(boolean includeThread, boolean includeAllGit, KeyValuePair[] additionalFields, Charset charset) {
        super(charset);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.includeThread = includeThread;
        this.includeAllGit = includeAllGit;
        this.additionalFields = additionalFields;
        mapper = new ObjectMapper(new JsonFactory());
        loadGitValues();
    }

    @PluginFactory
    public static JsonLogLayout createLayout(@PluginAttribute("includeThread") boolean includeThread,
                                             @PluginAttribute(value = "includeAllGit") boolean includeAllGit,
                                             @PluginElement("AdditionalField") KeyValuePair[] additionalFields) {
        return new JsonLogLayout(includeThread, includeAllGit, additionalFields, Charset.defaultCharset());
    }

    private void loadGitValues() {
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("git.properties");
            if (stream == null) {
                return;
            }
            Properties properties = new Properties();
            properties.load(stream);
            if (includeAllGit) {
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    if (value != null && !value.isEmpty()) {
                        StringBuilder builder = new StringBuilder();
                        for (String fragment : key.split("\\.")) {
                            if (builder.length() == 0) {
                                builder.append(fragment);
                            } else if (!fragment.isEmpty()) {
                                builder.append(fragment.substring(0, 1).toUpperCase());
                                if (fragment.length() > 1) {
                                    builder.append(fragment.substring(1));
                                }
                            }
                        }
                        gitValues.put(builder.toString(), value);
                    }
                }
            } else {
                String commitId = properties.getProperty("git.commit.id.abbrev");
                if (commitId == null || commitId.isEmpty()) {
                    commitId = properties.getProperty("git.commit.id");
                }
                if (commitId != null) {
                    gitValues.put("gitCommitId", commitId);
                }
                String tags = properties.getProperty("git.tags");
                if (tags != null && !tags.isEmpty()) {
                    gitValues.put("gitTags", tags);
                }
            }
        } catch (IOException e) {
            // Ignore if the git.properties is not set.
        }
    }

    @Override
    public String toSerializable(LogEvent event) {
        try {
            Map<Object, Object> json = new LinkedHashMap<>();
            Marker marker = event.getMarker();

            json.put(Key.timestamp, dateFormat.format(new Date(event.getTimeMillis())));
            json.put(Key.level, event.getLevel().getStandardLevel());

            json.put(Key.message, event.getMessage().getFormattedMessage());

            ThreadContext.getImmutableContext().forEach(json::put);

            String loggerName = event.getLoggerName();
            json.put(Key.name, loggerName);

            StackTraceElement source = event.getSource();
            if (source != null) {
                String className = source.getClassName();
                if (!loggerName.equals(className)) {
                    json.put(Key.className, className);
                }
                json.put(Key.method, source.getMethodName());
                json.put(Key.line, source.getLineNumber());
            }

            if (marker != null) {
                json.put(Key.marker, marker.getName());
            }

            json.put(Key.runtimeId, runtimeId);

            if (includeThread) {
                json.put(Key.thread, event.getThreadName());
                json.put(Key.threadId, event.getThreadId());
                json.put(Key.threadPriority, event.getThreadPriority());
            }

            Throwable thrown = event.getThrown();
            if (thrown != null) {
                Map<Key, String> exceptionJson = new HashMap<>();
                exceptionJson.put(Key.name, thrown.getClass().getName());
                exceptionJson.put(Key.message, thrown.getMessage());

                StringWriter writer = new StringWriter();
                thrown.printStackTrace(new PrintWriter(writer));

                exceptionJson.put(Key.stackTrace, writer.toString());
                json.put(Key.exception, exceptionJson);
            }

            if (additionalFields != null) {
                for (KeyValuePair additionalField : additionalFields) {
                    json.put(additionalField.getKey(), additionalField.getValue());
                }
            }

            applyAdditionalLogging(event, json);

            if (!gitValues.isEmpty()) {
                json.putAll(gitValues);
            }
            return mapper.writeValueAsString(json) + EOL;
        } catch (Throwable t) {
            return "Failed to write out JSON: " + t.getMessage();
        }
    }

    /**
     * Add additional logging to the map.
     * @param event  Log event.
     * @param json   Map of logging data for this event.
     */
    protected void applyAdditionalLogging(LogEvent event, Map<Object, Object> json) {
    }

    private enum Key { timestamp, level, message, name, className, method, line, marker, runtimeId, thread, threadId,
        threadPriority, stackTrace, exception }
}
