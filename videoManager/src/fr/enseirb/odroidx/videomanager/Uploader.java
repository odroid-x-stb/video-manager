package fr.enseirb.odroidx.videomanager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

public class Uploader extends Service {
	static int NOTIFY_ID = 1;
	static int PORT = 5088;
	static String SERVER_IP = "192.168.1.8";
	static String SERVLET_UPLOAD = "http://192.168.1.8:8080/dash-manager/upload?name=";
	NotificationManager mNotifyManager = null;
	Builder mBuilder = null;
	private Intent mInvokeIntent;
	private volatile Looper mUploadLooper;
	private volatile ServiceHandler mUploadHandler;
	private HttpClient client = null;

	private int check = 0;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			// get extra datas
			Uri selectedFile = (Uri) msg.obj;
			Log.i(getClass().getSimpleName(), "selectedFile =" + selectedFile);

			// upload the file to the web server
			doHttpUpload(selectedFile);

			Log.i(getClass().getSimpleName(), "Message: " + msg);
			Log.i(getClass().getSimpleName(), "Done with #" + msg.arg1);
			stopSelf(msg.arg1);
		}
	};

	public void onCreate() {
		Log.i(getClass().getSimpleName(), "HttpUploader on create");

		// This is who should be launched if the user selects our persistent
		// notification.
		mInvokeIntent = new Intent();
		mInvokeIntent.setClassName("fr.enseirb.odroidx.videomanager",
				"fr.enseirb.odroidx.videomanager.HttpUploader");

		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.
		HandlerThread thread = new HandlerThread("HttpUploader");
		thread.start();

		mUploadLooper = thread.getLooper();
		mUploadHandler = new ServiceHandler(mUploadLooper);
	}

	public void onStart(Intent uploadintent, int startId) {
		// recup des data pour envoi via msg dans la msgqueue pour traitement
		Message msg = mUploadHandler.obtainMessage();
		msg.arg1 = startId;
		// on place l'uri reçu dans l'intent dans le msg pour le handler
		msg.obj = uploadintent.getData();
		mUploadHandler.sendMessage(msg);
		Log.d(getClass().getSimpleName(), "Sending: " + msg);
		Toast.makeText(Uploader.this, "Upload started",Toast.LENGTH_LONG).show();
	}

	public void doHttpUpload(Uri myFile) {
		createNotification();
		File f = new File(myFile.getPath());
		//int port = SendName(f.getName());;
		Log.d(getClass().getSimpleName(), "Tqsdqsqsqsqesting: ");
		if (f.exists()) {
			Socket s;
			try {
				s = new Socket(InetAddress.getByName(SERVER_IP), 5088);//Bug using variable port
				OutputStream fluxsortie = s.getOutputStream();
				int nb_parts = (int) (f.length() / 4096);

				InputStream in = new BufferedInputStream(new FileInputStream(f));
				ByteArrayOutputStream byte_array = new ByteArrayOutputStream();
				BufferedOutputStream buffer = new BufferedOutputStream(
						byte_array);
				
				byte[] to_write = new byte[4096];
				for (int i = 0; i < nb_parts; i++) {
					in.read(to_write, 0, 4096);
					buffer.write(to_write);
					buffer.flush();
					fluxsortie.write(byte_array.toByteArray());
					byte_array.reset();
					
					//Progress in notification
					mBuilder.setProgress(nb_parts, i, false);
                    mNotifyManager.notify(NOTIFY_ID, mBuilder.build());
				}
				int remaining = (int) (f.length() - nb_parts * 4096);
				in.read(to_write, 0, remaining);
				buffer.write(to_write);
				buffer.flush();
				fluxsortie.write(byte_array.toByteArray());
				byte_array.reset();
				buffer.close();
				fluxsortie.close();
				in.close();
				s.close();
			} catch (UnknownHostException e) {
				Log.i(getClass().getSimpleName(), "Unknown host");
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void onDestroy() {
		mUploadLooper.quit();

		if (check == 0) { // http response contains no error
			Toast.makeText(Uploader.this, R.string.uploadEnd, Toast.LENGTH_SHORT)
			.show();
			mBuilder.setContentText("Download complete")
			// Removes the progress bar
			.setProgress(0, 0, false);
		} else {
			Toast.makeText(Uploader.this, R.string.uploadFailed,
					Toast.LENGTH_SHORT).show();
			mBuilder.setContentText("Download Failed")
			// Removes the progress bar
			.setProgress(0, 0, false);
		}
		mNotifyManager.notify(NOTIFY_ID, mBuilder.build());
		super.onDestroy();
	}

	private final void createNotification() {
		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle("Uploading Movie")
		.setContentText("Upload in progress")
		.setSmallIcon(R.drawable.ic_launcher);
	}
	
	private final int SendName(String fileName){
		int port= PORT;
		try {
		    client = new DefaultHttpClient();  
		    String getURL = SERVLET_UPLOAD.concat(fileName);
		    Log.i(getClass().getSimpleName(), "Message: toto " + getURL);
		    HttpGet get = new HttpGet(getURL);
		    HttpResponse responseGet = client.execute(get);  
		    Log.i(getClass().getSimpleName(), "Message: totqsqsqo " + getURL);

		    HttpEntity resEntityGet = responseGet.getEntity();  
		    /*if (resEntityGet != null) {  
		        // do something with the response
		        String response = EntityUtils.toString(resEntityGet);
		        Log.i("GET RESPONSE", response);
		        port = Integer.parseInt(response);
		    }*/
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return port;
	}

@Override
public IBinder onBind(Intent intent) {
	return null;
}
}