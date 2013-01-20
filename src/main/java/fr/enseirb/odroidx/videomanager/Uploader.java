/**
 * Copyright (C) 2012 Philippe Donon <pdonon@enseirb-matmeca.fr>
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.enseirb.odroidx.videomanager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class Uploader extends Service {
	private static final int CONNECTION_ERROR = 1;
	private static final int UNKNOWN = 2;
	private static final int HTTP_SERVER = 3;
	static int NOTIFY_ID = 1;
	static int PART_SIZE = 6024;
	static int PORT = 5088;
	static String SERVLET_UPLOAD = ":8080/dash-manager/upload?name=";
	private String server_ip = null;
	NotificationManager mNotifyManager = null;
	Notification.Builder mBuilder = null;
	private volatile Looper mUploadLooper;
	private volatile ServiceHandler mUploadHandler;
	private HttpClient client = null;

	private int STATUS = 0;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			// get extra datas
			Uri selectedFile = (Uri) msg.obj;

			// upload the file to the web server
			doUpload(selectedFile);
			Log.e(getClass().getSimpleName(), "" + STATUS);
			Log.i(getClass().getSimpleName(), "Message: " + msg);
			Log.i(getClass().getSimpleName(), "Done with #" + msg.arg1);
			stopSelf(msg.arg1);
		}
	};

	public void onCreate() {
		Log.i(getClass().getSimpleName(), "Uploader on create");
		HandlerThread thread = new HandlerThread("Uploader");
		thread.start();

		mUploadLooper = thread.getLooper();
		mUploadHandler = new ServiceHandler(mUploadLooper);
	}

	public void onStart(Intent uploadintent, int startId) {
		//Getting data to send to the message queue
		Message msg = mUploadHandler.obtainMessage();
		msg.arg1 = startId;
		//Putting IRU in message for handler
		msg.obj = uploadintent.getData();
		server_ip = uploadintent.getStringExtra("IP");
		Log.d("uploader", "server ip : " + server_ip);
		if (server_ip == null) {
			Log.e(getClass().getSimpleName(), "IP null");
			Toast.makeText(Uploader.this, R.string.menu_ip, Toast.LENGTH_SHORT)
					.show();
		} else {
			mUploadHandler.sendMessage(msg);
			Log.d(getClass().getSimpleName(), "Sending: " + msg);
		}
	}

	public void doUpload(Uri myFile) {
		createNotification();
		File f = new File(myFile.getPath());
		SendName(f.getName().replace(' ', '-'));
		Log.e(getClass().getSimpleName(), "test: " + f.exists());
		if (f.exists()) {
			Socket s;
			try {
				Log.e(getClass().getSimpleName(), "test: " + server_ip);
				s = new Socket(InetAddress.getByName(server_ip), 5088);// Bug
				// using
				// variable
				// port
				OutputStream fluxsortie = s.getOutputStream();
				int nb_parts = (int) (f.length() / PART_SIZE);

				InputStream in = new BufferedInputStream(new FileInputStream(f));
				ByteArrayOutputStream byte_array = new ByteArrayOutputStream();
				BufferedOutputStream buffer = new BufferedOutputStream(
						byte_array);

				byte[] to_write = new byte[PART_SIZE];
				for (int i = 0; i < nb_parts; i++) {
					in.read(to_write, 0, PART_SIZE);
					buffer.write(to_write);
					buffer.flush();
					fluxsortie.write(byte_array.toByteArray());
					byte_array.reset();
					if ((i % 250) == 0) {
						mBuilder.setProgress(nb_parts, i, false);
						mNotifyManager.notify(NOTIFY_ID, mBuilder.getNotification());
					}
				}
				int remaining = (int) (f.length() - nb_parts * PART_SIZE);
				in.read(to_write, 0, remaining);
				buffer.write(to_write);
				buffer.flush();
				fluxsortie.write(byte_array.toByteArray());
				byte_array.reset();
				buffer.close();
				fluxsortie.close();
				in.close();
				s.close();
			} catch (ConnectException e) {
				if (STATUS != HTTP_SERVER)
					STATUS = CONNECTION_ERROR;
				e.printStackTrace();
			} catch (UnknownHostException e) {
				if (STATUS != HTTP_SERVER)
					STATUS = UNKNOWN;
				Log.i(getClass().getSimpleName(), "Unknown host");
				e.printStackTrace();
			} catch (IOException e) {
				if (STATUS != HTTP_SERVER)
					STATUS = CONNECTION_ERROR;
				e.printStackTrace();
			}
		}
	}

	public void onDestroy() {
		mUploadLooper.quit();
		Log.e(getClass().getSimpleName(), "destroy" + STATUS);
		switch (STATUS) {
		case CONNECTION_ERROR:
			Toast.makeText(getApplicationContext(), R.string.connectionFailed,
					Toast.LENGTH_SHORT).show();
			break;
		case UNKNOWN:
			Toast.makeText(Uploader.this, R.string.unknown, Toast.LENGTH_SHORT)
					.show();
			break;
		case HTTP_SERVER:
			Toast.makeText(Uploader.this, R.string.serverDead,
					Toast.LENGTH_SHORT).show();
			break;
		default:
			Toast.makeText(Uploader.this, R.string.uploadEnd,
					Toast.LENGTH_SHORT).show();
			mBuilder.setContentText("Download complete").setProgress(0, 0, false);
		}
		mNotifyManager.notify(NOTIFY_ID, mBuilder.getNotification());
		super.onDestroy();
	}

	private final void createNotification() {
		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new Notification.Builder(this);
		mBuilder.setContentTitle("Uploading Movie")
				.setContentText("Upload in progress")
				.setSmallIcon(R.drawable.ic_launcher);
	}

	//Send file name to the servlet
	private final int SendName(String fileName) {
		int port = PORT;
		try {
			client = new DefaultHttpClient();
			String getURL = "http://".concat(server_ip).concat(SERVLET_UPLOAD)
					.concat(fileName);
			HttpGet get = new HttpGet(getURL);
			HttpResponse responseGet = client.execute(get);

			HttpEntity resEntityGet = responseGet.getEntity();
			/*
			 * if (resEntityGet != null) { // do something with the response
			 * String response = EntityUtils.toString(resEntityGet);
			 * Log.i("GET RESPONSE", response); port =
			 * Integer.parseInt(response); }
			 */
		} catch (HttpHostConnectException e) {
			STATUS = HTTP_SERVER;
			e.printStackTrace();

		} catch (Exception e) {
			STATUS = HTTP_SERVER;
			e.printStackTrace();
		}
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return port;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
