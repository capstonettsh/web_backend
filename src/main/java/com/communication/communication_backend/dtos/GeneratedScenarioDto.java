package com.communication.communication_backend.dtos;

public class GeneratedScenarioDto {
    private String taskInstruction;
    private String backgroundInformation;
    private String personality;
    private String questionsForDoctor;
    private String responseGuidelines;
    private String sampleResponses;

    public GeneratedScenarioDto(String taskInstruction, String backgroundInformation, String personality, 
                                String questionsForDoctor, String responseGuidelines, String sampleResponses) {
        this.taskInstruction = taskInstruction;
        this.backgroundInformation = backgroundInformation;
        this.personality = personality;
        this.questionsForDoctor = questionsForDoctor;
        this.responseGuidelines = responseGuidelines;
        this.sampleResponses = sampleResponses;
    }

    // Getters and setters
    public String getTaskInstruction() { return taskInstruction; }
    public void setTaskInstruction(String taskInstruction) { this.taskInstruction = taskInstruction; }

    public String getBackgroundInformation() { return backgroundInformation; }
    public void setBackgroundInformation(String backgroundInformation) { this.backgroundInformation = backgroundInformation; }

    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }

    public String getQuestionsForDoctor() { return questionsForDoctor; }
    public void setQuestionsForDoctor(String questionsForDoctor) { this.questionsForDoctor = questionsForDoctor; }

    public String getResponseGuidelines() { return responseGuidelines; }
    public void setResponseGuidelines(String responseGuidelines) { this.responseGuidelines = responseGuidelines; }

    public String getSampleResponses() { return sampleResponses; }
    public void setSampleResponses(String sampleResponses) { this.sampleResponses = sampleResponses; }    
}
