package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
@Slf4j
 @Service
public class MediaFileServiceImpl implements MediaFileService {

  @Autowired
 MediaFilesMapper mediaFilesMapper;

  //用来进行文件处理的类，MinioConfig中把它加入到spring容器中
  @Autowired
 MinioClient minioClient;

  @Autowired
 MediaProcessMapper mediaProcessMapper;

  @Autowired
  MediaFileService currentProxy;

  //通过这个注解得到nacos中的配置文件中的内容
 //获取存储普通文件的桶名
  @Value("${minio.bucket.files}")
  private String bucket_mediafiles;

  //获取存储视频的桶名
  @Value("${minio.bucket.videofiles}")
  private String bucket_video;

 @Override
 public MediaFiles getFileById(String mediaId) {

  MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
  return mediaFiles;
 }

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
  
  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }


 //根据扩展名获取mimeType，参数extension是扩展名
 private String getMimeType(String extension){
  //如果扩展名extension为空，为了防止空指针异常，给它一个""
  if(extension==null){
   extension="";
  }

  //通过扩展名得到媒体资源类型  媒体资源类型也就是mimeType
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
  //这个mimeType是默认的mimeType，就是不知道文件类型的时候的mimeType
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流

  //能通过扩展名拿到mimeType
  if(extensionMatch!=null){
   //通过扩展名拿到mimeType
   mimeType=extensionMatch.getMimeType();
  }
  return mimeType;

 }

 /**
  * 将文件上传到minio
  * @param localFilePath 文件本地路径
  * @param mimeType 媒体类型
  * @param bucket 桶
  * @param objectName 对象名
  * @return
  */
 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket,String objectName){

                  try{
                   UploadObjectArgs uploadObjectArgs=UploadObjectArgs.builder()
                           .bucket(bucket)//桶
                           .filename(localFilePath) //指定本地文件
                           .object(objectName) //对象名 放在子目录下
                           .contentType(mimeType) ///设置媒体文件类型
                           .build();
                   //上传文件
                   minioClient.uploadObject(uploadObjectArgs);
                   log.debug("上传文件到minio成功，bucket:{},objectName:{}",bucket,objectName);
                   return true;
                  }
                  catch (Exception e){
                   e.printStackTrace();
                   log.error("上传文件出错，bucket:{},objectName:{},错误信息:{}",bucket,objectName,e.getMessage());
                  }
                  return false;

 }

 //获取文件默认存储目录路径 年/月/日
 //就是保存到minio的时候，不能直接在bucket的根目录下存储，要按照年月日分级存储，所以要采用这样的形式
 //这个方法得到的结果就是上传文件的时候，.object参数的一部分
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/")+"/";
  return folder;
 }

 //获取文件的md5
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }


 /**
  * @description 将文件信息添加到文件表
  * @param companyId  机构id
  * @param fileMd5  文件md5值
  * @param uploadFileParamsDto  上传文件的信息
  * @param bucket  桶
  * @param objectName 对象名称
  * @return com.xuecheng.media.model.po.MediaFiles
  * @author Mr.M
  * @date 2022/10/12 21:22
  */
 @Transactional
 public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
  //从数据库查询文件,看是否之前就上传过这个文件，每个文件的id都不同，所以通过id来查询
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles == null) {
   mediaFiles = new MediaFiles();
   //拷贝基本信息
   BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
   mediaFiles.setId(fileMd5);
   mediaFiles.setFileId(fileMd5);
   mediaFiles.setCompanyId(companyId);
   mediaFiles.setUrl("/" + bucket + "/" + objectName);
   mediaFiles.setBucket(bucket);
   mediaFiles.setFilePath(objectName);
   mediaFiles.setCreateDate(LocalDateTime.now());
   mediaFiles.setAuditStatus("002003");
   mediaFiles.setStatus("1");
   //保存文件信息到文件表
   int insert = mediaFilesMapper.insert(mediaFiles);
   if (insert < 0) {
    log.error("保存文件信息到数据库失败,{}",mediaFiles.toString());
    XueChengPlusException.cast("保存文件信息失败");
   }
   log.debug("保存文件信息到数据库成功,{}",mediaFiles.toString());

   addWaitingTask(mediaFiles);


  }
  return mediaFiles;

 }

 private void addWaitingTask(MediaFiles mediaFiles){
  //文件名称
  String filename=mediaFiles.getFilename();
  //文件扩展名
  String extension=filename.substring(filename.lastIndexOf("."));
  //获取文件的mimeType
  String mimeType=getMimeType(extension);
  if(mimeType.equals("video/x-msvideo")){//如果是avi视频写入待处理任务
   //创建一个MediaProcess类的对象，记录未处理文件的信息
   MediaProcess mediaProcess = new MediaProcess();
   BeanUtils.copyProperties(mediaFiles,mediaProcess);
   //状态是未处理
   mediaProcess.setStatus("1");
   mediaProcess.setCreateDate(LocalDateTime.now());
   mediaProcess.setFailCount(0);//失败次数是0
   mediaProcess.setUrl(null);
   //把待处理文件的信息插入到MediaProcess类对应的表中
   mediaProcessMapper.insert(mediaProcess);

  }
 }

 //合并分块
 @Override
 public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
  //=====获取分块文件路径=====
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  //组成将分块文件路径组成 List<ComposeSource>
  List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
          .limit(chunkTotal)
          .map(i -> ComposeSource.builder()
                  .bucket(bucket_video)
                  .object(chunkFileFolderPath.concat(Integer.toString(i)))
                  .build())
          .collect(Collectors.toList());
  //=====合并=====
  //文件名称
  String fileName = uploadFileParamsDto.getFilename();
  //文件扩展名
  String extName = fileName.substring(fileName.lastIndexOf("."));
  //合并文件路径
  String mergeFilePath = getFilePathByMd5(fileMd5, extName);
  try {
   //合并文件
   ObjectWriteResponse response = minioClient.composeObject(
           ComposeObjectArgs.builder()
                   .bucket(bucket_video)
                   //文件合并之后在minio上的存储路径以及文件名
                   .object(mergeFilePath)
                   .sources(sourceObjectList)
                   .build());
   log.debug("合并文件成功:{}",mergeFilePath);
  } catch (Exception e) {
   log.debug("合并文件失败,fileMd5:{},异常:{}",fileMd5,e.getMessage(),e);
   return RestResponse.validfail(false, "合并文件失败。");
  }

  // ====验证md5====
  //下载合并后的文件
  //要把minio上合并后的文件下载下来(然后获得本地的合并后文件流)才能和本地源文件的文件流进行比较，不能用minio的远程文件流和本地源文件流进行比较
  File minioFile = downloadFileFromMinIO(bucket_video,mergeFilePath);
  if(minioFile == null){
   log.debug("下载合并后文件失败,mergeFilePath:{}",mergeFilePath);
   return RestResponse.validfail(false, "下载合并后文件失败。");
  }

  //在try的括号里面定义流，流到时候会自动关闭，不需要加finally来关闭流
  try (InputStream newFileInputStream = new FileInputStream(minioFile)) {
   //minio上文件的md5值
   String md5Hex = DigestUtils.md5Hex(newFileInputStream);
   //比较md5值，不一致则说明文件不完整
   if(!fileMd5.equals(md5Hex)){
    return RestResponse.validfail(false, "文件合并校验失败，最终上传失败。");
   }
   //文件大小
   uploadFileParamsDto.setFileSize(minioFile.length());
  }catch (Exception e){
   log.debug("校验文件失败,fileMd5:{},异常:{}",fileMd5,e.getMessage(),e);
   return RestResponse.validfail(false, "文件合并校验失败，最终上传失败。");
  }finally {
   if(minioFile!=null){
    minioFile.delete();
   }
  }

  //文件入库
  currentProxy.addMediaFilesToDb(companyId,fileMd5,uploadFileParamsDto,bucket_video,mergeFilePath);
  //=====清除分块文件=====
  clearChunkFiles(chunkFileFolderPath,chunkTotal);



  return RestResponse.success(true);
 }

 /**
  * 从minio下载文件
  * @param bucket 桶
  * @param objectName 对象名称
  * @return 下载后的文件
  */
 public File downloadFileFromMinIO(String bucket,String objectName){
  //临时文件
  File minioFile = null;
  FileOutputStream outputStream = null;
  try{
   InputStream stream = minioClient.getObject(GetObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .build());
   //创建临时文件
   minioFile=File.createTempFile("minio", ".merge");
   outputStream = new FileOutputStream(minioFile);
   //把stream远程文件流拷贝到本地的临时文件流
   IOUtils.copy(stream,outputStream);
   return minioFile;
  } catch (Exception e) {
   e.printStackTrace();
  }finally {
   if(outputStream!=null){
    try {
     outputStream.close();
    } catch (IOException e) {
     e.printStackTrace();
    }
   }
  }
  return null;
 }
 /**
  * 得到合并后的文件的地址，这都是自己规定好的
  * @param fileMd5 文件id即md5值
  * @param fileExt 文件扩展名
  * @return
  */
 private String getFilePathByMd5(String fileMd5,String fileExt){
  return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
 }

 /**
  * 清除分块文件
  * @param chunkFileFolderPath 分块文件路径
  * @param chunkTotal 分块文件总数
  */
 private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){

  try {
   //把要删除的分块文件封装为 DeleteObject，然后组成一个list  ，然后把list放在RemoveObjectsArgs.objects这个方法的参数中
   List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
           .limit(chunkTotal)
           //chunkFileFolderPath.concat(Integer.toString(i))  这个是分块文件的路径
           .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
           .collect(Collectors.toList());

   RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
   Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
   results.forEach(r->{
    DeleteError deleteError = null;
    try {
     deleteError = r.get();
    } catch (Exception e) {
     e.printStackTrace();
     log.error("清楚分块文件失败,objectname:{}",deleteError.objectName(),e);
    }
   });
  } catch (Exception e) {
   e.printStackTrace();
   log.error("清楚分块文件失败,chunkFileFolderPath:{}",chunkFileFolderPath,e);
  }
 }



 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {


  //文件名
  String filename=uploadFileParamsDto.getFilename();
  //先得到扩展名
  String extension = filename.substring(filename.lastIndexOf("."));

  //通过扩展名得到mimeType
  String mimeType = getMimeType(extension);

  //子目录
  String defaultFolderPath = getDefaultFolderPath();
  //文件的md5值
  String fileMd5 = getFileMd5(new File(localFilePath));
  //objectName就是文件上传的时候.object()的参数
  String objectName=defaultFolderPath+fileMd5+extension;

  //将文件上传到minio
  boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_mediafiles, objectName);
  if(!result){
   XueChengPlusException.cast("上传文件失败");
  }

  //将文件信息保存到数据库
  //当在非事务方法中调用事务的方法，需要用代理类对象来调用事务方法，事务才一定能够生效
  //currentProxy就是当前类的一个代理类，是通过注入得到的代理类
//  @Autowired
//  MediaFileService currentProxy;
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);

  //如果返回的mediaFiles为null
  if(mediaFiles==null){
   XueChengPlusException.cast("文件上传后保存信息失败");
  }

  //准备返回的对象
  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
  return uploadFileResultDto;

 }

 @Override
 public RestResponse<Boolean> checkFile(String fileMd5) {

  //先查询数据库
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if(mediaFiles!=null){
   //桶
   String bucket = mediaFiles.getBucket();

   //这个数据库中的filePath就是查询minio时候.object中的参数
   String filePath = mediaFiles.getFilePath();

   //如果数据库存在再查询minio
   GetObjectArgs getObjectArgs = GetObjectArgs.builder()
           .bucket(bucket)
           .object(filePath)
           .build();

   //查询远程服务获取到一个流对象
   try {
    FilterInputStream fileInputStream = minioClient.getObject(getObjectArgs);
    if(fileInputStream!=null){
     //文件已存在
     return RestResponse.success(true);
    }

   } catch (Exception e) {
           e.printStackTrace();
  }

 }
  //文件不存在
 return RestResponse.success(false);
 }


 @Override
 public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

  //根据fileMd5来得到分块文件的存储路径，这个存储路径是自己根据fileMd5设置的
  //fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";   这个就是通过fileMd5得到分块文件存储路径的规则
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

  //如果数据库存在再查询minio
  GetObjectArgs getObjectArgs = GetObjectArgs.builder()
          //上传视频用bucket_video桶
          .bucket(bucket_video)
          //.object 就是chunkFileFolderPath 再加上分块文件的序号也就是文件名
          .object(chunkFileFolderPath+chunkIndex)
          .build();

  //查询远程服务获取到一个流对象
  try {
   FilterInputStream fileInputStream = minioClient.getObject(getObjectArgs);
   if(fileInputStream!=null){
    //文件已存在
    return RestResponse.success(true);
   }

  } catch (Exception e) {
   e.printStackTrace();
  }
 //文件不存在
 return RestResponse.success(false);
 }

 @Override
 public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {

  //分块文件的路径，getChunkFileFolderPath方法是自己写的
  String chunkFilePath = getChunkFileFolderPath(fileMd5)+chunk;
  //获取mimeType
  String mimeType = getMimeType(null);
  //将分块文件上传到minio
  boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_video, chunkFilePath);
  if(!b){
   return RestResponse.validfail(false,"上传文件失败");
  }
  //上传成功
  return RestResponse.success(true);

 }

 //得到分块文件的目录
 private String getChunkFileFolderPath(String fileMd5) {
  return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
 }


}
