package fr.enseirb.odroidx.videomanager;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class VideoManager extends Activity {
	private TextView mEmpty = null;
	private GridView mGrid = null;
	private FileAdapter mAdapter = null;
	private File mCurrentFile = null;
	private boolean noParent = false;
	protected Uri videoToUpload=null;
	File fichier;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_manager);

		mGrid = (GridView) findViewById(R.id.gridViewFiles);
		// Test repository mounted (readable) writable ?
		if (!Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			mEmpty = (TextView) mGrid.getEmptyView();
			mEmpty.setText(R.string.nomounted);
		} else {
			mCurrentFile = Environment.getExternalStorageDirectory();
			setTitle(mCurrentFile.getAbsolutePath());
			File[] fichiers = mCurrentFile.listFiles();

			ArrayList<File> liste = new ArrayList<File>();
			for (File f : fichiers)
				liste.add(f);

			mAdapter = new FileAdapter(this,
					android.R.layout.simple_list_item_1, liste);
			mGrid.setAdapter(mAdapter);
			mAdapter.sort();

			// Listener open file/folder
			mGrid.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> adapter, View view,
						int position, long id) {
					File fichier = mAdapter.getItem(position);
					if (fichier.isDirectory())
						updateDirectory(fichier);
					else
						seeItem(fichier);
				}
			});
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_video_manager, menu);
		return true;
	}

	public void setEmpty() {
		if (!mAdapter.isEmpty())
			mAdapter.clear();
	}

	public void updateDirectory(File pFile) {
		setTitle(pFile.getAbsolutePath());
		noParent = false;
		mCurrentFile = pFile;
		setEmpty();
		File[] fichiers = mCurrentFile.listFiles();
		if (fichiers != null) {
			for (File f : fichiers)
				mAdapter.add(f);
			mAdapter.sort();
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Back to parent folder
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			File parent = mCurrentFile.getParentFile();
			if (parent != null)
				updateDirectory(parent);
			else {
				// No more parent, clic twice to leave
				if (noParent != true) {
					Toast.makeText(this, R.string.noparent, Toast.LENGTH_SHORT)
					.show();
					noParent = true;
				} else
					finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void seeItem(File pFile) {
		videoToUpload = Uri.fromFile(pFile);
		Intent uploadIntent = new Intent( );
		uploadIntent.setClassName("fr.enseirb.odroidx.videomanager", "fr.enseirb.odroidx.videomanager.HttpUploader");
		uploadIntent.setData(videoToUpload);
		startService(uploadIntent);
	}
}
