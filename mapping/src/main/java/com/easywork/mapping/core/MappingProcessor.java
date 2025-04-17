package com.easywork.mapping.core;

import java.util.ArrayList;
import java.util.List;

import com.easywork.mapping.core.api.Converter;
import com.easywork.mapping.core.api.Generator;
import com.easywork.mapping.core.api.Parser;
import com.easywork.mapping.core.api.PreProcessor;
import com.easywork.mapping.core.api.PostProcessor;
import com.easywork.mapping.core.api.Validator;
import com.easywork.mapping.core.support.ValidationResult;

/**
 * 映射处理器
 * 负责整合和执行整个映射转换流程
 */
public class MappingProcessor {
    private List<PreProcessor> preProcessors;
    private List<Validator> validators;
    private Parser parser;
    private Converter converter;
    private Generator generator;
    private List<PostProcessor> postProcessors;

    private MappingProcessor() {
        this.preProcessors = new ArrayList<>();
        this.validators = new ArrayList<>();
        this.postProcessors = new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public MappingResult process(String input) {
        // 预处理
        String processedInput = input;
        for (PreProcessor processor : preProcessors) {
            processedInput = processor.process(processedInput);
        }

        // 验证
        ValidationResult validationResult = new ValidationResult();
        for (Validator validator : validators) {
            ValidationResult result = validator.validate(processedInput);
            if (!result.isValid()) {
                validationResult.getErrors().addAll(result.getErrors());
            }
        }

        if (!validationResult.isValid()) {
            return MappingResult.error(validationResult.getErrors());
        }

        // 解析
        Object parsedData = parser.parse(processedInput);

        // 转换
        Object convertedData = converter.convert(parsedData);

        // 生成
        String output = generator.generate(convertedData);

        // 后处理
        for (PostProcessor processor : postProcessors) {
            output = processor.process(output);
        }

        return MappingResult.success(output);
    }

    public static class Builder {
        private MappingProcessor processor;

        private Builder() {
            processor = new MappingProcessor();
        }

        public Builder addPreProcessor(PreProcessor preProcessor) {
            processor.preProcessors.add(preProcessor);
            return this;
        }

        public Builder addValidator(Validator validator) {
            processor.validators.add(validator);
            return this;
        }

        public Builder setParser(Parser parser) {
            processor.parser = parser;
            return this;
        }

        public Builder setConverter(Converter converter) {
            processor.converter = converter;
            return this;
        }

        public Builder setGenerator(Generator generator) {
            processor.generator = generator;
            return this;
        }

        public Builder addPostProcessor(PostProcessor postProcessor) {
            processor.postProcessors.add(postProcessor);
            return this;
        }

        public MappingProcessor build() {
            if (processor.parser == null) {
                throw new IllegalStateException("Parser must be set");
            }
            if (processor.converter == null) {
                throw new IllegalStateException("Converter must be set");
            }
            if (processor.generator == null) {
                throw new IllegalStateException("Generator must be set");
            }
            return processor;
        }
    }
}