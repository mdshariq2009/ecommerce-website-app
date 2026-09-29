class LiveChatWidget {
  constructor() {
    this.isOpen = false;
    this.conversationId = null;
    this.userEmail = '';
    this.userName = '';
    this.isConnected = false;
    this.messages = [];
    this.pollInterval = null;
    this.init();
  }

  init() {
    this.createWidgetHTML();
    this.attachEventListeners();
  }

  createWidgetHTML() {
    const widgetHTML = `
      <button id="liveChatButton" class="live-chat-button" title="Live Chat">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      </button>

      <div id="liveChatWindow" class="live-chat-window" style="display: none;">
        <div class="live-chat-header">
          <div>
            <h3>Live Chat Support</h3>
            <p class="live-chat-status">🔄 Connecting...</p>
          </div>
          <button id="closeChatButton" class="live-chat-close-btn" title="Close Chat">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <div id="messagesContainer" class="live-chat-messages">
          <div class="live-chat-welcome">
            <p>👋 Welcome! How can we help you today?</p>
          </div>
        </div>

        <form id="chatForm" class="live-chat-form" style="display: none;">
          <input 
            type="text" 
            id="messageInput" 
            class="live-chat-input" 
            placeholder="Type your message..." 
            disabled
          />
          <button type="submit" class="live-chat-send-btn" disabled title="Send Message">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </form>
      </div>
    `;

    document.body.insertAdjacentHTML('beforeend', widgetHTML);
    this.injectStyles();
  }

  injectStyles() {
    const styles = `
      .live-chat-button {
        position: fixed;
        bottom: 30px;
        right: 30px;
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        transition: all 0.3s ease;
        z-index: 999;
      }

      .live-chat-button:hover {
        transform: scale(1.1);
        box-shadow: 0 6px 16px rgba(102, 126, 234, 0.6);
      }

      .live-chat-button:active {
        transform: scale(0.95);
      }

      .live-chat-window {
        position: fixed;
        bottom: 100px;
        right: 30px;
        width: 380px;
        height: 600px;
        background: white;
        border-radius: 12px;
        box-shadow: 0 5px 40px rgba(0, 0, 0, 0.16);
        display: flex;
        flex-direction: column;
        z-index: 1000;
        animation: slideUp 0.3s ease-out;
      }

      @keyframes slideUp {
        from {
          opacity: 0;
          transform: translateY(20px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .live-chat-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 20px;
        border-radius: 12px 12px 0 0;
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
      }

      .live-chat-header h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
      }

      .live-chat-status {
        margin: 5px 0 0 0;
        font-size: 12px;
        opacity: 0.9;
      }

      .live-chat-close-btn {
        background: none;
        border: none;
        color: white;
        cursor: pointer;
        padding: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s ease;
      }

      .live-chat-close-btn:hover {
        transform: rotate(90deg);
      }

      .live-chat-messages {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
        background: #f8f9fa;
        display: flex;
        flex-direction: column;
        gap: 12px;
      }

      .live-chat-messages::-webkit-scrollbar {
        width: 6px;
      }

      .live-chat-messages::-webkit-scrollbar-track {
        background: #f1f1f1;
      }

      .live-chat-messages::-webkit-scrollbar-thumb {
        background: #888;
        border-radius: 3px;
      }

      .live-chat-messages::-webkit-scrollbar-thumb:hover {
        background: #555;
      }

      .live-chat-welcome {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
        text-align: center;
        color: #666;
        font-size: 14px;
      }

      .live-chat-message {
        display: flex;
        margin-bottom: 8px;
      }

      .live-chat-message.user {
        justify-content: flex-end;
      }

      .live-chat-message.admin {
        justify-content: flex-start;
      }

      .live-chat-bubble {
        max-width: 70%;
        padding: 12px 16px;
        border-radius: 12px;
        word-wrap: break-word;
      }

      .live-chat-message.user .live-chat-bubble {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-bottom-right-radius: 4px;
      }

      .live-chat-message.admin .live-chat-bubble {
        background: #e9ecef;
        color: #333;
        border-bottom-left-radius: 4px;
      }

      .live-chat-sender {
        margin: 0 0 4px 0;
        font-size: 12px;
        font-weight: 600;
        opacity: 0.8;
      }

      .live-chat-text {
        margin: 0;
        font-size: 14px;
        line-height: 1.4;
      }

      .live-chat-time {
        display: block;
        margin-top: 4px;
        font-size: 11px;
        opacity: 0.7;
      }

      .live-chat-form {
        display: flex;
        gap: 8px;
        padding: 15px;
        border-top: 1px solid #e9ecef;
        background: white;
        border-radius: 0 0 12px 12px;
      }

      .live-chat-input {
        flex: 1;
        padding: 10px 14px;
        border: 1px solid #ddd;
        border-radius: 20px;
        font-size: 14px;
        outline: none;
        transition: border-color 0.2s ease;
      }

      .live-chat-input:focus {
        border-color: #667eea;
      }

      .live-chat-input:disabled {
        background: #f5f5f5;
        cursor: not-allowed;
      }

      .live-chat-send-btn {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
      }

      .live-chat-send-btn:hover:not(:disabled) {
        transform: scale(1.05);
      }

      .live-chat-send-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      @media (max-width: 480px) {
        .live-chat-window {
          width: calc(100vw - 20px);
          height: 70vh;
          bottom: 80px;
          right: 10px;
          left: 10px;
        }

        .live-chat-bubble {
          max-width: 85%;
        }
      }
    `;

    const styleSheet = document.createElement('style');
    styleSheet.textContent = styles;
    document.head.appendChild(styleSheet);
  }

  attachEventListeners() {
    const chatButton = document.getElementById('liveChatButton');
    const closeButton = document.getElementById('closeChatButton');
    const chatForm = document.getElementById('chatForm');
    const messageInput = document.getElementById('messageInput');

    chatButton.addEventListener('click', () => this.toggleChat());
    closeButton.addEventListener('click', () => this.closeChat());
    chatForm.addEventListener('submit', (e) => this.handleSendMessage(e));
  }

  toggleChat() {
    if (!this.isOpen && !this.isConnected) {
      this.initializeChat();
    }
    this.isOpen = !this.isOpen;
    const chatWindow = document.getElementById('liveChatWindow');
    chatWindow.style.display = this.isOpen ? 'flex' : 'none';
  }

  async initializeChat() {
    let email = localStorage.getItem('userEmail');
    let name = localStorage.getItem('userName');

    if (!email) {
      email = prompt('Please enter your email:');
      if (!email) return;
      localStorage.setItem('userEmail', email);
    }

    if (!name) {
      name = prompt('Please enter your name:');
      if (!name) return;
      localStorage.setItem('userName', name);
    }

    this.userEmail = email;
    this.userName = name;

    try {
      const response = await fetch('/api/live-chat/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userEmail: email,
          userName: name,
          userId: localStorage.getItem('userId') || null
        })
      });

      const data = await response.json();
      this.conversationId = data.conversationId;
      this.isConnected = true;
      this.updateStatus('🟢 Connected');
      this.enableInput();
      await this.fetchMessages();
      this.startPolling();
    } catch (error) {
      console.error('Error initializing chat:', error);
      alert('Failed to start chat. Please try again.');
    }
  }

  async fetchMessages() {
    if (!this.conversationId) return;

    try {
      const response = await fetch(`/api/live-chat/messages/${this.conversationId}`);
      const messages = await response.json();
      this.displayMessages(messages);
    } catch (error) {
      console.error('Error fetching messages:', error);
    }
  }

  displayMessages(messages) {
    const container = document.getElementById('messagesContainer');
    
    if (messages.length === 0) {
      container.innerHTML = '<div class="live-chat-welcome"><p>👋 Welcome! How can we help you today?</p></div>';
      return;
    }

    container.innerHTML = messages.map(msg => `
      <div class="live-chat-message ${msg.isAdmin ? 'admin' : 'user'}">
        <div class="live-chat-bubble">
          <p class="live-chat-sender">${msg.isAdmin ? '🛟 Support Team' : 'You'}</p>
          <p class="live-chat-text">${msg.message}</p>
          <span class="live-chat-time">${new Date(msg.createdAt).toLocaleTimeString()}</span>
        </div>
      </div>
    `).join('');

    this.scrollToBottom();
  }

  async handleSendMessage(e) {
    e.preventDefault();

    const input = document.getElementById('messageInput');
    const message = input.value.trim();

    if (!message || !this.conversationId) return;

    input.value = '';

    try {
      await fetch('/api/live-chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId: this.conversationId,
          message: message,
          senderEmail: this.userEmail,
          senderName: this.userName,
          isAdmin: false
        })
      });

      setTimeout(() => this.fetchMessages(), 500);
    } catch (error) {
      console.error('Error sending message:', error);
    }
  }

  async closeChat() {
    if (this.conversationId) {
      try {
        await fetch('/api/live-chat/close', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ conversationId: this.conversationId })
        });
      } catch (error) {
        console.error('Error closing chat:', error);
      }
    }

    this.isOpen = false;
    this.conversationId = null;
    this.isConnected = false;
    this.messages = [];
    this.stopPolling();
    document.getElementById('liveChatWindow').style.display = 'none';
  }

  updateStatus(status) {
    const statusEl = document.querySelector('.live-chat-status');
    if (statusEl) {
      statusEl.textContent = status;
    }
  }

  enableInput() {
    const input = document.getElementById('messageInput');
    const sendBtn = document.querySelector('.live-chat-send-btn');
    const form = document.getElementById('chatForm');

    input.disabled = false;
    sendBtn.disabled = false;
    form.style.display = 'flex';
  }

  scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    container.scrollTop = container.scrollHeight;
  }

  startPolling() {
    if (this.pollInterval) clearInterval(this.pollInterval);
    this.pollInterval = setInterval(() => this.fetchMessages(), 2000);
  }

  stopPolling() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  new LiveChatWidget();
});
