package at.nice.tc.dao.ai.client;

public class Main {
    public static void main(String[] args) {
        ChatApiClient chatApiClient = ChatApiClient.qwen32B("https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru", null);
        chatApiClient.healthCheck();
    }
}
