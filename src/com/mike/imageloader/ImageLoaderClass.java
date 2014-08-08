package com.mike.imageloader;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

public class ImageLoaderClass {

	private Bitmap mBitmap;

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

		public DownloadImages(String URLS, ImageView mImageView) {

			this.URLS = URLS;
			this.mImageView = mImageView;

		}

		@Override
		protected Bitmap doInBackground(String... params) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}

	}

}
