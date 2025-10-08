
const Status = Object.freeze({
    Pending: 'pending',
    Success: 'success',
    Error: 'error',
    Warning: 'warning'
});

class Store {
    constructor() {
        this.fields = [];            // [{id, name}]
        this.checks = [];            // [{id, description, prompt}]
        this.tests = new Map();      // id -> TestEntity
        this.prompts = new Map();    // key(testId,checkId) -> string
        this.chats = new Map();      // key -> [{at, from, text}]
        this.processing = { tests: new Set(), checks: new Set() }; // keys
    }

    key(testId, checkId) { return `${testId}__${checkId}`; }

    setPrompt(testId, checkId, text) {
        this.prompts.set(this.key(testId, checkId), text || '');
    }

    getPrompt(testId, checkId) {
        return this.prompts.get(this.key(testId, checkId)) || '';
    }

    appendChat(testId, checkId, msg) {
        const k = this.key(testId, checkId);
        if (!this.chats.has(k)) this.chats.set(k, []);
        this.chats.get(k).push(msg);
    }

    getChat(testId, checkId) {
        return this.chats.get(this.key(testId, checkId)) || [];
    }

    startTestProcessing(testId) { this.processing.tests.add(String(testId)); }
    endTestProcessing(testId) { this.processing.tests.delete(String(testId)); }
    isTestProcessing(testId) { return this.processing.tests.has(String(testId)); }

    startCheckProcessing(testId, checkId) { this.processing.checks.add(this.key(testId, checkId)); }
    endCheckProcessing(testId, checkId) { this.processing.checks.delete(this.key(testId, checkId)); }
    isCheckProcessing(testId, checkId) { return this.processing.checks.has(this.key(testId, checkId)); }
}

class TestEntity {
    constructor({ id, testKey, version }, store) {
        this.id = String(id);
        this.version = String(version);
        this.testKey = testKey;
        this.store = store;
        this.fields = [];         // [{id,name,value}]
        this.checkResults = new Map(); // checkId -> {answerText, status}
        this.status = Status.Pending;
        this.active = false;      // активен для клика или заблокирован
    }

    setFieldsWithValues(fields, values) {
        this.fields = fields.map((f, i) => ({ id: String(f.id), name: f.name, value: values[i] }));
    }

    setBatchResults(results) {
        // results: [{id, text}] — определяем статус проверки
        results.forEach(r => {
            const status = this.computeCheckStatus(r.text);
            this.checkResults.set(String(r.id), { answerText: r.text, status });
        });
        this.status = this.computeTestStatus();
        this.active = true;
    }

    static computeCheckStatus(text) {
        const t = (text || '').toLowerCase();
        if (t.includes('не соответствует')) return Status.Error;
        if (t === 'соответствует') return Status.Success;
        return Status.Warning;
    }

    computeTestStatus() {
        const statuses = Array.from(this.checkResults.values()).map(v => v.status);
        if (statuses.length === 0) return Status.Pending;
        if (statuses.some(s => s === Status.Warning)) return Status.Warning;
        if (statuses.some(s => s === Status.Error)) return Status.Error;
        if (statuses.every(s => s === Status.Success)) return Status.Success;
        return Status.Warning;
    }
}

class UI {
    constructor(store) {
        this.store = store;
        this.elements = {
            tabs: document.getElementById('tabs-container'),
            checks: document.getElementById('checks-container'),
            content: document.getElementById('content-area'),
            details: document.getElementById('details-section'),
            promptInput: document.getElementById('prompt-input'),
            fieldsList: document.getElementById('fields-list'),
            jsonContent: document.getElementById('json-content'),
            chatMessages: document.getElementById('chat-messages'),
            sendBtn: document.getElementById('send-btn'),
            stopBtn: document.getElementById('stop-btn')
        };
        this.currentTestId = null;
        this.currentCheckId = null;

        this.wirePromptDnD();
        this.wireChatControls();
    }

    renderSidebar(tests) {
        const c = this.elements.tabs;
        c.innerHTML = '';
        tests.forEach(test => {
            const btn = document.createElement('button');
            btn.className = 'tab';
            if (String(this.currentTestId) === String(test.id)) btn.classList.add('active');

            const left = document.createElement('div');
            left.className = 'tab-left';

            const st = document.createElement('span');
            st.className = 'tab-status';
            st.innerHTML = this.statusIcon(test.status);

            const title = document.createElement('span');
            title.textContent = `${test.testKey} (${test.version}.0)`;

            const spin = document.createElement('span');
            spin.className = 'tab-spinner';
            if (this.store.isTestProcessing(test.id)) spin.innerHTML = '<div class="spinner"></div>';

            left.appendChild(st);
            left.appendChild(title);
            btn.appendChild(left);
            btn.appendChild(spin);

            btn.disabled = !test.active;
            btn.addEventListener('click', () => this.selectTest(test.id));

            c.appendChild(btn);
        });
    }

    renderChecks(checks, test) {
        const c = this.elements.checks;
        c.innerHTML = '';
        checks.forEach(ch => {
            const tile = document.createElement('div');
            tile.className = 'check-tile';
            if (String(this.currentCheckId) === String(ch.id)) tile.classList.add('active');
            tile.addEventListener('click', () => this.selectCheck(test.id, ch.id));

            const status = document.createElement('span');
            status.className = 'check-tile-status';
            const result = test.checkResults.get(String(ch.id));
            status.innerHTML = this.statusIcon(result?.status || Status.Pending);

            const spinner = document.createElement('span');
            spinner.className = 'check-tile-spinner';
            if (this.store.isCheckProcessing(test.id, ch.id)) spinner.innerHTML = '<div class="spinner"></div>';

            tile.appendChild(document.createTextNode(ch.description || ch.name || ch.id));
            tile.appendChild(status);
            tile.appendChild(spinner);

            c.appendChild(tile);
        });
        this.elements.content.classList.remove('hidden');
        this.elements.details.classList.add('hidden');
    }

    renderPrompt(testId, checkId) {
        const v = this.store.getPrompt(testId, checkId);
        this.elements.promptInput.value = v || '';
    }

    renderFieldsList(fields, onClick) {
        const c = this.elements.fieldsList;
        c.innerHTML = '';
        fields.forEach(f => {
            const el = document.createElement('span');
            el.className = 'field-item';
            el.textContent = f.name;
            el.draggable = true;
            el.addEventListener('click', () => {
                this.elements.jsonContent.textContent = JSON.stringify(f.value, null, 2);
                c.querySelectorAll('.field-item').forEach(x => x.classList.remove('selected'));
                el.classList.add('selected');
                if (onClick) onClick(f);
            });
            el.addEventListener('dragstart', (e) => {
                e.dataTransfer.setData('text/plain', f.name);
                el.classList.add('dragging');
            });
            el.addEventListener('dragend', () => el.classList.remove('dragging'));
            c.appendChild(el);
        });
    }

    renderChat(testId, checkId) {
        const msgs = this.store.getChat(testId, checkId);
        const c = this.elements.chatMessages;
        c.innerHTML = '';
        msgs.forEach(m => {
            const d = document.createElement('div');
            d.className = `chat-message ${m.from === 'user' ? 'user' : 'system'}`;
            const isMatch = (m.text || '').toLowerCase().includes('соответствует');
            if (isMatch) d.className += ' match';
            d.textContent = m.text;
            c.appendChild(d);
        });
        c.scrollTop = c.scrollHeight;
    }

    selectTest(testId) {
        this.currentTestId = testId;
        this.currentCheckId = null;
        app.onTestSelected(testId);
    }

    selectCheck(testId, checkId) {
        this.currentTestId = testId;
        this.currentCheckId = checkId;
        app.onCheckSelected(testId, checkId);
        this.elements.details.classList.remove('hidden');
    }

    setButtonsProcessing(isProcessing) {
        const { sendBtn, stopBtn } = this.elements;
        if (!sendBtn || !stopBtn) return;
        if (isProcessing) {
            sendBtn.classList.add('hidden');
            stopBtn.classList.remove('hidden');
            stopBtn.disabled = false;
        } else {
            sendBtn.classList.remove('hidden');
            stopBtn.classList.add('hidden');
            sendBtn.disabled = false;
        }
    }

    statusIcon(status) {
        switch (status) {
            case Status.Success: return '<span class="status-success">✓</span>';
            case Status.Error: return '<span class="status-error">✗</span>';
            case Status.Pending: return '<span class="status-processing">●</span>';
            default: return '<span class="status-warning">●</span>';
        }
    }

    wirePromptDnD() {
        const el = this.elements.promptInput;
        el.addEventListener('dragover', (e) => { e.preventDefault(); el.style.backgroundColor = '#f0f8ff'; });
        el.addEventListener('dragleave', () => { el.style.backgroundColor = ''; });
        el.addEventListener('drop', (e) => {
            e.preventDefault();
            el.style.backgroundColor = '';
            const placeholder = e.dataTransfer.getData('text/plain');
            const pos = el.selectionStart;
            el.value = el.value.slice(0, pos) + placeholder + el.value.slice(pos);
            el.selectionStart = el.selectionEnd = pos + placeholder.length;
            el.focus();
            if (this.currentTestId && this.currentCheckId) {
                this.store.setPrompt(this.currentTestId, this.currentCheckId, el.value);
            }
        });
        el.addEventListener('input', () => {
            if (this.currentTestId && this.currentCheckId) {
                this.store.setPrompt(this.currentTestId, this.currentCheckId, el.value);
            }
        });
    }

    wireChatControls() {
        const { sendBtn, stopBtn } = this.elements;
        if (sendBtn) sendBtn.addEventListener('click', () => app.sendChat());
        if (stopBtn) stopBtn.addEventListener('click', () => app.stopChat());
        const chatInput = document.getElementById('chat-input');
        chatInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && e.ctrlKey) {
                e.preventDefault();
                app.sendChat();
            }
        });
    }

    updateAll(testList) {
        this.renderSidebar(testList);
        if (this.currentTestId) {
            const t = app.getTest(this.currentTestId);
            if (t) {
                this.renderChecks(app.store.checks, t);
                this.renderFieldsList(t.fields);
                if (this.currentCheckId) {
                    this.renderPrompt(this.currentTestId, this.currentCheckId);
                    this.renderChat(this.currentTestId, this.currentCheckId);
                }
            }
        }
    }
}

class App {
    constructor() {
        this.store = new Store();
        this.ui = new UI(this.store);
        this.abortControllers = new Map(); // key -> AbortController
    }

    get tests() { return Array.from(this.store.tests.values()); }
    getTest(id) { return this.store.tests.get(String(id)); }

    async init() {
        // 1) Получить список тестов из URL
        const testsParam = new URLSearchParams(window.location.search).get('tests');
        const tests = JSON.parse(testsParam || '[]'); // [{ id, testKey, version }]
        tests.forEach(t => this.store.tests.set(String(t.id), new TestEntity(t, this.store)));

        // 2) Загрузить общий список полей
        this.store.fields = await Jira.getFields();

        // 3) Для каждого теста: получить значения полей
        for (const test of this.tests) {
            this.store.startTestProcessing(test.id);
            this.ui.updateAll(this.tests);
            test.active = false;
            try {
                const fieldIds = this.store.fields.map(f => String(f.id));
                const values = await Jira.getTestFieldValues(test.id, fieldIds);
                test.setFieldsWithValues(this.store.fields, values);
            } finally {
                this.store.endTestProcessing(test.id);
                this.ui.updateAll(this.tests);
            }
        }

        // 4) Запустить первичные проверки (batch) для каждого теста
        for (const test of this.tests) {
            this.store.startTestProcessing(test.id);
            test.status = Status.Pending;
            this.ui.updateAll(this.tests);
            try {
                const results = await AI.startBatch(test.id); // [{id,text}]
                test.setBatchResults(results);
                // Сохранить в чат начальные сообщения для каждой проверки
                results.forEach(r => {
                    // первичный промпт — возьмём из общего списка проверок позже; сейчас только ответ
                    this.store.appendChat(test.id, r.id, { at: new Date().toISOString(), from: 'assistant', text: r.text });
                });
            } finally {
                this.store.endTestProcessing(test.id);
                this.ui.updateAll(this.tests);
            }
        }

        // 5) Если не загружены общие проверки — загрузить
        if (!this.store.checks.length) {
            this.store.checks = await AI.getChecks(); // [{id,description,prompt}]
        }

        // 6) Отрисовать левый сайдбар
        this.ui.updateAll(this.tests);
    }

    onTestSelected(testId) {
        const t = this.getTest(testId);
        if (!t) return;
        this.ui.renderChecks(this.store.checks, t);
        this.ui.renderFieldsList(t.fields);
    }

    onCheckSelected(testId, checkId) {
        // В промпт подставить общий шаблон проверки (если есть) + сохранить
        const base = this.store.checks.find(c => String(c.id) === String(checkId));
        const current = this.store.getPrompt(testId, checkId) || (base?.prompt || '');
        this.store.setPrompt(testId, checkId, current);
        this.ui.renderPrompt(testId, checkId);

        // Сформировать стартовый чат: первичный запрос и ответ из batch, если ещё не были
        const chat = this.store.getChat(testId, checkId);
        if (chat.length === 0) {
            if (current) this.store.appendChat(testId, checkId, { at: new Date().toISOString(), from: 'user', text: current });
            const t = this.getTest(testId);
            const res = t?.checkResults.get(String(checkId));
            if (res?.answerText) {
                this.store.appendChat(testId, checkId, { at: new Date().toISOString(), from: 'assistant', text: res.answerText });
            }
        }
        this.ui.renderChat(testId, checkId);
    }

    async sendChat() {
        const testId = this.ui.currentTestId;
        const checkId = this.ui.currentCheckId;
        if (!testId || !checkId) return;

        const test = this.getTest(testId);
        const check = this.store.checks.find(c => String(c.id) === String(checkId));
        const prompt = this.store.getPrompt(testId, checkId);
        const chatInput = document.getElementById('chat-input');
        const text = (chatInput.value || '').trim();
        if (!text) return;

        const key = this.store.key(testId, checkId);
        if (this.abortControllers.has(key)) return; // уже обрабатывается

        const controller = new AbortController();
        this.abortControllers.set(key, controller);

        // Включаем спиннеры
        this.store.startTestProcessing(testId);
        this.store.startCheckProcessing(testId, checkId);
        this.ui.setButtonsProcessing(true);

        // Добавляем сообщение пользователя
        this.store.appendChat(testId, checkId, { at: new Date().toISOString(), from: 'user', text });
        this.ui.renderChat(testId, checkId);
        chatInput.value = '';

        // Собираем полный список сообщений
        const messages = [
            { at: new Date().toISOString(), from: 'user', text: prompt || '' }
        ].concat(this.store.getChat(testId, checkId));

        try {
            const payload = {
                test: test.id,
                check: String(checkId),
                messages
            };
            const data = await AI.chat(payload, { signal: controller.signal });
            const response = data.response ?? (typeof data === 'string' ? data : JSON.stringify(data));
            this.store.appendChat(testId, checkId, { at: new Date().toISOString(), from: 'assistant', text: response });

            // Обновим статус проверки и теста
            const status = TestEntity.computeCheckStatus(response);
            const t = this.getTest(testId);
            t.checkResults.set(String(checkId), { answerText: response, status });
            t.status = t.computeTestStatus();

        } catch (e) {
            this.store.appendChat(testId, checkId, { at: new Date().toISOString(), from: 'system', text: 'Ошибка при получении ответа.' });
        } finally {
            this.abortControllers.delete(key);
            this.store.endCheckProcessing(testId, checkId);
            this.store.endTestProcessing(testId);
            this.ui.setButtonsProcessing(false);
            this.ui.updateAll(this.tests);
        }
    }

    stopChat() {
        const testId = this.ui.currentTestId;
        const checkId = this.ui.currentCheckId;
        if (!testId || !checkId) return;
        const key = this.store.key(testId, checkId);
        const c = this.abortControllers.get(key);
        if (c) c.abort();
    }
}

// Bootstrap
window.app = new App();
document.addEventListener('DOMContentLoaded', () => {
    window.app.init().catch(console.error);
});