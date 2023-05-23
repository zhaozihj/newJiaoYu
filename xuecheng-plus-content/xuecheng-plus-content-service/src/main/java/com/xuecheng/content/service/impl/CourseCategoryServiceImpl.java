package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程分类树形结构查询
 *
 * @return
 */
@Service
@Slf4j
public class CourseCategoryServiceImpl implements CourseCategoryService {


    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtoList= courseCategoryMapper.selectTreeNodes(id);

        //找到每个节点的子结点，最终封装成List<CourseCategoryTreeDto>
        //先将List转为map,key就是map中元素的id，value就是CourseCategoryTreeDto对象，目的就是为了方便从map获取节点，filter(item -> !id.equals(item.getId()))是为了把根节点排除，controller方法参数id的值就是"1",(key1, key2) -> key2是为了解决key可能重复的问题，但是这里不会重复
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtoList.stream().filter(item -> !id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));

        //定义一个list作为最终返回的list
         List<CourseCategoryTreeDto> courseCategorys=new ArrayList<CourseCategoryTreeDto>();

         //从头遍历List<CourseCategoryTreeDto>，一边遍历一边把子结点放在父节点的childrenTreeNodes
        //filter(item->!id.equals(item.getId()))还是为了去除根节点
         courseCategoryTreeDtoList.stream().filter(item->!id.equals(item.getId())).forEach(item->{

             if(id.equals(item.getParentid())){
                 courseCategorys.add(item);
             }
             //找到节点的父节点
             CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
             if(courseCategoryParent!=null){
                 if(courseCategoryParent.getChildrenTreeNodes()==null){
                     //如果该父节点的childrenTreeNodes属性为空要new一个集合，因为要向该集合中放它的子节点
                     courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                 }

                 //把每个节点的子结点放在父节点的childrenTreeNodes属性中
                courseCategoryParent.getChildrenTreeNodes().add(item);
             }

         });

         return courseCategorys;
    }
}
