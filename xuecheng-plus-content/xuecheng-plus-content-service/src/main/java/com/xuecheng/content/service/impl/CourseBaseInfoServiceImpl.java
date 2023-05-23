package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
public class CourseBaseInfoServiceImpl  implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto) {

        //拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper=new LambdaQueryWrapper<>();
        //根据名称模糊查询,在sql当中拼接course_base.name like '%值%'
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()),CourseBase::getName,courseParamsDto.getCourseName());
        //根据课程的审核状态查询  course_base.audit_status=?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,courseParamsDto.getAuditStatus());

        //根据课程的发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()),CourseBase::getStatus,courseParamsDto.getPublishStatus());

        //创建page分页参数对象,参数，当前页码，每页记录数
        Page<CourseBase> page=new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        //开始进行分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //数据列表
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();


        //这个PageResult类是我们用来返回分页查询的数据的，是自己定义的
        //List<T> items, long counts, long page, long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<CourseBase>(items,total,pageParams.getPageNo(),pageParams.getPageSize());
        return courseBasePageResult;
    }

    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
//        //合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
//            XueChengPlusException.cast("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            //throw new RuntimeException("课程分类为空");
//            XueChengPlusException.cast("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            //throw new RuntimeException("课程分类为空");
//            XueChengPlusException.cast("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            //throw new RuntimeException("课程等级为空");
//            XueChengPlusException.cast("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            //throw new RuntimeException("教育模式为空");
//            XueChengPlusException.cast("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            //throw new RuntimeException("适应人群为空");
//            XueChengPlusException.cast("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            //throw new RuntimeException("收费规则为空");
//            XueChengPlusException.cast("收费规则为空");
//        }


        //向课程基本信息表course_base写入数据
        CourseBase courseBase=new CourseBase();
        //将传入的页面参数放到courseBase对象
        BeanUtils.copyProperties(dto,courseBase);//只要属性名称一致就可以拷贝，如果dto中有属性为null，那么courseBase中的那个相同属性值也会变null
        //这些内容都是拷贝后添加，如果拷贝之前添加，这些属性拷贝之后会变成空
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        //审核状态默认为未提交
        courseBase.setAuditStatus("202002");
        //发布状态为未发布
        courseBase.setStatus("203001");

        //插入数据库
        int insert=courseBaseMapper.insert(courseBase);
        //insert<=0则说明插入数据库失败
        if(insert<=0){
            throw new RuntimeException("添加课程失败");
        }

        //向课程营销信息表course_market写入数据
         CourseMarket courseMarketNew =new CourseMarket();
        //将页面输入的数据拷贝到courseMarketNew
        BeanUtils.copyProperties(dto,courseMarketNew);
        //课程的id
        Long courseId = courseBase.getId();
        courseMarketNew.setId(courseId);
        //保存营销信息
        saveCourseMarket(courseMarketNew);
        //从数据库查询课程的详细信息,包括两部分
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;


    }

    //查询课程完整信息，在createCourseBase,updateCourseBase方法中用到
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){
        //从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase==null){
            return null;
        }

        //从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //组装在一起
        CourseBaseInfoDto courseBaseInfoDto=new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);

        if(courseMarket!=null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        //解决courseBaseInfoDto中大分类名称mtName，小分类名称stName 的赋值
        CourseCategory courseCategoryMt = courseCategoryMapper.selectById(courseBaseInfoDto.getMt());
        String mtName = courseCategoryMt.getName();
        CourseCategory courseCategorySt = courseCategoryMapper.selectById(courseBaseInfoDto.getSt());
        String stName = courseCategorySt.getName();

        courseBaseInfoDto.setMtName(mtName);
        courseBaseInfoDto.setStName(stName);
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {

        //拿到课程id
        Long courseId = editCourseDto.getId();
        //查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }

        //数据合法性校验
        //根据具体的业务逻辑去校验
        //本机构只能修改本机构的课程
        if(!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }

        //封装数据
        BeanUtils.copyProperties(editCourseDto,courseBase);
        //修改时间
        courseBase.setChangeDate(LocalDateTime.now());

        //更新数据库
        int i=courseBaseMapper.updateById(courseBase);
        if(i<=0){
            XueChengPlusException.cast("修改课程失败");
        }

        //更新营销信息
        //查找原本的营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //将传递参数拷贝给courseMarket
        //如果courseMarket不为空，也就是原本数据库中有营销信息
        if(courseMarket!=null) {
            BeanUtils.copyProperties(editCourseDto, courseMarket);
        }
        //如果courseMarket为空，也就是原本数据库中没有营销信息
        else
        {
            courseMarket=new CourseMarket();
            BeanUtils.copyProperties(editCourseDto,courseMarket);
        }
        //保存营销信息
        int i1 = saveCourseMarket(courseMarket);
        if(i1<0){
            XueChengPlusException.cast("修改课程失败");
        }

        //查询课程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;


    }

    //单独写一个方法保存营销信息，逻辑：存在则更新，不存在则添加   在createCourseBase,updateCourseBase方法中用到
    private int saveCourseMarket(CourseMarket courseMarketNew){
        //参数合法性校验
        String charge=courseMarketNew.getCharge();
        if(StringUtils.isEmpty(charge)){
            //throw new RuntimeException("收费规则为空");
            XueChengPlusException.cast("收费规则为空");
        }
        //如果课程收费，价格没有填写也需要抛出异常
        if(charge.equals("201001")){
            if(courseMarketNew.getPrice()==null||courseMarketNew.getPrice().floatValue()<=0){
               // throw new RuntimeException("课程的价格不能为空并且必须大于0");
               XueChengPlusException.cast("课程的价格不能为空并且必须大于0");
            }
        }
        //从数据库查询营销信息，存在则更新，不存在则添加
        Long id=courseMarketNew.getId();
        CourseMarket courseMarket=courseMarketMapper.selectById(id);
        if(courseMarket==null){
            //插入数据库
            int insert=courseMarketMapper.insert(courseMarketNew);
            return insert;
        }
        else
        {
            //将courseMarketNew拷贝到courseMarket
            BeanUtils.copyProperties(courseMarketNew,courseMarket);
            //防止courseMarket的id值变为null
            courseMarket.setId(courseMarketNew.getId());
            //更新
            int i=courseMarketMapper.updateById(courseMarket);
            return i;
        }


    }


}
