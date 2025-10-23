package at.nice.tc.dao.jira;

import dto.testCase.VersionDTO;
import impl.Step;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Lazy
@Service
public class JiraImpl implements Jira {
    private final JiraTestCaseAPI testCaseAPI;
    private final JiraTestRunAPI testRunAPI;
    private final JiraClient jiraClient;


    @Override
    public String getFullTest(String id) {
        return testCaseAPI.requestToJira(id);
    }

    @Override
    public String getLastUpdate(String id) {
        return testCaseAPI.requestToJira(id, "updatedOn");
    }

    @Override
    public List<Step> stepByStepScript(String id) {
        return testCaseAPI.getTestSteps(id);
    }

    @Override
    public boolean isAvailable() {
        return jiraClient.isAvailable();
    }

    @Override
    public List<VersionDTO> getAllVersions(String testKey) {
        return testCaseAPI.getAllVersionsTestCaseById(testKey);
    }
}
