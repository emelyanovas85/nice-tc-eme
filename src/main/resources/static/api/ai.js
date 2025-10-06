class AI {
    static API_BASE_URL = '/api/ai';

    static async startBatch(testId) {
        const res = await fetch(`${this.API_BASE_URL}/batch/${encodeURIComponent(testId)}`);
        if (!res.ok) throw new Error('batch failed of test ' + testId);
        return res.json(); // [{id, text}]
    }

    static async getChecks() {
        const res = await fetch(`${this.API_BASE_URL}/checks);
        if (!res.ok) throw new Error('checks failed of test ' + testId);
        return res.json(); // [{id, description, prompt}]
    }

    static async chat(payload) {
        const res = await fetch(`${this.API_BASE_URL}/chat`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('chat failed ');
        return res.json(); // {response, ...} или потоковое SSE — здесь простой ответ
    }
}