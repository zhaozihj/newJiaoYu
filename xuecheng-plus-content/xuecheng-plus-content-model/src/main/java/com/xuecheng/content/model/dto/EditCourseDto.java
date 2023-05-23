package com.xuecheng.content.model.dto;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


//修改课程的时候比新增课程的时候接受的参数多了一个courseId
@Data
public class EditCourseDto extends AddCourseDto {

    @ApiModelProperty(value="课程id",required = true)
    private Long id;
}
