package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zijianLi
 * @create 2023- 05- 07- 17:07
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;


    @Autowired
    ContentServiceClient contentServiceClient;
    /**
     * @description 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     * @return com.xuecheng.learning.model.dto.XcChooseCourseDto
     * @author Mr.M
     * @date 2022/10/24 17:33
     */
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //首先远程调用内容查询收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null){
            XueChengPlusException.cast("课程不存在");
        }
        //收费规则
        String charge = coursepublish.getCharge();

        XcChooseCourse xcChooseCourse = null;
        if ("201000".equals(charge)){
            //免费的话会向选课记录表，我的课程表插入
            //选课记录表，
            xcChooseCourse = addFreeCourse(userId, coursepublish);
            //我的课程表
            XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);
        }else{
            //收费的话会向选课记录表写数据
            xcChooseCourse = addChargeCourse(userId, coursepublish);
        }

        //判断学生的学习资格，即learnStatus
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);

        //构造返回值
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse, xcChooseCourseDto);
        xcChooseCourseDto.setLearnStatus(learningStatus.getLearnStatus());
        return xcChooseCourseDto;
    }

    /**
     * @description 判断学习资格
     * @param userId
     * @param courseId
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @author Mr.M
     * @date 2022/10/3 7:37
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //查询课程表
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if(xcCourseTables==null){
            //"code":"702002","desc":"没有选课或选课后没有支付
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            //没有选课或选课后没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        //code":"702003","desc":"已过期需要申请续期或重新支付"
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(!isExpires){
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;

        }else{
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }
    }


    /**
     * 保存选课成功的状态，在收到order服务发送的消息之后要干的事：
     * 1. 更新选课表的状态   2. 向我的课程表插入这个付费课程
     * @param chooseCourseId
     * @return
     */
    @Override
    @Transactional
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        //根据chooseCourseId选课id来查询一下课程数据表choose_course
        //根据choosecourseId查询选课记录xcChooseCourseMapper
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if(xcChooseCourse == null){
            log.debug("收到支付结果通知在choose_course表中没有查询到关联的选课记录,choosecourseId:{}",chooseCourseId);
            return false;
        }
        String status = xcChooseCourse.getStatus();
        if("701001".equals(status)){
            //添加到课程表
            addCourseTables(xcChooseCourse);
            return true;
        }
        //待支付状态701002才处理,更新选课记录choose_course的状态为支付成功
        if ("701002".equals(status)) {
            //更新为选课成功
            xcChooseCourse.setStatus("701001");
            int update = xcChooseCourseMapper.updateById(xcChooseCourse);
            if(update>0){
                log.debug("收到支付结果通知处理成功,选课记录:{}",xcChooseCourse);
                //添加到课程表
                XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);
                return true;
            }else{
                log.debug("收到支付结果通知处理失败,选课记录:{}",xcChooseCourse);
                XueChengPlusException.cast("添加选课记录失败");
                return false;
            }
        }

        return false;

    }

    //添加免费课程,免费课程加入选课记录表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        //判断，如果已经存在了免费的选课记录且选课状态为成功，就直接返回
        //查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            //直接返回第一个即可
            return xcChooseCourses.get(0);
        }


        //如果不存在免费的选课记录
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());//有效期开始时间
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));//有效期结束时间
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加选课记录失败，insert<=0");
        }
        return xcChooseCourse;

    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId,CoursePublish coursepublish){
        //判断，如果已经存在了收费的选课记录且选课状态为待支付，就直接返回
        //查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费课程
                .eq(XcChooseCourse::getStatus, "701002");//待支付成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            //直接返回第一个即可
            return xcChooseCourses.get(0);
        }

        //如果不存在免费的选课记录
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加选课记录失败，insert<=0");
        }
        return xcChooseCourse;

    }
    //添加到我的课程表
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse){
        //选课成功了才可以向我的课程表添加
        String status = xcChooseCourse.getStatus();
        if(!"701001".equals(status)){
            XueChengPlusException.cast("选课没有成功，所以无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            return xcCourseTables;
        }
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setUserId(xcChooseCourse.getUserId());
        xcCourseTablesNew.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTablesNew.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTablesNew.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTablesNew.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTablesNew.setUpdateDate(LocalDateTime.now());
        int insert = xcCourseTablesMapper.insert(xcCourseTablesNew);
        if (insert<=0){
            XueChengPlusException.cast("添加我的课程表失败");
        }

        return xcCourseTablesNew;
    }

    /**
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;
    }

    @Override
    /**
     * @description 查询我的课程表，在进入用户中心时调用这个方法
     * @param params
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.learning.model.po.XcCourseTables>
     * @author LiZijian
     * @date 2023/3/27 9:24
     */
    public PageResult<XcCourseTables> mycoursetables(MyCourseTableParams params) {
        //页码
        long pageNo = params.getPage();
        //每页记录数
        long pageSize = params.getSize();
        //分页条件
        Page<XcCourseTables> page = new Page<>(pageNo, pageSize);
        //根据用户id查询
        String userId = params.getUserId();
        LambdaQueryWrapper<XcCourseTables> lambdaQueryWrapper =
                new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId);

        //分页查询
        Page<XcCourseTables> pageResult = xcCourseTablesMapper.selectPage(page, lambdaQueryWrapper);
        //拿到数据列表
        List<XcCourseTables> records = pageResult.getRecords();
        //记录总数
        long total = pageResult.getTotal();
        PageResult<XcCourseTables> courseTablesResult = new PageResult<>(records, total, pageNo, pageSize);
        return courseTablesResult;
    }


}
