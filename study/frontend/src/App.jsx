import { useEffect, useState } from 'react';
import NoticeList from './components/NoticeList';
import NoticeForm from './components/NoticeForm';
import NoticeDetail from './components/NoticeDetail';
import {
  fetchNotices,
  fetchNotice,
  createNotice,
  updateNotice,
  deleteNotice,
} from './api/notice';
import './App.css';

const VIEW = {
  LIST: 'list',
  DETAIL: 'detail',
  CREATE: 'create',
  EDIT: 'edit',
};

export default function App() {
  const [view, setView] = useState(VIEW.LIST);
  const [notices, setNotices] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState(null);

  const loadList = async () => {
    try {
      const data = await fetchNotices();
      setNotices(data);
      setError(null);
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    loadList();
  }, []);

  const openDetail = async (id) => {
    try {
      const data = await fetchNotice(id);
      setSelected(data);
      setView(VIEW.DETAIL);
    } catch (e) {
      setError(e.message);
    }
  };

  const handleCreate = async (data) => {
    await createNotice(data);
    await loadList();
    setView(VIEW.LIST);
  };

  const handleUpdate = async (data) => {
    const updated = await updateNotice(selected.id, data);
    setSelected(updated);
    await loadList();
    setView(VIEW.DETAIL);
  };

  const handleDelete = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    await deleteNotice(selected.id);
    setSelected(null);
    await loadList();
    setView(VIEW.LIST);
  };

  return (
    <div className="container">
      <h1>게시판</h1>
      {error && <div className="error">에러: {error}</div>}
      {view === VIEW.LIST && (
        <NoticeList
          notices={notices}
          onSelect={openDetail}
          onNew={() => setView(VIEW.CREATE)}
        />
      )}
      {view === VIEW.DETAIL && selected && (
        <NoticeDetail
          notice={selected}
          onBack={() => setView(VIEW.LIST)}
          onEdit={() => setView(VIEW.EDIT)}
          onDelete={handleDelete}
        />
      )}
      {view === VIEW.CREATE && (
        <NoticeForm
          onSubmit={handleCreate}
          onCancel={() => setView(VIEW.LIST)}
        />
      )}
      {view === VIEW.EDIT && selected && (
        <NoticeForm
          initial={selected}
          onSubmit={handleUpdate}
          onCancel={() => setView(VIEW.DETAIL)}
        />
      )}
    </div>
  );
}