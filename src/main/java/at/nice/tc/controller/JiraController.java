package at.nice.tc.controller;

import at.nice.tc.dto.JiraFieldDTO;
import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import at.nice.tc.service.JiraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/jira")
public class JiraController {

    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }


    /**
     * Возвращает список полей теста
     */
    @GetMapping("/fields")
    public ResponseEntity<List<JiraFieldDTO>> getFields() {
        try {
            List<JiraFieldDTO> fields = jiraService.getFields();
            if (fields == null)
                return ResponseEntity.notFound().build(); // 404 Not Found
            return ResponseEntity.ok(fields);
        } catch (Throwable t) {
            return ResponseEntity.internalServerError().build(); // 500
        }
    }

    /**
     * @param id идентификатор определенной версии (12345), а не теста (ABCDE-T777)
     */
    @GetMapping("/versions/{id}")
    public CompletableFuture<ResponseEntity<JiraTestDTO>> readTest(@PathVariable String id) {
        return jiraService.readTestAsync(id)
                .handle((result, throwable) -> {
                    if (throwable != null)
                        return ResponseEntity.internalServerError().build(); // 500
                    if (result != null) {
                        return ResponseEntity.ok(result); // 200 OK
                    } else {
                        return ResponseEntity.notFound().build(); // 404 Not Found
                    }
                });
    }

    /**
     * @param testId идентификатор теста (ABCDE-T777)
     */
    @GetMapping("/tests/{id}")
    public CompletableFuture<ResponseEntity<List<JiraTestVersionDTO>>> searchVersions(@PathVariable String testId) {
        return jiraService.searchVersionsAsync(testId)
                .handle((result, throwable) -> {
                    if (throwable != null)
                        return ResponseEntity.internalServerError().build(); // 500
                    if (result != null) {
                        return ResponseEntity.ok(result); // 200 OK
                    } else {
                        return ResponseEntity.notFound().build(); // 404 Not Found
                    }
                });
    }

    /**
     * Возвращает список id тестов (вида 12345) в порядке использования в прогоне  TODO: на фронте можно посчитать и отрисовать вкладки на сайдбаре, а каждый тест запросить через /versions/{id}
     * @param runId идентификатор прогона (ABCDE-С777)
     */
    @GetMapping("/runs/{id}")
    public CompletableFuture<ResponseEntity<List<JiraTestVersionDTO>>> searchRun(@PathVariable String runId) {
        return jiraService.readRunAsUsedTestVersionsAsync(runId)
                .handle((result, throwable) -> {
                    if (throwable != null)
                        return ResponseEntity.internalServerError().build(); // 500
                    if (result != null) {
                        return ResponseEntity.ok(result); // 200 OK
                    } else {
                        return ResponseEntity.notFound().build(); // 404 Not Found
                    }
                });
    }
}