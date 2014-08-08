package com.mike.imageloader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.mike.utils.L;

public class ImageLoaderClass {

	private Bitmap mBitmap;
	private static final int LOAD_BITMAP_URL = 1;
	private static final int LOAD_BITMAP_SDCARD = 2;
	private static final String TAG = "IMAGE LOADER : ";
	private static final String ERROR_CODE = "ERROR CODE : ";
	private static final boolean DEBUG = true;// Change to false if no Log is
												// required.

	private Rect mRect;

	public ImageLoaderClass() {

		super();

	}

	public void LoadUrlImages(String URLS, ImageView mImageView) {

		DownloadImages mDownloadImages = new DownloadImages(URLS, mImageView);
		mDownloadImages.execute();

	}

	private class DownloadImages extends AsyncTask<String, Void, Bitmap> {

		private String URLS;
		private ImageView mImageView;
		private final WeakReference<ImageView> imageViewReference;

		public DownloadImages(String URLS, ImageView mImageView) {

			this.URLS = URLS;
			this.mImageView = mImageView;
			imageViewReference = new WeakReference<ImageView>(mImageView);

		}

		@Override
		protected Bitmap doInBackground(String... params) {

			
			Bitmap mBitmap = downloadURL(URLS);

			return mBitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {

			mImageView = imageViewReference.get();

			mImageView.setImageBitmap(bitmap);

		}

	}

	private Bitmap downloadURL(String URLS) {
		if (DEBUG)
			L.m("DOWNLOADING IMAGES");
		final AndroidHttpClient mClient = AndroidHttpClient
				.newInstance("Android");
		final HttpGet getRequest = new HttpGet(URLS);

		try {

			HttpResponse mHttpResponse = mClient.execute(getRequest);
			final int statusCode = mHttpResponse.getStatusLine()
					.getStatusCode();

			if (statusCode != HttpStatus.SC_OK) {

				L.m(TAG + ERROR_CODE + statusCode);
				return null;
			}

			final HttpEntity mHttpEntity = mHttpResponse.getEntity();
			if (mHttpEntity != null) {

				InputStream is = null;

				try {

					is = mHttpEntity.getContent();

					final BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					options.inSampleSize = calculateInSampleSize(options);
					options.inJustDecodeBounds = false;

					mRect = new Rect(2, 2, 2, 2);

					final Bitmap bitmap = BitmapFactory.decodeStream(
							is, mRect, options);

					return bitmap;

				} finally {

					if (is != null) {
						/**
						 * Important to close the input stream. May cause
						 * battery draining and leaks.
						 **/
						is.close();
					}

					mHttpEntity.consumeContent();

				}

			}

		} catch (Exception e) {
			if (DEBUG)
				L.m(TAG + "ERROR DOWNLOADING BITMAP. ERROR MESSAGE"
						+ e.toString());
			getRequest.abort();
		} finally {

			if (mClient != null) {
				if (DEBUG)
					L.m(TAG + "DOWNLOADING COMPLETE."
							+ "CLOSING HTTP CLIENT NOW");
				mClient.close();

			}

		}

		return null;
	}

	public static int calculateInSampleSize(BitmapFactory.Options options) {

		int reqWidth = options.outWidth;
		int reqHeight = options.outHeight;

		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;

	}

	static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int bytes = read();
					if (bytes < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

}
