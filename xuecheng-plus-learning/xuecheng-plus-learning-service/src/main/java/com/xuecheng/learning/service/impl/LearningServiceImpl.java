package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author zijianLi
 * @create 2023- 05- 12- 23:35
 */
@Service
@Slf4j
public class LearningServiceImpl implements LearningService {

    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Autowired
    XcCourseTablesMapper xccourseTablesMapper;

    @Override
    /**
     * @description 获取教学视频
     * @param courseId 课程id
     * @param teachplanId 课程计划id
     * @param mediaId 视频文件id
     * @return com.xuecheng.base.model.RestResponse<java.lang.String>
     * @author LiZijian
     * @date 2023/03/05 9:08
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {

        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish==null){
            return RestResponse.validfail("课程不存在");
        }

        //远程调用内容管理服务，根据课程计划表的id（teachplanId）查询课程计划的信息，如果里面的is_preview是1的话表示这个小节可以试学
        //coursepublish从这个对象中解析出teachplanId
        //todo 如果支持试学，就直接调用媒资服务，远程查询试学的课程的url

        if (StringUtils.isNotEmpty(userId)){
            //获取学习资格
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            //学习资格，[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            String learnStatus = learningStatus.getLearnStatus();
            if ("702002".equals(learnStatus)){
                return RestResponse.validfail("无法学习，因为有选课或选课后没有支付");
            }else if ("702003".equals(learnStatus)){
                return RestResponse.validfail("无法学习，因为已过期需要申请续期或重新支付");
            }else {
                //说明有资格学习
                //远程调用媒资服务获取视频的播放地址
                RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrlByMediaId;
            }
        }

        //此时用户没有登录
        //此时要查询课程信息,看收费规则
        String charge = coursepublish.getCharge();
        if ("201000".equals(charge)){
            //有资格学习免费的，
            //远程调用媒资服务获取视频的播放地址
            RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrlByMediaId;

        }

        return RestResponse.validfail("该课程需要购买才能观看");
    }


}
