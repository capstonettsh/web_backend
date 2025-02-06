package com.communication.doctorconfig;

public class SettingKafkaTopicName {
    private final String sessionDateTime;
    private final String userId;

    public SettingKafkaTopicName(String sessionDateTime, String userId) {
        this.sessionDateTime = sessionDateTime;
        this.userId = userId;
    }

    private String getBase() {
        return userId + "_" + sessionDateTime + "_config";
    }

    public synchronized String getSetting() {
        return getBase();
    }

    public synchronized String getTitleandDescInput() {
        return getBase() + "-titleanddescinput";
    }

    public synchronized String getGeneratedResponse() {
        return getBase() + "-generated-responses";
    }

    public synchronized String getScenario() {
        return getBase() + "-scenario";
    }

    public synchronized String getInstructions() {
        return getBase() + "-instructions";
    }

    public synchronized String getBackground() {
        return getBase() + "-background";
    }

    public synchronized String getPersonality() {
        return getBase() + "-personality";
    }

    public synchronized String getOpening() {
        return getBase() + "-opening";
    }

    public synchronized String getQuestionsfordoctor() {
        return getBase() + "-questionsfordoctor";
    }

    public synchronized String getResponseGuidelines() {
        return getBase() + "-response-guidelines";
    }

    public synchronized String getSampleResponses() {
        return getBase() + "-sample-responses";
    }

    public synchronized String getRubricCriteriaInput() {
        return getBase() + "-rubric-criteria-input";
    }

    public synchronized String getGeneratedRubrics() {
        return getBase() + "-rubrics";
    }
}
