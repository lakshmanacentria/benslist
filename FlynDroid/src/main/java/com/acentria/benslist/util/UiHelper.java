package com.acentria.benslist.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;

public class UiHelper {


    /*uri find*/
    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        String path = "";
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);

            System.out.println("UiHelper=> "+ path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Uri.parse(path);
    }
}
