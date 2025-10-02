/**
 * Управление Server-Sent Events (без Thymeleaf)
 */

TestSystemApp.prototype.initSSE = function() {
    this.sseUrl = `${this.apiBase}/sse`;
    this.sseReconnectAttempts = 0;
    this.sseMaxReconnectAttempts = 10;
    this.sseReconnectInterval = 5000;
    this.sseEventSource = null;

    this.connectSSE();
};

TestSystemApp.prototype.connectSSE = function() {
    if (this.sseEventSource) {
        this.sseEventSource.close();
    }

    this.updateConnectionStatus('connecting');

    try {
        this.sseEventSource = new EventSource(this.sseUrl);

        this.sseEventSource.onopen = () => {
            console.log('SSE подключение установлено (REST API)');
            this.sseReconnectAttempts = 0;
            this.state.sseConnected = true;
            this.updateConnectionStatus('connected');
        };

        this.sseEventSource.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                this.handleSSEEvent(data);
            } catch (error) {
                console.error('Ошибка парсинга SSE события:', error);
            }
        };

        this.sseEventSource.onerror = (error) => {
            console.error('SSE ошибка:', error);
            this.state.sseConnected = false;
            this.updateConnectionStatus('disconnected');

            if (this.sseReconnectAttempts < this.sseMaxReconnectAttempts) {
                this.scheduleSSEReconnect();
            }
        };

        // Обработчики специфических событий
        this.sseEventSource.addEventListener('chat_message', (event) => {
            const data = JSON.parse(event.data);
            this.handleChatMessage(data);
        });

        this.sseEventSource.addEventListener('status_update', (event) => {
            const data = JSON.parse(event.data);
            this.handleStatusUpdate(data);
        });

    } catch (error) {
        console.error('Ошибка создания SSE подключения:', error);
        this.updateConnectionStatus('failed');
        this.scheduleSSEReconnect();
    }
};

TestSystemApp.prototype.scheduleSSEReconnect = function() {
    this.sseReconnectAttempts++;
    this.updateConnectionStatus('connecting');

    setTimeout(() => {
        this.connectSSE();
    }, this.sseReconnectInterval);
};

TestSystemApp.prototype.updateConnectionStatus = function(status) {
    const statusIndicator = document.getElementById('status-indicator');
    const statusText = document.getElementById('status-text');

    if (!statusIndicator || !statusText) return;

    statusIndicator.classList.remove('connected', 'connecting', 'disconnected', 'failed');

    switch (status) {
        case 'connected':
            statusIndicator.classList.add('connected');
            statusText.textContent = 'REST API';
            break;
        case 'connecting':
            statusIndicator.classList.add('connecting');
            statusText.textContent = 'Подключение...';
            break;
        case 'disconnected':
            statusIndicator.classList.add('disconnected');
            statusText.textContent = 'Отключено';
            break;
    }
};

TestSystemApp.prototype.handleSSEEvent = function(data) {
    console.log('Получено SSE событие (REST API):', data);
};

TestSystemApp.prototype.handleChatMessage = function(data) {
    const { testId, checkId, message } = data;

    if (!this.state.chats[testId]) {
        this.state.chats[testId] = {};
    }
    if (!this.state.chats[testId][checkId]) {
        this.state.chats[testId][checkId] = [];
    }

    const messageObj = {
        type: message.type || 'system',
        content: message.content,
        timestamp: message.timestamp || new Date().toISOString()
    };

    this.state.chats[testId][checkId].push(messageObj);

    // Обновляем UI если это текущий чат
    if (this.state.currentTest === testId && this.state.currentCheck === checkId) {
        this.addMessageToChat(messageObj);
    }

    this.saveData();
};

TestSystemApp.prototype.handleStatusUpdate = function(data) {
    const { testId, checkId, status } = data;
    console.log(`Обновление статуса через REST API: ${testId}-${checkId} = ${status}`);

    // Обновляем статус в состоянии
    if (!this.state.statuses[testId]) {
        this.state.statuses[testId] = {};
    }
    this.state.statuses[testId][checkId] = status;

    this.saveData();
};
