package at.nice.tc.dao.jira;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import dto.testCase.VersionDTO;
import impl.TestCase;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class JiraRepo implements Jira {
    private JiraTestCaseAPI testCaseAPI;
    private JiraTestRunAPI testRunAPI;
    private JiraClient jiraClient;

    @Override
    public boolean isAvailable() {
        return jiraClient.isAvailable();
    }

    @Override
    public JiraTestDTO readTestFromJira(String id) {
        //PERUFR-T5?fields=
        // id,
        // projectId,
        // archived,
        // key,
        // name,
        // objective,
        // majorVersion,
        // latestVersion,
        // precondition,
        // folder(id,fullName),
        // status,
        // priority,
        // estimatedTime,
        // averageTime,
        // componentId,
        // owner,
        // labels,
        // customFieldValues,
        // testScript(id,text,steps(index,description,text,expectedResult,testData,attachments,customFieldValues,id,stepParameters(id,testCaseParameterId,value),testCase(id,key,name,archived,majorVersion,latestVersion,parameters(id,name,defaultValue,index)))),
        // testData,
        // parameters(id,name,defaultValue,index),
        // paramType
//            String testCase = testCaseAPI.requestToJira(id, "id","name", "objective", "" );
        final TestCase testCase = testCaseAPI.getTestCase(id);
        return JiraTestDTO
                .builder()
                .id(testCase.getId())
                .name(testCase.getName())
                .objective("objective")
                .steps(testCase.getSteps().stream().map(Objects::toString).toList()) //FIXME: Step это просто строки, но из JiraClient ДТО с полями
                .author("author")
                .status("ststus")
                .precondition("precondition")
                .labels(List.of("labels"))
                .priority(testCase.getPriority())
                .build();
    }

    @Override
    public List<JiraTestVersionDTO> getAllVersions(String testKey) {
        List<VersionDTO> allVersionsTestCaseById = testCaseAPI.getAllVersionsTestCaseById(testKey);
        return allVersionsTestCaseById.stream()
                .map(v -> JiraTestVersionDTO.builder()
                        .testKey(testKey)
                        .id(String.valueOf(v.getId()))
                        .version(String.valueOf(v.getMajorVersion()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId) {
        return List.of(JiraTestVersionDTO.builder().build());
    }
}
