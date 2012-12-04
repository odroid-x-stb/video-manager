package fr.enseirb.odroidx.videomanager;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class VideoManager extends Activity {

	// Text if folder empty
	private TextView mEmpty = null;
	// ListView of files
	private ListView mList = null;
	private FileAdapter mAdapter = null;
	private File mCurrentFile = null;
	private boolean noParent = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_manager);
		
		mList = (ListView) findViewById(R.id.listViewFiles);
		// Test repository mounted (readable) writable ?
		if (!Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			mEmpty = (TextView) mList.getEmptyView();
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
			mList.setAdapter(mAdapter);
			mAdapter.sort();

			// Listener open file/folder
			mList.setOnItemClickListener(new OnItemClickListener() {
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
		// Inflate the menu; this adds items to the action bar if it is present.
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
		String extension = pFile.getName()
				.substring(pFile.getName().indexOf(".") + 1).toLowerCase();
		if (extension.equals("avi")) {
			// upload(Uri.fromFile(pFile), "video/avi");
		}
	}

}
