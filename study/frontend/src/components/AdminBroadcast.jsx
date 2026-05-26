import { useEffect, useState } from 'react';

const API = 'http://localhost:8080/api/admin';

export default function AdminBroadcast() {
  const [status, setStatus] = useState('loading'); // 'loading' | 'forbidden' | 'ready'

  // 점검모드 상태
  const [maintenance, setMaintenance] = useState({ enabled: false, lastEnabledBy: null, lastEnabledAt: null, lastDisabledAt: null });
  const [maintToggling, setMaintToggling] = useState(false);
  const [maintMsg, setMaintMsg] = useState(null);
  const [maintErr, setMaintErr] = useState(null);

  const loadMaintenance = () => {
    fetch(`${API}/maintenance/status`, { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => { if (data) setMaintenance(data); })
      .catch(() => {});
  };

  useEffect(() => {
    fetch(`${API}/me`, { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { admin: false }))
      .then((data) => setStatus(data.admin ? 'ready' : 'forbidden'))
      .catch(() => setStatus('forbidden'));
    loadMaintenance();
  }, []);

  const onToggleMaintenance = async () => {
    setMaintErr(null);
    setMaintMsg(null);
    const turningOn = !maintenance.enabled;
    const confirmMsg = turningOn
      ? '점검 모드를 켜시겠어요?\n- 관리자 외 모든 사용자가 API를 사용할 수 없게 됩니다.\n- 전체 가입 회원에게 점검 안내 메일이 1회 발송됩니다.'
      : '점검 모드를 해제하시겠어요?\n- 사용자가 다시 서비스를 이용할 수 있게 됩니다.\n- 전체 가입 회원에게 점검 완료 안내 메일이 1회 발송됩니다.';
    if (!window.confirm(confirmMsg)) return;
    setMaintToggling(true);
    try {
      const r = await fetch(`${API}/maintenance/${turningOn ? 'enable' : 'disable'}`, {
        method: 'POST',
        credentials: 'include',
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.message || '요청 실패');
      if (turningOn) {
        setMaintMsg(data.alreadyOn
          ? '이미 점검 모드가 켜져 있습니다.'
          : `점검 모드를 켰어요. ${data.recipientsQueued || 0}명에게 안내 메일을 발송했습니다.`);
      } else {
        setMaintMsg(data.alreadyOff
          ? '이미 점검 모드가 꺼져 있습니다.'
          : `점검 모드를 해제했어요. ${data.recipientsQueued || 0}명에게 완료 안내 메일을 발송했습니다.`);
      }
      loadMaintenance();
    } catch (err) {
      setMaintErr(err.message);
    } finally {
      setMaintToggling(false);
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
          <h2>🛠️ 서버 점검 모드</h2>
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

  const fmtTime = (iso) => {
    if (!iso) return '-';
    try { return new Date(iso).toLocaleString('ko-KR'); } catch { return iso; }
  };

  return (
    <div className="card">
      <div className="toolbar">
        <h2>🛠️ 서버 점검 모드</h2>
        <span
          className="muted"
          style={{
            padding: '4px 10px', borderRadius: 999, fontWeight: 700,
            background: maintenance.enabled ? '#fef3c7' : '#dcfce7',
            color: maintenance.enabled ? '#b45309' : '#166534',
          }}
        >
          {maintenance.enabled ? '점검 중' : '정상 운영'}
        </span>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 12 }}>
        <div style={{ fontSize: 13, color: '#475569' }}>
          점검 모드를 켜면 <strong>관리자 외 모든 사용자</strong>의 API 호출이 차단되고,
          전체 가입 회원에게 <strong>점검 안내 메일</strong>이 1회 발송됩니다.
          해제 시에는 <strong>점검 완료 안내 메일</strong>이 1회 발송됩니다.
        </div>
        <div style={{ fontSize: 12, color: '#94a3b8' }}>
          마지막 ON: {fmtTime(maintenance.lastEnabledAt)}{maintenance.lastEnabledBy ? ` (by ${maintenance.lastEnabledBy})` : ''} ·
          마지막 OFF: {fmtTime(maintenance.lastDisabledAt)}
        </div>
        {maintMsg && <div className="profile-edit-success">✅ {maintMsg}</div>}
        {maintErr && <div className="error">⚠️ {maintErr}</div>}
        <div>
          <button
            type="button"
            onClick={onToggleMaintenance}
            disabled={maintToggling}
            style={{
              padding: '10px 16px', fontSize: 14, fontWeight: 700,
              borderRadius: 10, border: 'none', cursor: 'pointer',
              background: maintenance.enabled ? '#16a34a' : '#f59e0b',
              color: '#fff',
            }}
          >
            {maintToggling
              ? '처리 중…'
              : (maintenance.enabled ? '점검 모드 해제 + 완료 메일 발송' : '점검 모드 시작 + 전체 메일 발송')}
          </button>
        </div>
      </div>
    </div>
  );
}
