import { useEffect, useState } from 'react';
import NoticeList from './components/NoticeList';
import NoticeForm from './components/NoticeForm';
import NoticeDetail from './components/NoticeDetail';
import './App.css';

const BASE_URL = 'http://localhost:8080/api/notices';

async function api(method, path = '', body) {
  const res = await fetch(BASE_URL + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error((await res.text()) || `HTTP ${res.status}`);
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export default function App() {
  const [view, setView] = useState('list');
  const [notices, setNotices] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState(null);

  const refresh = async () => {
    setNotices(await api('GET'));
    setSelected(null);
    setView('list');
  };

  const run = async (label, fn) => {
    try {
      await fn();
      setError(null);
    } catch (e) {
      setError(e.message);
      alert(`${label} 실패: ${e.message}`);
    }
  };

  useEffect(() => { run('목록', refresh); }, []);

  const open = (id) => run('조회', async () => {
    setSelected(await api('GET', `/${id}`));
    setView('detail');
  });

  const create = (data) => run('등록', async () => {
    await api('POST', '', data);
    alert('등록되었습니다.');
    await refresh();
  });

  const update = (data) => run('수정', async () => {
    await api('PUT', `/${selected.id}`, data);
    alert('수정되었습니다.');
    await refresh();
  });

  const remove = () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    run('삭제', async () => {
      await api('DELETE', `/${selected.id}`);
      alert('삭제되었습니다.');
      await refresh();
    });
  };

  return (
    <div className="container">
      <header className="page-header">
        <div>
          <h1>게시판</h1>
          <div className="subtitle">함께 나누는 공지와 이야기</div>
        </div>
      </header>
      {error && <div className="error">에러: {error}</div>}
      {view === 'list' && (
        <NoticeList notices={notices} onSelect={open} onNew={() => setView('create')} />
      )}
      {view === 'detail' && selected && (
        <NoticeDetail
          notice={selected}
          onBack={() => setView('list')}
          onEdit={() => setView('edit')}
          onDelete={remove}
        />
      )}
      {view === 'create' && (
        <NoticeForm onSubmit={create} onCancel={() => setView('list')} />
      )}
      {view === 'edit' && selected && (
        <NoticeForm initial={selected} onSubmit={update} onCancel={() => setView('list')} />
      )}
    </div>
  );
}