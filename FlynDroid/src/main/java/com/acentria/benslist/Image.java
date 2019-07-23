package com.acentria.benslist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Image {
	
	final public static int PIC_CROP = 4101;
	public static ImageView requestedHolder;
	public static String requestItem;
	public static AsyncHttpResponseHandler onSuccessListener;
	public static Uri outputFileUri;
	
	public static void openGallery(int returnCode, Context context, ImageView holder, String item, AsyncHttpResponseHandler success){
		requestedHolder = holder;
		requestItem = item;
		onSuccessListener = success;
		
		/* gallary intent */
        Intent gallary_intent = new Intent();
        gallary_intent.setType("image/*");
        gallary_intent.setAction(Intent.ACTION_GET_CONTENT);
        
        /* camera intent */
        List<Intent> camera_intents = new ArrayList<Intent>();
        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/thumbnails");
        String captured_image = Account.accountData.get("username") +"_"+ System.currentTimeMillis() +".jpg";
        File file = new File(myDir, captured_image);
        captured_image = file.getAbsolutePath();
        outputFileUri = Uri.fromFile(file);
        camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        camera_intent.putExtra("return-data", true);
        camera_intents.add(camera_intent);

        /* merge and start intent chooser */
        Intent chooserIntent = Intent.createChooser(gallary_intent, Lang.get("select_source"));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, camera_intents.toArray(new Parcelable[]{}));
        ((Activity) context).startActivityForResult(chooserIntent, returnCode);
	}
	
	public static void manageSelectedImage(int resultCode, Intent data, Context instance) {

		if ( resultCode == Activity.RESULT_CANCELED ){
			Toast.makeText(instance, Lang.get("file_selection_canceled"), Toast.LENGTH_LONG).show();
		}
		else if ( resultCode == Activity.RESULT_OK ) {
			boolean isCamera;
			Uri selectedImageUri;
			
			if ( data == null ) {
			    isCamera = true;
			}
			else {
			    if( data.getAction() == null ) {
			        isCamera = false;
			    }
			    else {
			        isCamera = data.getAction().equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			    }
			}
			
			if ( isCamera ) {
				selectedImageUri = outputFileUri;
			}
			else {
			    selectedImageUri = data == null ? null : data.getData();
			}
			
			if ( selectedImageUri == null ) {
				Toast.makeText(instance, "File isn't selected, please try again", Toast.LENGTH_LONG).show();
			}
			else {
				if ( instance instanceof AddListingActivity ) {
					AddListingActivity.addListing.addPicture(selectedImageUri.toString());
					//AddListing.add(selectedImageUri, PIC_CROP);
				}
				else {				
					int thumb_width = Integer.parseInt(Utils.getCacheConfig("account_thumb_width"));
					thumb_width = thumb_width > 0 ? thumb_width * 2 : 200;
					
					int thumb_height = Integer.parseInt(Utils.getCacheConfig("account_thumb_height"));
					thumb_height = thumb_height > 0 ? thumb_height * 2 : 200;
					
					performCrop(selectedImageUri, thumb_width, thumb_height, PIC_CROP);
				}
			}
	    }
	}
	
	public static void performCrop(Uri picUri, int width, int height, int returnCode) {
		Bitmap originalImage;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			AssetFileDescriptor fileDescriptor = null;
			fileDescriptor = Config.context.getContentResolver().openAssetFileDescriptor( Uri.parse(picUri.toString()), "r");
			originalImage = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
			originalImage = ExifUtil.rotateBitmap(picUri.toString(), originalImage);

			Bitmap background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

			float originalWidth = originalImage.getWidth(), originalHeight = originalImage.getHeight();
			Canvas canvas = new Canvas(background);
			float scale = width/originalWidth;
			float xTranslation = 0.0f, yTranslation = (height - originalHeight * scale)/2.0f;
			Matrix transformation = new Matrix();
			transformation.postTranslate(xTranslation, yTranslation);
			transformation.preScale(scale, scale);
			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(originalImage, transformation, paint);

			upload(background);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    try {
//	        Intent cropIntent = new Intent("com.android.camera.action.CROP");
//	        // indicate image type and Uri
//	        cropIntent.setDataAndType(picUri, "image/*");
//
//	        // set crop properties
//	        cropIntent.putExtra("crop", "true");
//	        cropIntent.putExtra("aspectX", width);
//	        cropIntent.putExtra("aspectY", height);
//	        cropIntent.putExtra("outputX", width);
//	        cropIntent.putExtra("outputY", height);
//	        cropIntent.putExtra("return-data", true);
//
//	        Config.context.startActivityForResult(cropIntent, returnCode);
//	    }
//	    catch (ActivityNotFoundException anfe) {
//	    	Bitmap originalImage;
//			try {
//				originalImage = MediaStore.Images.Media.getBitmap(Config.context.getContentResolver(), picUri);
//				Bitmap background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//		    	float originalWidth = originalImage.getWidth(), originalHeight = originalImage.getHeight();
//		    	Canvas canvas = new Canvas(background);
//		    	float scale = width/originalWidth;
//		    	float xTranslation = 0.0f, yTranslation = (height - originalHeight * scale)/2.0f;
//		    	Matrix transformation = new Matrix();
//		    	transformation.postTranslate(xTranslation, yTranslation);
//		    	transformation.preScale(scale, scale);
//		    	Paint paint = new Paint();
//		    	paint.setFilterBitmap(true);
//		    	canvas.drawBitmap(originalImage, transformation, paint);
//
//		    	upload(background);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//	    }
	}
	
	public static void upload(Bitmap image) {
		/* set selected image to the related image view */
		if ( requestedHolder != null ) {
			requestedHolder.setImageBitmap(image);
		}
		
		/* send selected image to the server */
		RequestParams params = new RequestParams();
		
		File file = new File(Environment.getExternalStorageDirectory() + "/tmpfiletosend.jpg");
		FileOutputStream out;
		try {
			out = new FileOutputStream(file);
			image.compress(Bitmap.CompressFormat.JPEG, 85, out);
			params.put("image", file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		HashMap<String, String> add_params = new HashMap<String, String>();
		add_params.put("account_id", Account.accountData.get("id"));
		add_params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		
		/* do request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.setTimeout(30000); // set 30 seconds for this task
		
		final String url = Utils.buildRequestUrl(requestItem, add_params, null);
    	client.post(url, params, onSuccessListener);
	}
}