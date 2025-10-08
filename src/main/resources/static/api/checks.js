class Checks {
    static API_BASE_URL = '/api/checks';

    static async getAllChecks() {
        const res = await fetch(`${this.API_BASE_URL}/`);
        if (!res.ok) throw new Error('getAllChecks failed');
        return res.json(); // [{id, name, description, prompt, type (test,run)}]
    }

    static async updateCheck(id, newPrompt) {
        const res = await fetch(`${this.API_BASE_URL}/${id}/update`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: newPrompt
        });
        if (!res.ok) throw new Error('failed to update check ' + id);
        return res.json(); // {response, ...} или потоковое SSE — здесь простой ответ
    }
}