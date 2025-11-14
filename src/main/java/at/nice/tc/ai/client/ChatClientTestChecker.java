package at.nice.tc.ai.client;

import at.nice.tc.ai.tools.googleTool.GoogleTools;
import at.nice.tc.service.JiraService;
import at.nice.tc.utils.JiraUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class ChatClientTestChecker {

    private final ChatModel chatModel;
    private final JiraService jiraService;
    private final GoogleTools googleTools;
    private final ConcurrentMap<Integer, ChatClientHolder> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> id$key = new ConcurrentHashMap<>();

    public String checkTestCase(Integer idTestCase) {
        ChatClientHolder holder = clients.computeIfAbsent(idTestCase, key -> createNewHolder());
        clearMemory(idTestCase);

        Map<String, Object> testAsMap = jiraService
                .getTest(String.valueOf(idTestCase))
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .join();

        String markdownTest = JiraUtils.toMarkdown(testAsMap);
        String requirements = googleTools.getTestCaseRequirements("ignore");
        String prompt = String.join("\n\n", requirements, markdownTest);

        return holder.chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
//                .doOnNext(c -> {
//                    System.out.println(c);
//                    System.out.flush();
//                })
                .reduce("", String::concat)
                .block();
    }

    public String askQuestionByKey(String key, String question) {
        Integer idTestCase = id$key.entrySet().stream()
                .filter(entry -> entry.getValue().equals(key))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        ChatClientHolder holder = clients.get(idTestCase);

        if (holder == null) {
            return "Ошибка: тест-кейс c id %s не был проверен ранее\n\n Проверенные:\n\n%s"
                    .formatted(idTestCase, JiraUtils.toString(id$key));
        }

        return holder.chatClient
                .prompt()
                .user(question)
                .stream()
                .content()
//                .doOnNext(c -> {
//                    System.out.println(c);
//                    System.out.flush();
//                })
                .reduce("", String::concat)
                .block();//todo Flux<String> для UI части с отдельными чатами по ТК
    }


    public void clearMemory(Integer idTestCase) {
        ChatClientHolder holder = clients.get(idTestCase);
        if (holder != null) {
            holder.chatMemory.clear("default");//todo м.б. позже conversationId
        }
    }

    private ChatClientHolder createNewHolder() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.1)
                        .topP(0.1)
                        .frequencyPenalty(0.01)
                        .presencePenalty(0.01)
                        .build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        return new ChatClientHolder(client, memory);
    }

    public void addIdKeyMapping(Integer idTestCase, String key) {
        id$key.put(idTestCase, key);
    }

    private record ChatClientHolder(ChatClient chatClient, ChatMemory chatMemory) {
    }
}
