package at.nice.tc.aiTools.JiraTool;

import at.nice.tc.utils.ThrowableUtils;
import bugbusters.modules.restclients.httpclient.HttpClient;
import bugbusters.modules.restclients.httpclient.HttpRequest;
import dto.testCase.VersionDTO;
import impl.Step;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import jiraClient.JiraClientSingleton;
import lombok.RequiredArgsConstructor;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    public String getTest(String id, List<String> fields) {
        if (fields == null)
            return getFullTest(id);
        return testCaseAPI.requestToJira(id, fields.toArray(new String[0]));
    }

    @Override
    public String getTestExecutions(int versionId, List<String> fields) {
        if (fields == null || fields.isEmpty())
            // Без указания полей возвращает 500
            fields = List.of("testResultStatus","environment","key","userKey","assignedTo","jiraVersionId",
                    "estimatedTime","executionTime","executionDate","automated","testRun","testCase","issueLinks","sprint");

        HttpClient httpClient = JiraClientSingleton.getJiraClient().getHttpClient();
        HttpRequest request = httpClient.GET("/rest/tests/1.0/testcase/" + versionId + "/testresults");
        request.addQueryParameter("fields", String.join(",", fields)); // повторное присваивание не срабатывает, приходится вручную джойнить через запятую
        try (Response response = request.execute()) {
            ResponseBody body = response.body();
            return (body != null) ? body.string() : "";
        } catch (Exception ex) {
            return ThrowableUtils.reThrow(ex);
        }
    }

    @Override
    public String getLastUpdate(String id) {
        return testCaseAPI.requestToJira(id, "updatedOn");
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
