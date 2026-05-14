import { useEffect, useRef, useState } from 'react';

const API = 'http://localhost:8080/api/chat';

export default function Chat() {
  const [sessions, setSessions] = useState([]);
  const [activeId, setActiveId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const bottomRef = useRef(null);

  const loadSessions = async () => {
    const r = await fetch(`${API}/sessions`, { credentials: 'include' });
    if (!r.ok) return;
    const data = await r.json();
    setSessions(data);
    if (!activeId && data.length > 0) {
      setActiveId(data[0].id);
    }
  };

  const loadMessages = async (sid) => {
    if (!sid) {
      setMessages([]);
      return;
    }
    const r = await fetch(`${API}/sessions/${sid}/messages`, { credentials: 'include' });
    if (!r.ok) return;
    setMessages(await r.json());
  };

  useEffect(() => {
    loadSessions();
  }, []);

  useEffect(() => {
    loadMessages(activeId);
  }, [activeId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const onNewSession = async () => {
    const r = await fetch(`${API}/sessions`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    if (!r.ok) return;
    const s = await r.json();
    setSessions((prev) => [s, ...prev]);
    setActiveId(s.id);
    setMessages([]);
  };

  const onDeleteSession = async (sid) => {
    if (!window.confirm('이 대화를 삭제할까요?')) return;
    await fetch(`${API}/sessions/${sid}`, { method: 'DELETE', credentials: 'include' });
    setSessions((prev) => prev.filter((s) => s.id !== sid));
    if (activeId === sid) {
      setActiveId(null);
      setMessages([]);
    }
  };

  const onSend = async (e) => {
    e.preventDefault();
    const content = input.trim();
    if (!content || sending) return;
    setError(null);

    let sid = activeId;
    if (!sid) {
      const r = await fetch(`${API}/sessions`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      });
      if (!r.ok) {
        setError('세션 생성 실패');
        return;
      }
      const s = await r.json();
      setSessions((prev) => [s, ...prev]);
      sid = s.id;
      setActiveId(sid);
    }

    const optimisticUser = {
      id: `tmp-${Date.now()}`,
      role: 'user',
      content,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, optimisticUser]);
    setInput('');
    setSending(true);

    try {
      const r = await fetch(`${API}/sessions/${sid}/messages`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content }),
      });
      if (!r.ok) {
        const data = await r.json().catch(() => ({}));
        throw new Error(data.message || '응답 실패');
      }
      const assistantMsg = await r.json();
      await loadMessages(sid);
      loadSessions();
    } catch (err) {
      setError(err.message);
      setMessages((prev) => prev.filter((m) => m.id !== optimisticUser.id));
    } finally {
      setSending(false);
    }
  };

  const formatTokens = (m) =>
    m.outputTokens != null ? ` · ${m.inputTokens ?? 0} in / ${m.outputTokens} out` : '';

  return (
    <div className="chat-wrap">
      <aside className="chat-sidebar">
        <button type="button" className="chat-new-btn" onClick={onNewSession}>
          + 새 대화
        </button>
        <div className="chat-session-list">
          {sessions.length === 0 && (
            <div className="chat-empty muted">대화가 없어요. 새 대화를 시작해 보세요.</div>
          )}
          {sessions.map((s) => (
            <div
              key={s.id}
              className={`chat-session-item ${s.id === activeId ? 'active' : ''}`}
              onClick={() => setActiveId(s.id)}
            >
              <span className="chat-session-title">{s.title}</span>
              <button
                type="button"
                className="chat-session-del"
                onClick={(e) => {
                  e.stopPropagation();
                  onDeleteSession(s.id);
                }}
                aria-label="삭제"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      </aside>

      <main className="chat-main">
        <div className="chat-messages">
          {messages.length === 0 && (
            <div className="chat-empty muted">
              메시지를 입력하면 Claude가 대답해요.
            </div>
          )}
          {messages.map((m) => (
            <div key={m.id} className={`chat-bubble chat-bubble-${m.role}`}>
              <div className="chat-bubble-role">{m.role === 'user' ? '나' : 'Claude'}</div>
              <div className="chat-bubble-content">{m.content}</div>
              {m.role === 'assistant' && (
                <div className="chat-bubble-meta muted">{formatTokens(m)}</div>
              )}
            </div>
          ))}
          {sending && (
            <div className="chat-bubble chat-bubble-assistant">
              <div className="chat-bubble-role">Claude</div>
              <div className="chat-bubble-content"><span className="chat-typing">생각 중…</span></div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {error && <div className="error chat-error">⚠️ {error}</div>}

        <form className="chat-input-form" onSubmit={onSend}>
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                onSend(e);
              }
            }}
            placeholder="메시지를 입력하세요 (Enter 전송, Shift+Enter 줄바꿈)"
            rows={3}
            disabled={sending}
            maxLength={10000}
          />
          <button type="submit" className="primary" disabled={sending || !input.trim()}>
            {sending ? '전송 중…' : '전송'}
          </button>
        </form>
      </main>
    </div>
  );
}
