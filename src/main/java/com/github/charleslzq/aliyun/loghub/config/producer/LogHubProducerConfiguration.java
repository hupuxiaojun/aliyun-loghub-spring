package com.github.charleslzq.aliyun.loghub.config.producer;

import com.aliyun.openservices.log.producer.LogProducer;
import com.github.charleslzq.aliyun.loghub.annotation.support.LogHubProjectBeanPostProcessor;
import com.github.charleslzq.aliyun.loghub.annotation.support.LogHubStoreBeanPostProcessor;
import com.github.charleslzq.aliyun.loghub.annotation.support.LogHubTopicBeanPostProcessor;
import com.github.charleslzq.aliyun.loghub.config.LogHubProjectProperties;
import com.github.charleslzq.aliyun.loghub.producer.DefaultLogItemConversionService;
import com.github.charleslzq.aliyun.loghub.producer.LogHubMessageSendingTemplate;
import com.github.charleslzq.aliyun.loghub.producer.LogHubProducerTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Configuration
@EnableConfigurationProperties({
        LogHubProducerProperties.class,
        LogHubProjectProperties.class
})
public class LogHubProducerConfiguration {

    @Autowired
    protected DefaultListableBeanFactory defaultListableBeanFactory;
    @Autowired
    private LogHubProducerProperties logHubProducerProperties;
    @Autowired
    private LogHubProjectProperties logHubProjectProperties;
    private String hostIp = "127.0.0.1";
    private String hostName = "localhost";

    @Bean
    @ConditionalOnMissingBean(name = "logHubProducerConversionService")
    public ConversionService logHubProducerConversionService() {
        return new DefaultLogItemConversionService();
    }

    @Bean
    public LogHubProducerTemplate logHubProducerTemplate(@Qualifier("logHubProducerConversionService") ConversionService conversionService) {
        try {
            hostIp = InetAddress.getLocalHost().getHostAddress();
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Error when accessing host ip and name", e);
        }
        String source = logHubProducerProperties.getSource() == SourceType.HOST_IP ? hostIp : hostName;
        LogProducer logProducer = new LogProducer(logHubProducerProperties.generateProducerConfig());
        logHubProjectProperties.generateProjectConfig().forEach(logProducer::setProjectConfig);

        return new LogHubProducerTemplate(logProducer, source, conversionService, logHubProducerProperties.isFlushImmediately());
    }

    @Bean
    public LogHubMessageSendingTemplate logHubMessageSendingTemplate(
            LogHubProducerTemplate logHubProducerTemplate
    ) {
        return new LogHubMessageSendingTemplate(logHubProducerTemplate);
    }

    @Bean
    public LogHubProjectBeanPostProcessor logHubProjectBeanPostProcessor(LogHubProducerTemplate logHubProducerTemplate) {
        return new LogHubProjectBeanPostProcessor(logHubProducerTemplate, defaultListableBeanFactory);
    }

    @Bean
    public LogHubStoreBeanPostProcessor logHubStoreBeanPostProcessor(LogHubProducerTemplate logHubProducerTemplate) {
        return new LogHubStoreBeanPostProcessor(logHubProducerTemplate, defaultListableBeanFactory);
    }

    @Bean
    public LogHubTopicBeanPostProcessor logHubTopicBeanPostProcessor(LogHubProducerTemplate logHubProducerTemplate) {
        return new LogHubTopicBeanPostProcessor(logHubProducerTemplate, defaultListableBeanFactory);
    }
}
