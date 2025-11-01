package at.nice.tc.aiTools.JiraTool;

import dto.testCase.VersionDTO;
import impl.Step;

import java.util.List;

public interface Jira {

    String getFullTest(String id);

    String getTest(String id, List<String> fields);

    String getLastUpdate(String id);

    boolean isAvailable();

    List<VersionDTO> getAllVersions(String testKey);
}
