package com.ycm.webserver;

import android.os.Environment;

import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.ycm.demo.App;

import java.io.File;

public class FileUtils {

    /**
     * Create a random file based on mimeType.
     *
     * @param file file.
     *
     * @return file object.
     */
    public static File createLocalFile(MultipartFile file) {
//        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(file.getContentType().toString());
//        if (StringUtils.isEmpty(extension)) {
//            extension = MimeTypeMap.getFileExtensionFromUrl(file.getFilename());
//        }
//        String uuid = UUID.randomUUID().toString();
        return new File(App.getInstance().getRootDir(), file.getFilename());
    }

    /**
     * SD is available.
     *
     * @return true, otherwise is false.
     */
    public static boolean storageAvailable() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sd = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
            return sd.canWrite();
        } else {
            return false;
        }
    }
}
