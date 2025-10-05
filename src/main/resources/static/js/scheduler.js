// import Jira from ./api/jira.js


// при загрузке страницы
setInterval(() => {
    Jira.getStatus().then(status => {
        if (status !== 'connected')
            showBanner(status);
    });
}, 10000);