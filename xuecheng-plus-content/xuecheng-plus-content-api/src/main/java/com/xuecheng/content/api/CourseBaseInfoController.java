package com.xuecheng.content.api;

import com.sun.xml.internal.ws.api.model.wsdl.editable.EditableWSDLBoundFault;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//这个Api是swagger的注释，说明这个类是专门写课程信息管理接口
@Api(value="课程信息管理接口",tags="课程信息管理接口")
@RestController//相当于@Controller和@reponseBody
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;


    //这个ApiOperation是swagger的注释，说明这个接口是专门用来课程查询的
    @ApiOperation("课程分页查询接口")
    @PostMapping("/course/list")
    //pageParams存储分页信息，queryCourseParamsDto存储有关课程信息，@RequestBody是把json参数转换为对象
    //required为false就是这个参数可有可无，正常pageParams这种本身就可有可无
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){

        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);

        return courseBasePageResult;

    }

    @ApiOperation("新增课程")
    @PostMapping("/course")
    //@Validated注解用来激活AddCourseDto类中的JSR303校验的注解
    //@Validated(ValidationGroups.Inster.class)  这样就是只有ValidationGroups.Inster.class分组中的标记注解才能起作用，这时没有在任何分组中的也不起作用
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated(ValidationGroups.Inster.class) AddCourseDto addCourseDto){
        //获取到用户所属机构的id
        Long companyId=1232141425L;
        //int i=1/0;
        CourseBaseInfoDto courseBase=courseBaseInfoService.createCourseBase(companyId,addCourseDto);
        return courseBase;
    }

    @ApiOperation("根据课程id查询课程信息接口")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId){
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @ApiOperation("修改课程")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto){

        //获取到用户所属机构的id
        Long companyId=1232141425L;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.updateCourseBase(companyId, editCourseDto);
        return courseBaseInfoDto;
    }


}
