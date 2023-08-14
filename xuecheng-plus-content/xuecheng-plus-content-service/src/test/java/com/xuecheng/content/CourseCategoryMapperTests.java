package com.xuecheng.content;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author zijianLi
 * @create 2023- 04- 29- 12:00
 */
@SpringBootTest
class CourseCategoryMapperTests {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Test
    void testCourseCategoryMapper() {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos =
                courseCategoryMapper.selectTreeNodes("1");
        System.out.println(courseCategoryTreeDtos);



    }


}
