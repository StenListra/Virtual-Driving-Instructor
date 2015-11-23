package com.example.meelis.virtualdrivinginstructor;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by SLIST on 23-Nov-15.
 */
public class FileUtility {
    public FileUtility(File file, String location){
        if (file.exists()){
            file.delete();
            new File(Environment.getExternalStorageDirectory() + location);
        }
    }
    public void writeToFile(String message, File file){
        try {
            BufferedWriter out;
            FileWriter datawriter = new FileWriter(file,true);
            out = new BufferedWriter(datawriter);
            if (file.exists()) {
                out.write(message + "\n");
                out.flush();
            }
            out.close();
        } catch (IOException e) {
            Log.e("Error", "fail to write file");
        }
    }

}
