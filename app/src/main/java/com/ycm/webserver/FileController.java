package com.ycm.webserver;

import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.ycm.demo.App;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping(path = "/file")
public class FileController {

    @GetMapping(path = "/list")
    public String info() {
        JSONArray array = new JSONArray();

        File[] files = App.getInstance().getRootDir().listFiles();
        if (files.length > 0) {

            for (File file : files) {
                if (file.isDirectory()) continue;

                JSONObject item = new JSONObject();
                try {
                    item.put("name", file.getName());
                    item.put("size", file.length());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                array.put(item);
            }

        }

        return array.toString();
    }


    @PostMapping(path = "/upload")
    String upload(@RequestParam("file") MultipartFile file) throws IOException {
        File localFile = FileUtils.createLocalFile(file);
        file.transferTo(localFile);
        return localFile.getAbsolutePath();
    }

}
