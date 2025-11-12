package at.nice.tc.ai.dto.jira;

import java.util.List;

public record DTOTestWithNested(Integer id, Integer majorVersion, String key, TestScriptDTO testScript) {

    public record TestScriptDTO(StepByStepScriptDTO stepByStepScript) {
    }

    public record StepByStepScriptDTO(List<StepDTO> steps) {
    }

    public record StepDTO(Integer index, TestCaseDTO testCase) {
    }

    public record TestCaseDTO(Integer id, String key, Integer majorVersion) {
    }
}

