//package com.communication.communication_backend.service.facialAnalysis;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.context.ApplicationPidFileWriter;
//import org.springframework.context.ApplicationContext;
//import org.springframework.core.env.Environment;
//
//@SpringBootApplication
//public class KafkaemotionApplication {
//
//    private final FinalOutputConsumer finalOutputConsumer;
//
//    @Autowired
//    private Environment env;
//
//    public KafkaemotionApplication(FinalOutputConsumer finalOutputConsumer) {
//        this.finalOutputConsumer = finalOutputConsumer;
//    }
//
//    public static void main(String[] args) {
//        ApplicationContext context = SpringApplication.run(KafkaemotionApplication.class, args);
//
//        // Add a shutdown hook to write the final output
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            FinalOutputConsumer consumer = context.getBean(FinalOutputConsumer.class);
//            consumer.writeFinalOutput();
//        }));
//
//        SpringApplication app = new SpringApplication(KafkaemotionApplication.class);
//        app.addListeners(new ApplicationPidFileWriter());
//        app.run(args);
//    }
//
//    @PreDestroy
//    public void onExit() {
//        System.out.println("Shutting down gracefully...");
//        // Ensure the final output is written during shutdown
//        finalOutputConsumer.writeFinalOutput();
//    }
//
//    @PostConstruct
//    public void validateApiKeyResolution() {
//        String propertyKey = env.getProperty("open.ai.key");
//        System.out.println("PostConstruct - open.ai.key from Environment: " + (propertyKey != null ? "Present" : "Missing"));
//    }
//}