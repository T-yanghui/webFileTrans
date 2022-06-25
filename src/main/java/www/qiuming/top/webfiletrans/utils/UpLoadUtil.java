package www.qiuming.top.webfiletrans.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UpLoadUtil {

   private final static String ENCODING = "utf-8";
   @Autowired
   private Environment environment;
   public boolean combineChunk(HttpServletRequest request, HttpServletResponse response){

      response.setCharacterEncoding(ENCODING);
      //长传时候会有多个分片，需要记录当前为那个分片
      Integer schunk = null;
      //总分片数
      Integer schunks = null;
      //名字
      String name = null;
      //文件目录
      final String path = environment.getProperty("customer.store.path");
      final Long maxSingleFileSize=environment.getProperty("customer.store.maxFileSize",Long.class);
      final Integer retryNumber=environment.getProperty("customer.store.retryNumber",Integer.class);

      BufferedOutputStream os = null;
      try {
         //设置缓冲区大小  先读到内存里在从内存写
         DiskFileItemFactory factory = new DiskFileItemFactory();
         factory.setSizeThreshold(1024);
         factory.setRepository(new File(path));
         //解析
         ServletFileUpload upload = new ServletFileUpload(factory);
         //设置单个大小与最大大小
         upload.setFileSizeMax(maxSingleFileSize);
         upload.setSizeMax(2*maxSingleFileSize);
         List<FileItem> items = upload.parseRequest(request);
         for (FileItem item : items) {
            if (item.isFormField()) {
               //获取分片数赋值给遍量
               if ("chunk".equals(item.getFieldName())) {
                  schunk = Integer.parseInt(item.getString(ENCODING));
               }
               if ("chunks".equals(item.getFieldName())) {
                  schunks = Integer.parseInt(item.getString(ENCODING));
               }
               if ("name".equals(item.getFieldName())) {
                  name = item.getString(ENCODING);
               }
            }
         }
         //取出文件基本信息后
         for (FileItem item : items) {
            if (!item.isFormField()) {
               //有分片需要临时目录
               String temFileName = name;
               if (name != null) {
                  if (schunk != null) {
                     temFileName = schunk + "_" + name;
                  }
                  //判断文件是否存在
                  File temfile = new File(path, temFileName);
                  //断点续传  判断文件是否存在，若存在则不传
                  if (!temfile.exists()) {
                     item.write(temfile);
                  }
               }
            }
         }
         //等待时sleep次数
         int retry=0;
         //文件合并  当前分片为最后一个就合并
         if (schunk != null && schunk.intValue() == schunks.intValue() - 1) {
            File tempFile = new File(path, name);
            os = new BufferedOutputStream(new FileOutputStream(tempFile));
            //根据之前命名规则找到所有分片
            for (int i = 0; i < schunks; i++) {
               File file = new File(path, i + "_" + name);
               //并发情况 需要判断所有  因为可能最后一个分片传完，之前有的还没传完
               retry=0;
               while (!file.exists()&&retryNumber.intValue()>retry) {
                  //不存在休眠100毫秒后再从新判断
                  retry++;
                  Thread.sleep(100);
               }
               if(!file.exists()){
                  throw new FileUploadException();
               }
               //分片存在  读入数组中
               byte[] bytes = FileUtils.readFileToByteArray(file);
               os.write(bytes);
               os.flush();
               file.delete();
            }
            os.flush();
         }
         log.info("write as "+path+name);
         return true;
      }catch (Exception e){
         e.printStackTrace();
         cleanTempFiles(path,name,schunks);
         return false;
      }finally {
         try {
            if (os != null) {
               os.close();
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
   public static void cleanTempFiles(String path,String name,int schunks){
      for (int i = 0; i < schunks; i++) {
         File file = new File(path, i + "_" + name);
         if(file.exists())
            file.delete();
      }
   }
}
