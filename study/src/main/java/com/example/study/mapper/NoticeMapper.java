package com.example.study.mapper;

import com.example.study.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NoticeMapper {

    List<Notice> findAll();

    Notice findById(Long id);

    void insert(Notice notice);

    void update(Notice notice);

    void deleteById(Long id);

    void incrementViewCount(Long id);
}