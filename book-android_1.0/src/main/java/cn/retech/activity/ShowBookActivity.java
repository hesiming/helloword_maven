package main.java.cn.retech.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import kr.co.netntv.player4ux.Player4UxView;
import kr.co.netntv.player4ux.PlayerCore;
import main.java.cn.retech.custom_control.MemoryStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import cn.retech.activity.R;
import cn.retech.toolutils.DebugLog;

import com.umeng.analytics.MobclickAgent;

public class ShowBookActivity extends Activity {

	private class ExtractZipFileTaskInLocal extends AsyncTask<String, Integer, String> {
		int entryCount = 0;

		public ExtractZipFileTaskInLocal() {
			isBackGround_ = true;
		}

		@Override
		protected String doInBackground(String... params) {
			try {
				createDocumentFileInLocal2();
			} catch (Exception e) {
				e.printStackTrace();
				return "Fail";
			}
			return "Success";
		}

		@Override
		protected void onPostExecute(String result) {
			findViewById(R.id.progress_layout).setVisibility(View.GONE);
			if (result.contentEquals("Success") == false) {
				errorMessage();
				return;
			}
			readDocument();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			((TextView) findViewById(R.id.state_text)).setText("打开中，请稍后...");
			int percent = values[0] * 100 / entryCount;
			((TextView) findViewById(R.id.progress_text)).setText(values[0] + " / " + entryCount + "(" + percent + "%)");
		}

		private void createDocumentFileInLocal2() throws Exception {
			ZipInputStream zipFile = new ZipInputStream(getAssets().open(ASSET_FILE));
			while (zipFile.getNextEntry() != null) {
				entryCount++;
			}

			ZipInputStream zip = new ZipInputStream(getAssets().open(ASSET_FILE));
			ZipEntry ze;
			int index = 0;

			while ((ze = zip.getNextEntry()) != null) {
				final String path = ze.getName();

				if (ze.getName().indexOf("/") != -1) {
					File parent = new File(path).getParentFile();
					if (!parent.exists()) {
						if (!parent.mkdirs()) {
							throw new IOException("Unable to create folder " + parent);
						}
					}
				}

				DebugLog.d(TAG, path);
				FileOutputStream fout = openFileOutput(path, Context.MODE_WORLD_READABLE);// new
				byte[] bytes = new byte[4096];

				for (int c = zip.read(bytes); c != -1; c = zip.read(bytes)) {
					fout.write(bytes, 0, c);
				}
				zip.closeEntry();
				fout.close();

				index++;
				publishProgress(index);
			}
		}
	}

	private class ExtractZipFileTaskInSdcard extends AsyncTask<String, Integer, String> {
		File zipFile;
		int entryCount;

		public ExtractZipFileTaskInSdcard(File zipFile) {
			this.zipFile = zipFile;
			isBackGround_ = true;
		}

		@Override
		protected String doInBackground(String... params) {
			if (MemoryStatus.getAvailableExternalMemorySize() > zipFile.length() * 3) { // TODO
																																									// memory
																																									// required
				try {
					Thread.sleep(500);
					// createDocumentFileInSdCard(zipFile);
					// getFileStreamPath(ZIP_FILE).delete();
					DebugLog.d(TAG, "Contents File Proc Complete............");
					return "Success";
				} catch (Exception e) {
					e.printStackTrace();
					return "Fail";
				}
			} else {
				return "解压课件所需要的空间不足！";
			}
		}

		@Override
		protected void onPostExecute(String result) {
			findViewById(R.id.progress_layout).setVisibility(View.GONE);
			if (result.contentEquals("Success") == false) {
				if (result.contentEquals("MemoryError") == true) {
					Toast.makeText(ShowBookActivity.this, "Memory Error! (File size: " + MemoryStatus.formatSize(zipFile.length()) + ")", Toast.LENGTH_SHORT).show();
					finish();
					return;
				} else {
					errorMessage();
					return;
				}
			}

			File zipFile = new File(mZipFilepath);
			zipFile.delete();

			readDocument();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			int percent = values[0] * 100 / entryCount;
			((TextView) findViewById(R.id.progress_text)).setText(values[0] + " / " + entryCount + "(" + percent + "%)");
		}

		private void createDocumentFileInSdCard(File zipFile) throws Exception {
			ZipFile file = new ZipFile(zipFile + "/" + DOC_FILE);
			entryCount = file.size();
			ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze;
			String zipFileParent = zipFile.getParent();
			int index = 0;

			while ((ze = zip.getNextEntry()) != null) {
				String path = ze.getName();

				if (ze.getName().indexOf("/") != -1) {
					File parent = new File(path).getParentFile();
					if (!parent.exists()) {
						if (!parent.mkdirs()) {
							throw new IOException("Unable to create folder " + parent);
						}
					}
				}

				DebugLog.d(TAG, path);
				FileOutputStream fout = new FileOutputStream(zipFileParent + "/" + path);
				byte[] bytes = new byte[4096];

				for (int c = zip.read(bytes); c != -1; c = zip.read(bytes)) {
					fout.write(bytes, 0, c);
				}
				zip.closeEntry();
				fout.close();

				index++;
				publishProgress(index);
			}
		}
	}

	private final String TAG = this.getClass().getSimpleName();
	public static final String PREFERENCE_FILE = "preference_file";
	public static final String ORIENTATION = "orientation";
	private static final String ASSET_FILE = "document.zip";
	private static final String DOC_FILE = "document.st";

	private static final String ZIP_FILE = "tmp.zip";

	private static final String THUMBNAIL_FILENAME = "thumbnail_player4ux.image";
	public static final String EXTRA_ZIP_FILE = "EXTRA_ZIP_FILE";
	Player4UxView mGLView;
	String mZipFilepath;
	String mContentsPath;

	private boolean bInit = false;

	private boolean isBackGround_;

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == 0) {
			AlertDialog.Builder alert = new AlertDialog.Builder(ShowBookActivity.this);
			alert.setTitle("DreamBook");
			alert.setMessage("是否返回首页？");
			alert.setPositiveButton("是", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			});
			alert.setNegativeButton("否", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

			alert.setCancelable(false);
			if (!ShowBookActivity.this.isFinishing()) {
				alert.show();
			}
			return true;
		} else {
			return super.dispatchKeyEvent(event);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (bInit == false) {
			DebugLog.e(TAG, "onWindowFocusChanged hasFocus = " + hasFocus);
			if (isBackGround_ == false) {
				applyAspectRatio();
			}
		}
		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	protected void onCreate(Bundle icicle) {
		DebugLog.e(TAG, "onCreate");
		super.onCreate(icicle);
		Window w = getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.player4ux_main);

		PhoneStateListener phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
				case TelephonyManager.CALL_STATE_IDLE:
					if (mGLView != null) {
						mGLView.resumeMedia();
					}
					break;
				case TelephonyManager.CALL_STATE_RINGING:
				case TelephonyManager.CALL_STATE_OFFHOOK:
					if (mGLView != null) {
						mGLView.pauseMedia();
					}
					break;
				default:
					break;
				}
			}
		};

		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		Intent i = getIntent();
		mZipFilepath = i.getStringExtra(EXTRA_ZIP_FILE);
		if (mZipFilepath == null) {
			mContentsPath = getFilesDir().getAbsolutePath();
		} else {
			mContentsPath = new File(mZipFilepath).getPath();
		}

		if (mZipFilepath == null) {
			if (isVersionUpdated()) {
				findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
				new ExtractZipFileTaskInLocal().execute();
			} else {
				if (!getFileStreamPath(DOC_FILE).exists()) {
					findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
					new ExtractZipFileTaskInLocal().execute();
				} else {
					readDocument();
				}
			}
		} else {
			File zipFile = new File(mZipFilepath);
			// if (!new File(zipFile.getPath() + "/" + DOC_FILE).exists()) {
			findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
			new ExtractZipFileTaskInSdcard(zipFile).execute();
			// } else {
			// readDocument();
			// }
		}
	}

	@Override
	protected void onPause() {
		DebugLog.e(TAG, "onPause");
		if (mGLView != null) {
			mGLView.onPause();
		}
		super.onPause();

		MobclickAgent.onPause(this);
	}

	@Override
	protected void onResume() {
		DebugLog.e(TAG, "onResume");
		if (mGLView != null) {
			mGLView.onResume();
		}
		super.onResume();

		MobclickAgent.onResume(this);
	}

	private void applyAspectRatio() {
		View main_frame = findViewById(R.id.main_frame);

		int contentsWidth = PlayerCore.getDocumentWidth();
		int contentsHeight = PlayerCore.getDocumentHeight();
		float contentsAspectRatio = (float) contentsWidth / contentsHeight;
		int w = main_frame.getWidth();
		int h = main_frame.getHeight();
		float terminalAspectRatio = (float) w / h;

		DebugLog.e(TAG, "contents = " + contentsAspectRatio + ", w = " + contentsWidth + ", h = " + contentsHeight);
		DebugLog.e(TAG, "terminal = " + terminalAspectRatio + ", w = " + w + ", h = " + h);
		initPlayer4UxView();
		LayoutParams params = (LayoutParams) mGLView.getLayoutParams();
		if (terminalAspectRatio < 1.0f) { // portrait
			if (terminalAspectRatio < contentsAspectRatio) {
				applySize(false, contentsWidth, contentsHeight, w, h, params);
			} else {
				applySize(true, contentsWidth, contentsHeight, w, h, params);
			}
		} else {
			if (terminalAspectRatio < contentsAspectRatio) {
				applySize(false, contentsWidth, contentsHeight, w, h, params);
			} else {
				applySize(true, contentsWidth, contentsHeight, w, h, params);
			}
		}
		DebugLog.e(TAG, "params = " + (float) params.width / params.height + ", w = " + params.width + ", h = " + params.height);
		mGLView.requestLayout();
	}

	private void applySize(boolean bKeepHeight, int contentsWidth, int contentsHeight, int w, int h, LayoutParams params) {
		if (bKeepHeight) {
			params.gravity = Gravity.CENTER;
			params.height = h;
			float ratio = (float) h / contentsHeight;
			params.width = (int) (ratio * contentsWidth + 0.5f);
		} else {
			params.gravity = Gravity.CENTER;
			params.width = w;
			float ratio = (float) w / contentsWidth;
			params.height = (int) (ratio * contentsHeight + 0.5f);
		}
	}

	private void errorMessage() {
		Toast.makeText(ShowBookActivity.this, "无法正确读取课件，请确认设备以及SD卡的可用空间，或者在删除课件之后重新下载！", Toast.LENGTH_SHORT).show();
		File currentContentsDir = new File(mContentsPath);
		String strZipFile;
		if (mZipFilepath == null) {
			strZipFile = ZIP_FILE;
		} else {
			File zipFile = new File(mZipFilepath);
			strZipFile = zipFile.getName();
		}
		String fileList[] = currentContentsDir.list();
		for (String strFile : fileList) {
			if (strFile.contentEquals(strZipFile)) {
				continue;
			}
			if (strFile.contentEquals(THUMBNAIL_FILENAME)) {
				continue;
			}
			File file = new File(currentContentsDir, strFile);
			if (file.exists()) {
				file.delete();
			}
		}
		finish();
	}

	private void initPlayer4UxView() {
		bInit = true;
		FrameLayout parentView = (FrameLayout) findViewById(R.id.main_frame);
		mGLView = new Player4UxView(this, getApplication(), true, 16, 8, mContentsPath, parentView, ShowBookActivity.class);
		parentView.addView(mGLView);
	}

	private boolean isVersionUpdated() {
		PackageManager pm = getPackageManager();
		int curVersionCode = 1;
		try {
			curVersionCode = pm.getPackageInfo(getPackageName(), 0).versionCode;
			DebugLog.e("test", "ver = " + curVersionCode);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		int prevVersionCode = getPreferences(MODE_PRIVATE).getInt("versionCode", 1);
		DebugLog.e("test", "pref versionCode = " + prevVersionCode);

		if (prevVersionCode < curVersionCode) {
			DebugLog.e("test", "update");
			Editor editor = getPreferences(MODE_PRIVATE).edit();
			editor.putInt("versionCode", curVersionCode);
			editor.commit();
			return true;
		}

		return false;
	}

	private void readDocument() {
		DebugLog.e(TAG, "read before = " + mContentsPath);
		boolean bSuccess = PlayerCore.readDocument(mContentsPath);
		DebugLog.e(TAG, "readDocument = " + bSuccess);
		if (bSuccess == false) {
			return;
		}
		DebugLog.e(TAG, "read after");
		int orientation = PlayerCore.getOrientation();
		Editor editor = getSharedPreferences(ShowBookActivity.PREFERENCE_FILE, MODE_PRIVATE).edit();
		editor.putInt(ShowBookActivity.ORIENTATION, orientation);
		editor.commit();
		if (orientation == 0) {
			DebugLog.e(TAG, "CONTENTS: LANDSCAPE");
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (orientation == 1) {
			DebugLog.e(TAG, "CONTENTS: PORTRAIT");

			// for rotation top to bottom
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			if (display.getRotation() == Surface.ROTATION_0) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else if (display.getRotation() == Surface.ROTATION_180) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
			}
		}
		if (isBackGround_) {
			applyAspectRatio();
		}
	}
}
