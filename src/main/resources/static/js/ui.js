/**
 * Управление пользовательским интерфейсом (без Thymeleaf)
 */

TestSystemApp.prototype.handleTestInput = function(event) {
    const value = event.target.value.trim().toUpperCase();

    if (value.length >= 4) {
        this.loadTestData(value);
    }
};

TestSystemApp.prototype.handleTestInputKeydown = function(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        const value = event.target.value.trim();
        if (value) {
            this.loadTestData(value);
        }
    }
};

TestSystemApp.prototype.handlePromptKeydown = function(event) {
    if (event.ctrlKey && event.key === 'Enter') {
        event.preventDefault();
        this.sendPromptAsMessage();
    }
};

TestSystemApp.prototype.handleChatKeydown = function(event) {
    if (event.ctrlKey && event.key === 'Enter') {
        event.preventDefault();
        this.sendMessage();
    }
};

TestSystemApp.prototype.sendMessage = function() {
    const chatInput = document.getElementById('chat-input');
    const message = chatInput.value.trim();

    if (!message) return;

    chatInput.value = '';
    this.sendChatMessage('user', message);
};

TestSystemApp.prototype.sendChatMessage = function(type, content) {
    const testId = this.state.currentTest;
    const checkId = this.state.currentCheck;

    if (!testId || !checkId) return;

    const message = {
        type: type,
        content: content,
        timestamp: new Date().toISOString()
    };

    // Добавляем сообщение в чат
    if (!this.state.chats[testId]) {
        this.state.chats[testId] = {};
    }
    if (!this.state.chats[testId][checkId]) {
        this.state.chats[testId][checkId] = [];
    }

    this.state.chats[testId][checkId].push(message);

    // Отправляем на сервер через REST API
    this.sendToAI(testId, checkId, content);

    // Обновляем UI
    this.addMessageToChat(message);
    this.setProcessing(testId, checkId, true);

    this.saveData();
};

TestSystemApp.prototype.sendToAI = async function(testId, checkId, message) {
    try {
        const response = await fetch(`${this.apiBase}/ai/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                testId: testId,
                checkId: checkId,
                message: message,
                placeholders: this.state.placeholders[testId] || {}
            })
        });

        if (!response.ok) {
            throw new Error('Ошибка API');
        }

        console.log('Сообщение отправлено в AI');
    } catch (error) {
        console.error('Ошибка отправки сообщения:', error);
        this.setProcessing(testId, checkId, false);
    }
};

TestSystemApp.prototype.addMessageToChat = function(message) {
    const chatContainer = document.getElementById('chat-messages');

    // Удаляем пустое сообщение если есть
    const emptyMessage = chatContainer.querySelector('.empty-chat-message');
    if (emptyMessage) {
        emptyMessage.remove();
    }

    const messageElement = this.createMessageElement(message);
    chatContainer.appendChild(messageElement);

    // Прокрутка к последнему сообщению
    chatContainer.scrollTop = chatContainer.scrollHeight;
};

TestSystemApp.prototype.createMessageElement = function(message) {
    const element = document.createElement('div');
    element.className = `message ${message.type}`;

    const contentElement = document.createElement('div');
    contentElement.textContent = message.content;

    const timeElement = document.createElement('div');
    timeElement.className = 'message-time';
    timeElement.textContent = new Date(message.timestamp).toLocaleTimeString();

    element.appendChild(contentElement);
    element.appendChild(timeElement);

    return element;
};

TestSystemApp.prototype.handlePromptChange = function(event) {
    this.saveData();
};

TestSystemApp.prototype.sendPromptAsMessage = function() {
    const promptInput = document.getElementById('prompt-input');
    const prompt = promptInput.value.trim();

    if (!prompt) return;

    this.sendChatMessage('user', prompt);
};

// Управление секциями
window.toggleSection = function(sectionId) {
    const section = document.getElementById(sectionId);
    const header = section.querySelector('.section-header');

    header.classList.toggle('collapsed');
};
