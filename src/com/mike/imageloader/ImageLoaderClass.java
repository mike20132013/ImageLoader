package com.mike.imageloader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.mike.utils.L;

@SuppressLint("NewApi")
public class ImageLoaderClass {

	private static final int LOAD_BITMAP_URL = 1;
	private static final int LOAD_BITMAP_SDCARD = 2;
	private static final String TAG = "IMAGE LOADER : ";
	private static final String ERROR_CODE = "ERROR CODE : ";
	private static final boolean DEBUG = true;// Change to false if no Log is
												// required.

	private Rect mRect;

	private static final int HARD_CACHE_CAPACITY = 10;
	private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

	private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
			HARD_CACHE_CAPACITY / 2);

	// Hard cache, with a fixed maximum capacity and a life duration
	private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>(
			HARD_CACHE_CAPACITY / 2, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(
				LinkedHashMap.Entry<String, Bitmap> eldest) {
			if (size() > HARD_CACHE_CAPACITY) {
				// Entries push-out of hard reference cache are transferred to
				// soft reference cache
				sSoftBitmapCache.put(eldest.getKey(),
						new SoftReference<Bitmap>(eldest.getValue()));
				return true;
			} else
				return false;
		}
	};

	private final Handler purgeHandler = new Handler();

	private final Runnable purger = new Runnable() {
		public void run() {
			clearCache();
		}
	};

	public ImageLoaderClass() {

		super();

	}

	/**
	 * Clears the image cache used internally to improve performance. Note that
	 * for memory efficiency reasons, the cache will automatically be cleared
	 * after a certain inactivity delay.
	 */
	public void clearCache() {
		sHardBitmapCache.clear();
		sSoftBitmapCache.clear();
	}

	public void LoadUrlImages(Context context, String URLS, ImageView mImageView) {

		resetPurgeTimer();

		Bitmap bitmap = getBitmapFromCache(URLS);

		if (bitmap == null) {

			Log.d(TAG, "BITMAP NULL..AND EXECUTING THIS!");
			DownloadImagesTask mDownloadImages = new DownloadImagesTask(
					context, URLS, mImageView);
			mDownloadImages.execute();

		} else {

			Log.d(TAG, "BITMAP NOT NULL..AND EXECUTING THIS!");
			cancelDownload(URLS, mImageView);
			mImageView.setImageBitmap(bitmap);

		}

	}

	private static boolean cancelDownload(String URL, ImageView mImageView) {

		DownloadImagesTask mDownloadImagesTask = getBitmapDownloaderTask(mImageView);

		if (mDownloadImagesTask != null) {

			String bitmapURL = mDownloadImagesTask.URLS;

			if (bitmapURL == null || (!bitmapURL.equals(URL))) {

				mDownloadImagesTask.cancel(true);
			} else {

				return false;
			}

		}

		return true;
	}

	private static DownloadImagesTask getBitmapDownloaderTask(
			ImageView mImageView) {

		if (mImageView != null) {

			Drawable mDrawable = mImageView.getDrawable();

			if (mDrawable instanceof DownloadedDrawable) {

				DownloadedDrawable downloadedDrawable = (DownloadedDrawable) mDrawable;
				return downloadedDrawable.getBitmapDownloaderTask();

			}

		}

		return null;

	}

	static class DownloadedDrawable extends ColorDrawable {
		private final WeakReference<DownloadImagesTask> bitmapDownloaderTaskReference;

		public DownloadedDrawable(DownloadImagesTask bitmapDownloaderTask) {
			super(Color.BLACK);
			bitmapDownloaderTaskReference = new WeakReference<DownloadImagesTask>(
					bitmapDownloaderTask);
		}

		public DownloadImagesTask getBitmapDownloaderTask() {
			return bitmapDownloaderTaskReference.get();
		}
	}

	Bitmap downloadBitmap(String url) {
		final int IO_BUFFER_SIZE = 4 * 1024;

		final AndroidHttpClient client = AndroidHttpClient
				.newInstance("Android");

		final HttpGet getRequest = new HttpGet(url);

		try {
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Log.w("ImageDownloader", "Error " + statusCode
						+ " while retrieving bitmap from " + url);
				return null;
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				try {
					inputStream = entity.getContent();
					// return BitmapFactory.decodeStream(inputStream);
					// Bug on slow connections, fixed in future release.
					return BitmapFactory.decodeStream(new FlushedInputStream(
							inputStream));
				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					entity.consumeContent();
				}
			}
		} catch (IOException e) {
			getRequest.abort();
			Log.w(TAG, "I/O error while retrieving bitmap from " + url, e);
		} catch (IllegalStateException e) {
			getRequest.abort();
			Log.w(TAG, "Incorrect URL: " + url);
		} catch (Exception e) {
			getRequest.abort();
			Log.w(TAG, "Error while retrieving bitmap from " + url, e);
		} finally {
			if ((client instanceof AndroidHttpClient)) {
				((AndroidHttpClient) client).close();
			}
		}
		return null;
	}

	private class DownloadImagesTask extends AsyncTask<String, Void, Bitmap> {

		private String URLS;
		private final WeakReference<ImageView> imageViewReference;

		public DownloadImagesTask(Context context, String URLS,
				ImageView mImageView) {

			this.URLS = URLS;
			imageViewReference = new WeakReference<ImageView>(mImageView);

		}

		@Override
		protected Bitmap doInBackground(String... params) {

			//URLS = params[0];

			return downloadBitmap(URLS);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {

			// mImageView = new ImageView(context);
			//
			// mImageView.setScaleType(ScaleType.CENTER_CROP);
			// mImageView.setImageBitmap(bitmap);

			addBitmapToCache(URLS, bitmap);

			if (imageViewReference != null) {

				ImageView mImageView = imageViewReference.get();
				DownloadImagesTask mImagesTask = getBitmapDownloaderTask(mImageView);
				if (this == mImagesTask) {
					Log.d(TAG, "THIS!");
					mImageView.setScaleType(ScaleType.CENTER_CROP);
					mImageView.setImageBitmap(bitmap);
				} else {

					Log.d(TAG, "Not Working!");

				}

			}

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

					final Bitmap bitmap = BitmapFactory.decodeStream(is, mRect,
							options);

					// final Bitmap bitmap2 = BitmapFactory.decodeStream(is);

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

				mClient.close();

				if (mClient != null) {
					if (DEBUG) {
						L.m(TAG + " STILL DOWNLOADING"
								+ "TRYING TO CLOSE HTTP CLIENT NOW");
					}
				}

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

	/**
	 * @param url
	 *            The URL of the image that will be retrieved from the cache.
	 * @return The cached bitmap or null if it was not found.
	 */
	private Bitmap getBitmapFromCache(String url) {
		// First try the hard reference cache
		synchronized (sHardBitmapCache) {
			final Bitmap bitmap = sHardBitmapCache.get(url);
			if (bitmap != null) {
				// Bitmap found in hard cache
				// Move element to first position, so that it is removed last
				sHardBitmapCache.remove(url);
				sHardBitmapCache.put(url, bitmap);
				return bitmap;
			}
		}

		// Then try the soft reference cache
		SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
		if (bitmapReference != null) {
			final Bitmap bitmap = bitmapReference.get();
			if (bitmap != null) {
				// Bitmap found in soft cache
				return bitmap;
			} else {
				// Soft reference has been Garbage Collected
				sSoftBitmapCache.remove(url);
			}
		}

		return null;
	}

	/**
	 * Adds this bitmap to the cache.
	 * 
	 * @param bitmap
	 *            The newly downloaded bitmap.
	 */
	private void addBitmapToCache(String url, Bitmap bitmap) {
		if (bitmap != null) {
			synchronized (sHardBitmapCache) {
				sHardBitmapCache.put(url, bitmap);
			}
		}
	}

	/**
	 * Allow a new delay before the automatic cache clear is done.
	 */
	private void resetPurgeTimer() {
		purgeHandler.removeCallbacks(purger);
		purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
	}

}
