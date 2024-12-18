package com.dcg.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentConverter {
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    public String convertToTargetFormat(Object source, String sourceType, String targetType) {
        try {
            // First convert to object
            Object data = source;
            if (source instanceof String) {
                if ("xml".equalsIgnoreCase(sourceType)) {
                    data = xmlMapper.readValue((String) source, Object.class);
                } else {
                    data = jsonMapper.readValue((String) source, Object.class);
                }
            }

            // Then convert to target format
            if ("xml".equalsIgnoreCase(targetType)) {
                return xmlMapper.writeValueAsString(data);
            } else {
                return jsonMapper.writeValueAsString(data);
            }
        } catch (JsonProcessingException e) {
            log.error("Convert content error", e);
            throw new RuntimeException("Convert content error", e);
        }
    }
} 