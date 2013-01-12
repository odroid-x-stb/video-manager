package fr.enseirb.odroidx.videomanager;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;

public class STBRemoteControlCommunication {
	
	private static final String TAG = "STBRemoteControlCommunication"; 
	
	Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

	private static final int CMD__VIDEO_PLAY = 10;
	private static final int CMD__VIDEO_PAUSE = 11;
	private static final int CMD__VIDEO_PREVIOUS = 12;
	private static final int CMD__VIDEO_REWIND = 13;
	private static final int CMD__VIDEO_FORWARD = 14;
	private static final int CMD__VIDEO_NEXT = 15;
	private static final int CMD__MOVE_UP = 16;
	private static final int CMD__MOVE_DOWN = 17;
	private static final int CMD__MOVE_LEFT = 18;
	private static final int CMD__MOVE_RIGHT = 19;
	private static final int CMD__SELECT = 20;
	private static final int CMD__HOME = 21;
	private static final int CMD__BACK = 22;
    
	private static final int MSG__REGISTER_CLIENT = 1;
	private static final int MSG__UNREGISTER_CLIENT = 2;
	
	private Activity act;
	
	public STBRemoteControlCommunication (Activity a) {
		act = a;
	}
	
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	
        	switch (msg.what) {
	            case CMD__VIDEO_PLAY:
	            	press_key(KeyEvent.KEYCODE_MEDIA_PLAY);
	            	break;
	            case CMD__VIDEO_PAUSE:
	            	press_key(KeyEvent.KEYCODE_MEDIA_PAUSE);
	            	break;
	            case CMD__VIDEO_PREVIOUS:
	            	press_key(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
	            	break;
	            case CMD__VIDEO_REWIND:
	            	press_key(KeyEvent.KEYCODE_MEDIA_REWIND);
	            	break;
	            case CMD__VIDEO_FORWARD:
	            	press_key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
	            	break;
	            case CMD__VIDEO_NEXT:
	            	press_key(KeyEvent.KEYCODE_MEDIA_NEXT);
	            	break;
	            case CMD__MOVE_UP:
	            	press_key(KeyEvent.KEYCODE_DPAD_UP);
	            	break;
	            case CMD__MOVE_DOWN:
	            	press_key(KeyEvent.KEYCODE_DPAD_DOWN);
	            	break;
	            case CMD__MOVE_LEFT:
	            	press_key(KeyEvent.KEYCODE_DPAD_LEFT);
	            	break;
	            case CMD__MOVE_RIGHT:
	            	press_key(KeyEvent.KEYCODE_DPAD_RIGHT);
	            	break;
	            case CMD__SELECT:
	            	press_key(KeyEvent.KEYCODE_DPAD_CENTER);
	            	break;
	            case CMD__BACK:
	            	press_key(KeyEvent.KEYCODE_BACK);
	            	break;
	            case CMD__HOME:
	            	press_key(KeyEvent.KEYCODE_HOME);
	            	break;
	            default:
	                super.handleMessage(msg);
            }
        }
    }
    
    private void press_key (final int keyCmd) {
    	Thread myThread = new Thread() {
            public void run() {
    	    	try {
    	    		Instrumentation inst = new Instrumentation();
    		        inst.sendKeyDownUpSync(keyCmd);
    		        Log.v(TAG, "key "+keyCmd+"pressed");
    	    	} catch (Exception e) {
    	    		Log.e(TAG, "error while simulating key event :\n", e);
    	    	}
    	    }
    	};
    	myThread.start();
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, MSG__REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            	Log.e(TAG, "error :\n", e);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    public void doBindService() {
        act.bindService(new Intent("RemoteControlService.intent.action.Launch"), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    public void doUnbindService() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MSG__UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    Log.e(TAG, "error:\n", e);
                }
            }
            act.unbindService(mConnection);
            mIsBound = false;
        }
    }
}