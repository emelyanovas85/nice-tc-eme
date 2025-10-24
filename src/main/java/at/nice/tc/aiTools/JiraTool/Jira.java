package at.nice.tc.aiTools.JiraTool;

import dto.testCase.VersionDTO;
import impl.Step;

import java.util.List;

public interface Jira {

    String getFullTest(String id);

    String getLastUpdate(String id);

    List<Step> stepByStepScript(String id);

    boolean isAvailable();

    List<VersionDTO> getAllVersions(String testKey);
}
