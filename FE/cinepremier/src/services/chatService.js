import { request } from './authService';

const CONVERSATION_ID_KEY_PREFIX = 'cinepremier_chat_conversation_id';

const keyFor = (scope) => `${CONVERSATION_ID_KEY_PREFIX}:${scope}`;

export const getStoredConversationId = (scope = 'default') => localStorage.getItem(keyFor(scope)) || null;

export const setStoredConversationId = (conversationId, scope = 'default') => {
  if (conversationId) localStorage.setItem(keyFor(scope), conversationId);
};

export const clearStoredConversationId = (scope = 'default') => localStorage.removeItem(keyFor(scope));

export const clearAllStoredConversationIds = () => {
  Object.keys(localStorage)
    .filter((key) => key.startsWith(CONVERSATION_ID_KEY_PREFIX))
    .forEach((key) => localStorage.removeItem(key));
};

// Ollama chạy local trên CPU có thể mất 15-60s+ mỗi lượt (cold start, tool-calling nhiều vòng),
// nên chat cần timeout dài hơn hẳn timeout mặc định 30s của axios cho các API khác.
const CHAT_TIMEOUT_MS = 120000;

export const chatService = {
  sendMessage: ({ message, movieId, userId, token, scope = 'default' }) => {
    const conversationId = getStoredConversationId(scope);
    return request('/api/v1/chat', {
      method: 'POST',
      body: { message, movieId, userId, conversationId },
      token,
      timeout: CHAT_TIMEOUT_MS
    }).then((res) => {
      if (res?.conversationId) setStoredConversationId(res.conversationId, scope);
      return res;
    });
  }
};
