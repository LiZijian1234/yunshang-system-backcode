package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * @author zijianLi 课程查询参数对应的类
 * @create 2023- 04- 28- 19:54
 */
@Data
@ToString
public class QueryCourseParamsDto {
    //审核状态
    private String auditStatus;
    //课程名称
    private String courseName;
    //发布状态
    private String publishStatus;

}
