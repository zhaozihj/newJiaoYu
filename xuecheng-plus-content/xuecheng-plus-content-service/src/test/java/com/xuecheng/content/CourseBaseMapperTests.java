package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CourseBaseMapperTests {

    @Autowired
    CourseBaseMapper courseBaseMapper;


@Test
    public void testCourseBaseMapper(){
    CourseBase courseBase = courseBaseMapper.selectById(18L);
    //看是否为空，如果为空则运行之后，下面控制台是红色的，如果不为空，运行之后下面控制台是绿色的
    Assertions.assertNotNull(courseBase);

    //详细进行分页查询的单元测试
    //查询条件
    QueryCourseParamsDto courseParamsDto=new QueryCourseParamsDto();
    courseParamsDto.setCourseName("java");

    //拼装查询条件
    LambdaQueryWrapper<CourseBase> queryWrapper=new LambdaQueryWrapper<>();
    //根据名称模糊查询,在sql当中拼接course_base.name like '%值%'
     queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()),CourseBase::getName,courseParamsDto.getCourseName());
    //根据课程的审核状态查询  course_base.audit_status=?
    queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,courseParamsDto.getAuditStatus());

    //分页参数对象
    PageParams pageParams=new PageParams();
    pageParams.setPageNo(1L);
    pageParams.setPageSize(2L);

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
    System.out.println(courseBasePageResult);

}
}
