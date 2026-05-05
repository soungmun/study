import { Route, Routes } from 'react-router-dom';
import NoticeList from './components/NoticeList';
import NoticeDetail from './components/NoticeDetail';
import NoticeCreate from './components/NoticeCreate';
import NoticeEdit from './components/NoticeEdit';
import './App.css';

export default function App() {
  return (
    <div className="container">
      <header className="page-header">
        <div>
          <h1>게시판</h1>
          <div className="subtitle">함께 나누는 공지와 이야기</div>
        </div>
      </header>
      <Routes>
        <Route path="/" element={<NoticeList />} />
        <Route path="/notices/new" element={<NoticeCreate />} />
        <Route path="/notices/:id" element={<NoticeDetail />} />
        <Route path="/notices/:id/edit" element={<NoticeEdit />} />
      </Routes>
    </div>
  );
}