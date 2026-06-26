import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom'; // React Router v6

const DeleteAccountButton = () => {
    const { deleteAccount } = useAuth();
    const navigate = useNavigate();

    const handleDeleteAccount = async () => {
        if (window.confirm('정말로 회원 탈퇴를 하시겠습니까? 탈퇴 시 모든 정보가 삭제되며 복구할 수 없습니다.')) {
            const result = await deleteAccount();
            if (result.success) {
                alert(result.message);
                navigate('/'); // 탈퇴 성공 시 메인 페이지로 이동
            } else {
                alert(result.message);
            }
        }
    };

    return (
        <button
            onClick={handleDeleteAccount}
            style={{
                backgroundColor: '#dc3545', // 빨간색
                color: 'white',
                padding: '10px 15px',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer',
                fontSize: '16px',
                marginTop: '20px'
            }}
        >
            회원 탈퇴
        </button>
    );
};

export default DeleteAccountButton;
