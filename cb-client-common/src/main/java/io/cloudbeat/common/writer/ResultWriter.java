package io.cloudbeat.common.writer;

//import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.cloudbeat.common.reporter.model.TestResult;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public final class ResultWriter {
    public static void writeResult(TestResult result, String resultFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        //mapper.registerModule(new Jdk8Module());
        String resultJson;
        try {
            resultJson = mapper.writeValueAsString(result);
        } catch (Throwable e) { /*JsonProcessingException*/
            System.out.println("Failed to serialize results, error: " + e.toString());
            Arrays.stream(e.getStackTrace()).forEach(x -> System.out.println(x.toString()));
            return;
        }

        try {
            PrintWriter writer = new PrintWriter(resultFilePath, "UTF-8");
            writer.write(resultJson);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            System.err.println("Failed to create " + resultFilePath + ", error: " + e.toString());
        }
    }
}
