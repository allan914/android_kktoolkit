/* Copyright (C) 2013 KKBOX Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * ​http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * KKImageManager
 */
package com.kkbox.toolkit.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import com.kkbox.toolkit.internal.image.KKImageRequest;
import com.kkbox.toolkit.internal.image.KKImageRequestListener;
import com.kkbox.toolkit.utils.StringUtils;
import com.kkbox.toolkit.utils.UserTask;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;

public class KKImageManager {
	public class ActionType {
		public static final int DOWNLOAD = 0;
		public static final int CALL_LISTENER = 1;
		public static final int UPDATE_VIEW_BACKGROUND = 2;
		public static final int UPDATE_VIEW_SOURCE = 3;
	}

	private static final int MAX_WORKING_COUNT = 10;
	private static final long FATAL_STORAGE_SIZE = 30 * 1024 * 1024;
	private static final HashMap<WeakReference<View>, Bitmap> viewBackgroundBitmapReference = new HashMap<WeakReference<View>, Bitmap>();
	private static final HashMap<WeakReference<ImageView>, Bitmap> imageViewSourceBitmapReference = new HashMap<WeakReference<ImageView>, Bitmap>();

	private final HashMap<View, KKImageRequest> fetchList = new HashMap<View, KKImageRequest>();
	private final ArrayList<KKImageRequest> workingList = new ArrayList<KKImageRequest>();
	private int workingCount = 0;
	private Context context;
	private Cipher cipher = null;

	protected KKImageRequestListener imageRequestListener = new KKImageRequestListener() {
		@Override
		public void onComplete(KKImageRequest request, Bitmap bitmap) {
			if (request.getActionType() == ActionType.CALL_LISTENER) {
				if (request.getImageCacheListener() != null) {
					request.getImageCacheListener().onReceiveBitmap(bitmap);
				}
			} else if (request.getActionType() == ActionType.UPDATE_VIEW_BACKGROUND) {
				View view = request.getView();
				view.setBackgroundDrawable(new BitmapDrawable(context.getResources(), bitmap));
				autoRecycleViewBackgroundBitmap(view);
				fetchList.remove(view);
			} else if (request.getActionType() == ActionType.UPDATE_VIEW_SOURCE) {
				ImageView imageView = (ImageView)request.getView();
				imageView.setImageBitmap(bitmap);
				autoRecycleViewSourceBitmap(imageView);
				fetchList.remove(request.getView());
			}
			workingCount--;
			workingList.remove(request);
			startFetch();
		}

		@Override
		public void onNetworkError(KKImageRequest request) {
			if (request.getView() != null) {
				fetchList.remove(request.getView());
			}
			workingCount--;
			workingList.remove(request);
			startFetch();
		}
	};

	public static String getTempImagePath(Context context, String url) {
		final File cacheDir = new File(context.getCacheDir().getAbsolutePath() + File.separator + "image");
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		return context.getCacheDir().getAbsolutePath() + File.separator + "image" + File.separator + StringUtils.getMd5Hash(url);
	}

	public static void removeCacheIfExists(Context context, String url) {
		final File cacheFile = new File(getTempImagePath(context, url));
		cacheFile.delete();
	}

	public static void autoRecycleViewBackgroundBitmap(View view) {
		if (Build.VERSION.SDK_INT < 11) {
			Iterator iterator = viewBackgroundBitmapReference.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<WeakReference<View>, Bitmap> entry = (Map.Entry<WeakReference<View>, Bitmap>)iterator.next();
				View currentView = entry.getKey().get();
				if (view.equals(currentView)) {
					Bitmap bitmap = entry.getValue();
					if (bitmap != null) {
						bitmap.recycle();
						bitmap = null;
					}
					iterator.remove();
				}
			}
			Drawable drawable = view.getBackground();
			if (drawable instanceof BitmapDrawable) {
				Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
				viewBackgroundBitmapReference.put(new WeakReference<View>(view), bitmap);
			}
		}
	}

	public static void autoRecycleViewSourceBitmap(ImageView view) {
		if (Build.VERSION.SDK_INT < 11) {
			Iterator iterator = imageViewSourceBitmapReference.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<WeakReference<ImageView>, Bitmap> entry = (Map.Entry<WeakReference<ImageView>, Bitmap>)iterator.next();
				ImageView currentView = entry.getKey().get();
				if (view.equals(currentView)) {
					Bitmap bitmap = entry.getValue();
					if (bitmap != null) {
						bitmap.recycle();
						bitmap = null;
					}
					iterator.remove();
				}
			}
			Drawable drawable = view.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
				imageViewSourceBitmapReference.put(new WeakReference<ImageView>(view), bitmap);
			}
		}
	}

	public static void gc() {
		if (Build.VERSION.SDK_INT < 11) {
			Iterator iterator = viewBackgroundBitmapReference.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<WeakReference<View>, Bitmap> entry = (Map.Entry<WeakReference<View>, Bitmap>)iterator.next();
				if (entry.getKey().get() == null) {
					Bitmap bitmap = entry.getValue();
					if (bitmap != null) {
						bitmap.recycle();
						bitmap = null;
					}
					iterator.remove();
				}
			}
			iterator = imageViewSourceBitmapReference.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<WeakReference<ImageView>, Bitmap> entry = (Map.Entry<WeakReference<ImageView>, Bitmap>)iterator.next();
				if (entry.getKey().get() == null) {
					Bitmap bitmap = entry.getValue();
					if (bitmap != null) {
						bitmap.recycle();
						bitmap = null;
					}
					iterator.remove();
				}
			}
		}
	}

	public KKImageManager(Context context, Cipher localCipher) {
		this.context = context;
		this.cipher = localCipher;
		if (Build.VERSION.SDK_INT >= 9 && context.getCacheDir().getFreeSpace() < FATAL_STORAGE_SIZE) {
			File cacheDir = new File(context.getCacheDir().getAbsolutePath() + File.separator + "image");
			if (cacheDir.exists()) {
				for (File file : cacheDir.listFiles()) {
					file.delete();
				}
			}
		}
		gc();
	}

	public void downloadBitmap(String url, String localPath, KKImageOnReceiveHttpHeaderListener onReceiveHttpHeaderListener) {
		KKImageRequest request = new KKImageRequest(context, url, localPath, onReceiveHttpHeaderListener, cipher);
		workingList.add(request);
		startFetch();
	}

	public KKImageRequest loadBitmap(KKImageListener listener, String url, String localPath) {
		KKImageRequest request = new KKImageRequest(context, url, localPath, listener, cipher);
		workingList.add(request);
		startFetch();
		return request;
	}

	public void updateViewSource(ImageView view, String url, String localPath, int defaultResourceId) {
		updateView(view, url, localPath, defaultResourceId, false, false, null);
	}

	public void updateViewSourceAndSave(ImageView view, String url, String localPath, int defaultResourceId,
			KKImageOnReceiveHttpHeaderListener onReceiveHttpHeaderListener) {
		updateView(view, url, localPath, defaultResourceId, false, true, onReceiveHttpHeaderListener);
	}

	public void updateViewBackground(View view, String url, String localPath, int defaultResourceId) {
		updateView(view, url, localPath, defaultResourceId, true, false, null);
	}

	public void updateViewBackgroundAndSave(View view, String url, String localPath, int defaultResourceId,
			KKImageOnReceiveHttpHeaderListener onReceiveHttpHeaderListener) {
		updateView(view, url, localPath, defaultResourceId, true, true, onReceiveHttpHeaderListener);
	}

	public Bitmap loadCache(String url, String localPath) {
		String cachePath = getTempImagePath(context, url);
		final File cacheFile = new File(cachePath);
		if (cacheFile.exists()) { return BitmapFactory.decodeFile(cachePath); }
		return null;
	}

	@Override
	public void finalize() {
		for (KKImageRequest request : workingList) {
			request.cancel();
		}
	}

	private void updateView(View view, String url, String localPath, int defaultResourceId, boolean updateBackground, boolean saveToLocal,
			KKImageOnReceiveHttpHeaderListener onReceiveHttpHeaderListener) {
		KKImageRequest request = fetchList.get(view);
		if (request != null) {
			if (request.getUrl().equals(url)) {
				return;
			} else {
				if (request.getStatus() == UserTask.Status.RUNNING) {
					workingCount--;
				}
				request.cancel();
				workingList.remove(request);
			}
		}
		Bitmap bitmap = loadCache(url, localPath);
		if (bitmap != null) {
			if (updateBackground) {
				view.setBackgroundDrawable(new BitmapDrawable(context.getResources(), bitmap));
				autoRecycleViewBackgroundBitmap(view);
			} else {
				ImageView imageView = (ImageView)view;
				imageView.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
				autoRecycleViewSourceBitmap(imageView);
			}

			return;
		} else if (defaultResourceId > 0) {
			if (updateBackground) {
				view.setBackgroundResource(defaultResourceId);
			} else {
				ImageView imageView = (ImageView)view;
				imageView.setImageResource(defaultResourceId);
			}
		}
		request = new KKImageRequest(context, url, localPath, onReceiveHttpHeaderListener, view, updateBackground, cipher, saveToLocal);
		workingList.add(request);
		fetchList.put(view, request);
		startFetch();
	}

	private void startFetch() {
		if (workingCount < MAX_WORKING_COUNT) {
			for (KKImageRequest request : workingList) {
				if (request.getStatus() == UserTask.Status.PENDING) {
					request.execute(imageRequestListener);
					workingCount++;
				}
				if (workingCount >= MAX_WORKING_COUNT) {
					break;
				}
			}
		}
	}
}
