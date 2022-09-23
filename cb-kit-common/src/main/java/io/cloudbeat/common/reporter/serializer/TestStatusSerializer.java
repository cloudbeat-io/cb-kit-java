package io.cloudbeat.common.reporter.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.cloudbeat.common.reporter.model.TestStatus;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TestStatusSerializer extends JsonSerializer<TestStatus> {
    @Override
    public void serialize(TestStatus value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(value.toString().toLowerCase());
    }
}