package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jws.Oneway;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;


    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id判断是新增和修改
        //没有id的是新增，有id的是修改
        Long teachplanId = saveTeachplanDto.getId();
        if(teachplanId==null){
            //新增
            Teachplan teachplan=new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);

            //确定排序字段(orderby字段)，找到它的同级节点的个数，排序字段就是个数加1  select count(1) from teachplan where course_id=117 and parentid=268
            //orderby字段就是用来决定同级的章节的排列顺序，后面加进来的章节orderby会越来越大
            Long courseId = saveTeachplanDto.getCourseId();
            Long parentid = saveTeachplanDto.getParentid();

            int teachplanCount = getTeachplanCount(courseId, parentid);
            teachplan.setOrderby(teachplanCount);
            teachplanMapper.insert(teachplan);
        }
        else
        {
            //修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //将参数复制到teachplan
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);

        }

    }

    //获取同级的章节的个数
    private int getTeachplanCount(Long courseId,Long parentId){

        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count+1;
    }
}
