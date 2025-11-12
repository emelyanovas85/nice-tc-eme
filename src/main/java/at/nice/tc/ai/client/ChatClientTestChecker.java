package at.nice.tc.ai.client;

import at.nice.tc.service.JiraService;
import at.nice.tc.utils.JiraUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class ChatClientTestChecker {

    private final ChatModel chatModel;
    private final JiraService jiraService;
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

        //todo нормальный промпт для агрегатора и, собственно, нужен сам агрегатор как отдельный чат клиент
        String prompt = String.join("\n\n", Prompt.get(), markdownTest);
        return holder.chatClient.prompt()
                .user(prompt)
                .call()
                .content();
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
                .mutate()
                .defaultOptions(ChatOptions.builder()//todo тут нужно подумать стоит ли менять настройки на мягкие
                        .temperature(0.7)
                        .topP(0.8)
                        .build())
                .build()
                .prompt()
                .user(question)
                .call()
                .content();//todo Flux<String> для UI части с отдельными чатами по ТК
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
//            .defaultTools(jiraTools, googleTools)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.3)
                        .topP(0.3)
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
