package at.nice.tc.controller;

import at.nice.tc.model.Test;
import at.nice.tc.service.Example_JiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST контроллер для работы с Jira API
 */
@RestController
@RequestMapping("/api/jira")
@CrossOrigin(origins = "*")
public class Example_JiraController {

    @Autowired
    private Example_JiraService jiraService;

    /**
     * Получить список версий теста по ID
     * @param testId ID теста в формате T777
     * @return Список версий
     */
    @GetMapping("/tests/{testId}")
    public ResponseEntity<List<String>> getTestVersions(@PathVariable String testId) {
        try {
            List<String> versions = jiraService.getTestVersions(testId);
            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Получить список ID версий тестов в прогоне
     * @param runId ID прогона в формате C777
     * @return Список ID версий тестов
     */
    @GetMapping("/runs/{runId}")
    public ResponseEntity<List<String>> getRunTestVersions(@PathVariable String runId) {
        try {
            List<String> versionIds = jiraService.getRunTestVersions(runId);
            return ResponseEntity.ok(versionIds);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Получить данные версии теста
     * @param versionId ID версии в формате 12345
     * @return Данные теста
     */
    @GetMapping("/versions/{versionId}")
    public ResponseEntity<Test> getTestVersion(@PathVariable String versionId) {
        try {
            Test test = jiraService.getTestVersion(versionId);
            return ResponseEntity.ok(test);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Получить статус соединения с Jira
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        try {
            boolean isConnected = jiraService.checkConnection();
            if (isConnected) {
                return ResponseEntity.ok("connected");
            } else {
                return ResponseEntity.ok("disconnected");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
    }
}
