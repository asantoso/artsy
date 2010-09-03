package com.neusou.artsy;

import com.neusou.vivacious.IRemoteService;
import com.neusou.vivacious.IRemoteServiceCallback;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;

public class FlickrRemoteService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {		
		return mRemoteServiceBinder;
	}
	
	IRemoteService.Stub mRemoteServiceBinder = new RemoteServiceImpl();
	IRemoteServiceCallback.Stub mRemoteServiceCallbackBinder = new RemoteServiceCallbackImpl();
	
	public static final String WAKE_UP = "com.neusou.vivacious.WAKE_UP";

	class RemoteServiceCallbackImpl extends IRemoteServiceCallback.Stub {
		
		@Override
		public void valueChanged(int value) throws RemoteException {
			
			
		}
		
		
	}
	
	class RemoteServiceImpl extends IRemoteService.Stub {
		public static final String LOG_TAG = "RemoteServiceImpl";

		@Override
		public void registerCallback(IRemoteServiceCallback cb)
				throws RemoteException {
			
		}

		@Override
		public void unregisterCallback(IRemoteServiceCallback cb)
				throws RemoteException {
			
		}

		@Override
		public int processData(int data) throws RemoteException {
			Logger.l(Logger.DEBUG, LOG_TAG, "processData: "+data);
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					int i = 0;
					SystemClock.sleep(5000);
					Intent wakeup = new Intent(WAKE_UP);
					Logger.l(Logger.DEBUG, LOG_TAG, "sending intent: "+WAKE_UP);
					sendBroadcast(wakeup);
				}
				
			}).start();
			
			return 888;
		}
		

		
	}
}