package at.nice.tc.aiTools.JiraTool;

import at.nice.tc.service.JiraService;
import at.nice.tc.utils.JiraUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JiraTools {

    private final JiraService jiraService;


//    @Cacheable(value = "jiraATestFromJira", key = "#id + ':' + #fields.toString()") // FIXME: кэширование тупое

    /// /    @Tool(description = "Получает информацию о тест кейсе по его ID из Jira.  с выбранными полями (json-свойствами)")
//    @Tool(description = "Получает информацию о тест-кейсе по его ID из Jira. " +
//            "Позволяет выбрать конкретные поля для возврата, используя их имена из JSON-схемы. " +
//            "Имя поля можно определить по его описанию в JSON-файле " +
//            "(например, для 'Название/заголовок тест-кейса' используйте 'name', для 'Текущий статус тест-кейса' - 'status').")
//    public String readTestFromJira(
//
//            @ToolParam(description = "id теста")
//            String id,
//
//            @ToolParam(description =
//                    "Перечень доступных свойств с описанием можно получить с помощью getAvailableTestProperties. " +
//                    "Позволяет выбрать конкретные поля для возврата, используя их имена из JSON-схемы. " +
//                    "Имя поля можно определить по его описанию в JSON-файле " +
//                    "(например, для 'Название/заголовок тест-кейса' используйте 'name', для 'Текущий статус тест-кейса' - 'status')." +
//                    "Можно выбирать только ключи которые находятся в JSON")
//            List<String> fields
// ...
    @Tool(description = """
            Получает данные тест-кейса из Jira.
            Позволяет указать, какие именно свойства (поля) нужно вернуть.
            Это экономит время и возвращает только нужную информацию.
            Пример вызова: readTestFromJira('PROJ-123', ['name', 'status'])
            """)
    public String readTestFromJira(
            @ToolParam(description = "Уникальный идентификатор тест-кейса в Jira.")
            String id,

            @ToolParam(description = """
                    Какие поля тест-кейса нужно вернуть.
                    Указывайте имена, как они представлены в JSON (например, 'testScript.steps.attachments.fileName', 'testScript.steps.testCase').
                    Пример: ['testScript.steps.attachments.fileName', 'testScript.steps.testCase'].
                    Полный список: вызовите getAvailableTestProperties().
                    Выбирай все свойства id и добавляй только необходимые свойства
                    """)
            List<String> fields

    ) {
        return jiraService.getTest(id, fields)
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTestJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .thenApplyAsync(JiraUtils::toString)
                .join();
    }

    //    @Cacheable("availableTestProperties")
    @Tool(name = "getAvailableTestProperties", description = "Перечень доступных свойств тест-кейса")
    public List<String> getAvailableTestProperties(@ToolParam(description = "передай букву 'a'") String ignore) throws IOException {
        return jiraService.getAvailableTestProperties();
    }


    @Tool(description = "Получает информацию о доступности Jira")
    public String isAvailable(@ToolParam(description = "передай букву 'a'") String ignore) {//todo ignore избавиться от заплатки в виде параметра в методе
        return jiraService.isAvailable().join().toString();
    }

    @Cacheable(value = "jiraAllVersions", key = "#testKey")
    @Tool(name = "getAllVersions", description = "Получает информацию о версиях теста по его ID из Jira, например VPEPVV-T800")
    public String getAllVersions(String testKey) {
        return jiraService.getAllVersionsAsync(testKey).join().toString();
    }


    @Tool(description = "Получает информацию о ручных выполнениях теста")
    public String getTestExecutions(
            @ToolParam(description = "id конкретной версии тест-кейса, например 12345. Можно получить с помощью getAllVersions")
            int versionId,
            @ToolParam(description = "Перечень свойств, которые должен содержать результирующий json. " +
                    "Список доступных свойств можно получить с помощью getAvailableTestExecutionProperties. " +
                    "Выбирай только необходимые свойства!")
            List<String> fields) {
        return jiraService.getTestExecutions(versionId, fields).join();
    }

    //    @Cacheable("availableTestExecutionProperties")
    @Tool(name = "getAvailableTestExecutionProperties", description = "Перечень доступных свойств выполнений тест-кейс")
    public List<String> getAvailableTestExecutionProperties(@ToolParam(description = "передай букву 'a'") String ignore) throws IOException {
        return jiraService.getAvailableTestExecutionProperties();
    }
}
