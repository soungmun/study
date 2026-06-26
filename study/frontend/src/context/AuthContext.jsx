import React, { createContext, useState, useEffect, useContext } from 'react';
import { api } from '../utils/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const userData = await api.get('/auth/me');
                setUser(userData);
            } catch (error) {
                console.error('Failed to fetch user:', error);
                setUser(null);
            } finally {
                setLoading(false);
            }
        };
        fetchUser();
    }, []);

    const login = async (username, password) => {
        try {
            const userData = await api.post('/auth/login', { username, password });
            setUser(userData);
            return { success: true };
        } catch (error) {
            console.error('Login error:', error);
            return { success: false, message: error.message || '네트워크 오류' };
        }
    };

    const logout = async () => {
        try {
            await api.post('/auth/logout');
            setUser(null);
            return { success: true };
        } catch (error) {
            console.error('Logout error:', error);
            return { success: false, message: error.message || '로그아웃 실패' };
        }
    };

    const deleteAccount = async () => {
        try {
            await api.delete('/auth/me');
            setUser(null);
            return { success: true, message: '회원 탈퇴가 성공적으로 처리되었습니다.' };
        } catch (error) {
            console.error('Delete account error:', error);
            return { success: false, message: error.message || '회원 탈퇴 실패' };
        }
    };

    // 프로필 업데이트 기능 추가
    const updateProfile = async (profileData) => {
        try {
            const updatedUser = await api.put('/auth/me', profileData);
            setUser(updatedUser); // 업데이트된 사용자 정보로 상태 갱신
            return { success: true, user: updatedUser, message: '회원정보가 성공적으로 수정되었습니다.' };
        } catch (error) {
            console.error('Update profile error:', error);
            return { success: false, message: error.message || '회원정보 수정 실패' };
        }
    };

    return (
        <AuthContext.Provider value={{ user, setUser, loading, login, logout, deleteAccount, updateProfile, isAdmin: user?.role === 'ADMIN' }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    return useContext(AuthContext);
};
