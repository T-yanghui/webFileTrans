package www.qiuming.top.webfiletrans.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import www.qiuming.top.webfiletrans.utils.UpLoadUtil;

@Slf4j
@RestController
public class WebUploadController {
    @Autowired
    private UpLoadUtil upLoadUtil;
    @RequestMapping("/upload")
    @ResponseBody
    public void upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String res= upLoadUtil.combineChunk(request,response)?"上传成功":"上传失败";
    }
}
