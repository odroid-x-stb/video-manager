package fr.enseirb.odroidx.videomanager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class HttpUploader extends Service {

	private Intent mInvokeIntent;
	private volatile Looper mUploadLooper;
	private volatile ServiceHandler mUploadHandler;

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

	}

	public void doHttpUpload(Uri myFile) {
		File f = new File(myFile.getPath()); 
		if(f.exists()) 
		{ 
			Socket s;
			try {
				s = new Socket(InetAddress.getByName("192.168.1.8"),5088);
				OutputStream fluxsortie = s.getOutputStream(); 
				long nb_parts= f.length() / 4096; 

				InputStream in = new BufferedInputStream(new FileInputStream(f)); 
				ByteArrayOutputStream byte_array = new ByteArrayOutputStream(); 
				BufferedOutputStream buffer = new BufferedOutputStream(byte_array); 
				
				byte[] to_write = new byte[4096];
				for (int i = 0; i < nb_parts; i++) {
					in.read(to_write, 0,4096 );
					buffer.write(to_write);
					buffer.flush();
					fluxsortie.write(byte_array.toByteArray()); 
					byte_array.reset();
				}
				int remaining = (int) (f.length() - nb_parts*4096);
				in.read(to_write, 0, remaining);
				buffer.write(to_write);
				buffer.flush();
				fluxsortie.write(byte_array.toByteArray()); 
				byte_array.reset();
				//in.read(to_write, nb_parts * 4096, f.length());
				buffer.close();
				fluxsortie.close();
				in.close(); 
				s.close(); 
			}
			catch (UnknownHostException e) {
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
			Toast.makeText(HttpUploader.this, "envoyée", Toast.LENGTH_SHORT)
			.show();
		} else {
			Toast.makeText(HttpUploader.this, "échec d'envoi",
					Toast.LENGTH_SHORT).show();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}