package io.cloudbeat.common.reporter.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class EpochTimeSerializer extends JsonSerializer<Long> {

    //private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss\'Z\'");

    public EpochTimeSerializer() {
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        //SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        String formattedString =  value != null ? formatter.format(value) : null;
        gen.writeString(formattedString);
    }
}