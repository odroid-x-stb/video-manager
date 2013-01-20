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

import java.io.File;
import java.util.ArrayList;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class VideoManager extends Activity {
	private static int CODE_RETOUR = 1;
	private String ip = null;
	private TextView mEmpty = null;
	private GridView mGrid = null;
	private FileAdapter mAdapter = null;
	private File mCurrentFile = null;
	private boolean noParent = false;
	protected Uri videoToUpload = null;
	File fichier;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_manager);
		//Getting Ip from Customized HomeScreen
		Intent home = getIntent();
		if (home.getStringExtra("serverIP") != null) {
			ip = home.getStringExtra("serverIP");
		}

		mGrid = (GridView) findViewById(R.id.gridViewFiles);
		// Test repository mounted (readable) writable ?
		if (!Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			Toast.makeText(this,R.string.nomounted, Toast.LENGTH_SHORT).show();
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
						uploadItem(fichier);
				}
			});
		}

		STBRemoteControlCommunication stbrcc = new STBRemoteControlCommunication(this);
		stbrcc.doBindService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_video_manager, menu);
		if (ip == null) {
			getPreferences();
			Toast.makeText(this, "IP : ".concat(ip), Toast.LENGTH_SHORT).show();
		}
		return true;
	}

	//Option to set server IP
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_ip) {
			startActivityForResult(new Intent(this, Parameters.class),
					CODE_RETOUR);
		}
		else if (item.getItemId() == R.id.quit){
			finish();
			return true;    
		}
		return super.onOptionsItemSelected(item);
	}

	private void getPreferences() {

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		ip = preferences.getString("ip", "");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CODE_RETOUR) {
			getPreferences();
			Toast.makeText(this, "Nouvelle IP : ".concat(ip), Toast.LENGTH_SHORT)
			.show();
		}
		super.onActivityResult(requestCode, resultCode, data);
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

	//Launch service to upload the file
	private void uploadItem(File pFile) {
		videoToUpload = Uri.fromFile(pFile);
		Intent uploadIntent = new Intent();
		uploadIntent.putExtra("IP", ip);
		uploadIntent.setClassName("fr.enseirb.odroidx.videomanager",
				"fr.enseirb.odroidx.videomanager.Uploader");
		uploadIntent.setData(videoToUpload);
		startService(uploadIntent);
	}
}
