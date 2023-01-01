package de.szalkowski.activitylauncher.async;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import java.text.NumberFormat;
import java.util.Locale;

import de.szalkowski.activitylauncher.R;
import de.szalkowski.activitylauncher.databinding.ProgressDialogBinding;

public abstract class AsyncProvider<ReturnType> extends AsyncTask<Void, Integer, ReturnType> {
    private final CharSequence message;
    private final Listener<ReturnType> listener;
    private final AlertDialog dialog;
    private final ProgressDialogBinding binding;
    private final NumberFormat progressPercentFormat = NumberFormat.getPercentInstance();
    private int max;

    AsyncProvider(Context context, Listener<ReturnType> listener, boolean showProgressDialog) {
        this.message = context.getText(R.string.dialog_progress_loading);
        this.listener = listener;

        if (showProgressDialog) {
            this.binding = ProgressDialogBinding.inflate(LayoutInflater.from(context));
            this.dialog = new AlertDialog.Builder(context).setView(binding.getRoot()).create();
            this.progressPercentFormat.setMaximumFractionDigits(0);
        } else {
            this.binding = null;
            this.dialog = null;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (this.binding != null && values.length > 0) {
            int value = values[0];

            if (value == 0) {
                this.binding.progress.setIndeterminate(false);
                this.binding.progress.setMax(this.max);
            }

            this.binding.progress.setProgress(value);
            this.binding.progressNumber.setText(
                    String.format(Locale.getDefault(), "%1d/%2d", value, this.max)
            );
            double percent = (double) value / (double) this.max;
            this.binding.progressPercent.setText(this.progressPercentFormat.format(percent));
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (this.dialog != null && this.binding != null) {
            this.dialog.setCancelable(false);
            this.dialog.setTitle(this.message);
            this.binding.progress.setIndeterminate(true);
            this.dialog.show();
        }
    }

    @Override
    protected void onPostExecute(ReturnType result) {
        super.onPostExecute(result);
        if (this.listener != null) {
            this.listener.onProviderFinished(this, result);
        }

        if (this.dialog != null) {
            try {
                this.dialog.dismiss();
            } catch (IllegalArgumentException e) { /* ignore */ }
        }
    }

    abstract protected ReturnType run(Updater updater);

    @Override
    protected ReturnType doInBackground(Void... params) {
        return run(new Updater(this));
    }

    public interface Listener<ReturnType> {
        void onProviderFinished(AsyncProvider<ReturnType> task, ReturnType value);
    }

    public class Updater {
        private final AsyncProvider<ReturnType> provider;

        Updater(AsyncProvider<ReturnType> provider) {
            this.provider = provider;
        }

        public void update(int value) {
            this.provider.publishProgress(value);
        }

        public void updateMax(int value) {
            this.provider.max = value;
        }
    }
}
