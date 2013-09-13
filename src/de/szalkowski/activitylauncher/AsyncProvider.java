package de.szalkowski.activitylauncher;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class AsyncProvider<ReturnType> extends AsyncTask<Void,Integer,ReturnType> {
	public interface Listener<ReturnType> {
		public void onProviderFininshed(AsyncProvider<ReturnType> task, ReturnType value);
	}
	
	public class Updater {
		private AsyncProvider<ReturnType> provider;
		
		public Updater(AsyncProvider<ReturnType> provider) {
			this.provider = provider;
		}
		
		public void update(int value) {
			this.provider.publishProgress(value);
		}

		public void updateMax(int value) {
			this.provider.max = value;
		}
	}
	
	protected Context context;
	protected Listener<ReturnType> listener;
	protected int max;
	protected ProgressDialog progress;

	public AsyncProvider(Context context, Listener<ReturnType> listener, boolean showProgressDialog) {
		this.context = context;
		this.listener = listener;
		
		if(showProgressDialog) {
			this.progress = new ProgressDialog(context);
		} else {
			progress = null;
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if(this.progress != null && values.length > 0) {
			int value = values[0];
			
			if(value == 0) {
				this.progress.setIndeterminate(false);
				this.progress.setMax(this.max);
			}
			
			this.progress.setProgress(value);
		}
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		if (this.progress != null) {
			this.progress.setCancelable(false);
			this.progress.setMessage(context.getText(R.string.dialog_progress_loading));
			this.progress.setIndeterminate(true);
			this.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			this.progress.show();
		}
	}
	
	@Override
	protected void onPostExecute(ReturnType result) {
		super.onPostExecute(result);
		if(this.listener != null) {
			this.listener.onProviderFininshed(this, result);
		}
		
		if (this.progress != null) {
			this.progress.dismiss();
		}
	}
	
	abstract protected ReturnType run(Updater updater);

	@Override
	protected ReturnType doInBackground(Void... params) {
		return run(new Updater(this));
	}
}
