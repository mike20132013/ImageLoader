package com.mike.imageloader;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class ImageLoader extends AsyncTask<String, Void, Bitmap> {

	private final WeakReference<ImageView> imageViewReference;

	public ImageLoader(ImageView mImageView) {

		imageViewReference = new WeakReference<ImageView>(mImageView);
	}

	@Override
	protected Bitmap doInBackground(String... urls) {

		return downloadFile(urls[0]);
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {

		if (isCancelled()) {
			bitmap = null;
		}

		if (imageViewReference != null) {

			ImageView mImageView = imageViewReference.get();
			if (mImageView != null) {

				if (bitmap != null) {

					mImageView.setImageBitmap(bitmap);

				} else {

					mImageView.setImageDrawable(mImageView.getContext()
							.getResources()
							.getDrawable(R.drawable.list_placeholder));

				}

			}

		}

	}

	public Bitmap downloadFile(String Urls) {

		final AndroidHttpClient client = AndroidHttpClient
				.newInstance("Android");
		final HttpGet getRequest = new HttpGet(Urls);
		try {
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Log.w("ImageDownloader", "Error " + statusCode
						+ " while retrieving bitmap from " + Urls);
				return null;
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				try {
					inputStream = entity.getContent();
					final Bitmap bitmap = BitmapFactory
							.decodeStream(inputStream);
					return bitmap;
				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					entity.consumeContent();
				}
			}
		} catch (Exception e) {
			// Could provide a more explicit error message for IOException or
			// IllegalStateException
			getRequest.abort();
			Log.w("ImageDownloader", "Error while retrieving bitmap from "
					+ Urls);
		} finally {
			if (client != null) {
				client.close();
			}
		}
		return null;
	}

}
