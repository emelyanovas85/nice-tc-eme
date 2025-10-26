
const iframe = document.createElement('iframe');
iframe.src = 'http://localhost:8080/';
iframe.style.position = 'fixed';
iframe.style.right = '0';
iframe.style.top = '0';
iframe.style.width = '400px';
iframe.style.height = '600px';
iframe.style.zIndex = '9999';
iframe.style.border = '2px solid orange';
iframe.title = 'Vaadin ChatView';

document.body.appendChild(iframe);
