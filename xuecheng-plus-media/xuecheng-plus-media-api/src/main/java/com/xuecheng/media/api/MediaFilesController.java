package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @description 媒资文件管理接口
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
 @Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
 @RestController
public class MediaFilesController {


  @Autowired
  MediaFileService mediaFileService;


 @ApiOperation("媒资列表查询接口")
 @PostMapping("/files")
 public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
  Long companyId = 1232141425L;
  return mediaFileService.queryMediaFiels(companyId,pageParams,queryMediaParamsDto);

 }

    @ApiOperation("上传图片")
    //consumes属性指定http请求的MIME类型，值是数组类型，支持多个MIME类型，可以使用MediaType来指定MIME类型。
    @RequestMapping(value="/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //@RequestPart("filedata")  这个filedata是与前端约定好的
    //1.@RequestPart这个注解用在multipart/form-data表单提交请求的方法上。就是接受文件上传的这个参数的时候可以用@RequestPart
    //@RequestPart这个注解的value值和文件上传的文件的name属性要相同
    //springboot这里可以把上传的文件自动封装为MultipartFile类型的，如果是springmvc还需要自己进行一些配置
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile filedata) throws IOException {

        Long companyId = 1232141425L;
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        //文件大小
        uploadFileParamsDto.setFileSize(filedata.getSize());
        //图片
        uploadFileParamsDto.setFileType("001001");
        //文件名称
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());//文件名称
        //文件大小
        long fileSize = filedata.getSize();
        uploadFileParamsDto.setFileSize(fileSize);
        //创建临时文件,这个临时文件前缀和后缀应该都是随便起的
        File tempFile = File.createTempFile("minio", "temp");
        //上传的文件拷贝到临时文件
        filedata.transferTo(tempFile);
        //文件路径，通过创建的临时文件来获取文件存储路径，因为用filedata获取不到
        //这个路径获得的就是临时文件的路径，但是上传的文件已经拷贝到临时文件了
        String absolutePath = tempFile.getAbsolutePath();
        //上传文件
        UploadFileResultDto uploadFileResultDto = mediaFileService.uploadFile(companyId, uploadFileParamsDto, absolutePath);

        return uploadFileResultDto;

    }



}
