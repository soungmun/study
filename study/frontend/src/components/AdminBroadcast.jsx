import { useEffect, useState } from 'react';

const API = 'http://localhost:8080/api/admin';

export default function AdminBroadcast() {
  const [status, setStatus] = useState('loading'); // 'loading' | 'forbidden' | 'ready'
  const [subscribers, setSubscribers] = useState(0);
  const [form, setForm] = useState({ subject: '', body: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    fetch(`${API}/me`, { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { admin: false, subscribers: 0 }))
      .then((data) => {
        setSubscribers(data.subscribers || 0);
        setStatus(data.admin ? 'ready' : 'forbidden');
      })
      .catch(() => setStatus('forbidden'));
  }, []);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!form.subject.trim() || !form.body.trim()) {
      setError('제목과 본문을 모두 입력해 주세요.');
      return;
    }
    if (subscribers === 0) {
      setError('수신 동의한 사용자가 없습니다.');
      return;
    }
    if (!window.confirm(`수신 동의한 ${subscribers}명에게 공지 메일을 발송할까요?`)) return;

    setSubmitting(true);
    try {
      const r = await fetch(`${API}/broadcast`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ subject: form.subject.trim(), body: form.body }),
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.message || '발송에 실패했습니다.');
      setSuccess(`${data.recipients}명에게 발송 요청을 등록했어요. 메일은 백그라운드에서 순차 발송됩니다.`);
      setForm({ subject: '', body: '' });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (status === 'loading') {
    return (
      <div className="card">
        <div className="book-empty">
          <div className="spinner" />
          <p>확인 중…</p>
        </div>
      </div>
    );
  }

  if (status === 'forbidden') {
    return (
      <div className="card">
        <div className="toolbar">
          <h2>📣 공지 메일 발송</h2>
          <span className="muted">관리자 전용</span>
        </div>
        <div className="user-locked">
          <div className="user-locked-icon">🔒</div>
          <div className="user-locked-title">관리자만 사용할 수 있어요</div>
          <div className="user-locked-desc">
            관리자 계정으로 로그인한 뒤 다시 시도해 주세요.
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="toolbar">
        <h2>📣 공지 메일 발송</h2>
        <span className="muted">수신 동의자 <strong>{subscribers}</strong>명</span>
      </div>

      <form className="profile-edit-form" onSubmit={onSubmit}>
        <div className="profile-edit-row">
          <label>제목</label>
          <input
            type="text"
            value={form.subject}
            onChange={(e) => setForm({ ...form, subject: e.target.value })}
            placeholder="공지 메일 제목"
            maxLength={200}
            required
          />
        </div>

        <div className="profile-edit-row">
          <label>본문</label>
          <textarea
            value={form.body}
            onChange={(e) => setForm({ ...form, body: e.target.value })}
            placeholder="본문 내용. 줄바꿈은 그대로 적용됩니다."
            rows={10}
            maxLength={10000}
            required
            style={{ resize: 'vertical', fontFamily: 'inherit', padding: '10px 12px',
                     border: '1px solid #cbd5e1', borderRadius: '10px', width: '100%' }}
          />
        </div>

        {error && <div className="error">⚠️ {error}</div>}
        {success && <div className="profile-edit-success">✅ {success}</div>}

        <div className="profile-edit-actions">
          <button type="submit" className="primary" disabled={submitting || subscribers === 0}>
            {submitting ? '발송 요청 중…' : `${subscribers}명에게 발송`}
          </button>
        </div>
      </form>
    </div>
  );
}