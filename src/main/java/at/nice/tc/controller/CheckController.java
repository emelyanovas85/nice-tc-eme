package at.nice.tc.controller;

import at.nice.tc.dto.CheckDTO;
import at.nice.tc.dto.JiraFieldDTO;
import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import at.nice.tc.service.CheckService;
import at.nice.tc.service.JiraService;
import at.nice.tc.utils.ThrowableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;


    /**
     * Получить статус соединения с Jira
     */
    @GetMapping("/")
    public ResponseEntity<List<CheckDTO>> getAllChecks() {
        try {
            return ResponseEntity.ok(checkService.getAllChecks());
        } catch (Throwable t) {
            log.warn("", t);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * Получить статус соединения с Jira
     */
    @PutMapping("/{id}/update")
    public ResponseEntity<Boolean> updateCheck(@PathVariable String id, @RequestBody String newPrompt) {
        try {
            checkService.updateCheck(id, newPrompt);
            return ResponseEntity.ok(true);
        } catch (Throwable t) {
            log.warn("", t);
            return ResponseEntity.status(500).body(false);
        }
    }
}