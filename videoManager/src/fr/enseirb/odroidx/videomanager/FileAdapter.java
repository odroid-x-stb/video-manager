package fr.enseirb.odroidx.videomanager;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FileAdapter extends ArrayAdapter<File> {
	private Context context;
	// Organize folder and files (Alphabet + Folder>File)
	private class FileComparator implements Comparator<File> {
		public int compare(File lhs, File rhs) {
			// File or Folder
			if (lhs.isDirectory() && rhs.isFile())
				return -1;
			if (lhs.isFile() && rhs.isDirectory())
				return 1;

			// Alphabetic order
			return lhs.getName().compareToIgnoreCase(rhs.getName());
		}
	}

	public FileAdapter(Context context, int textViewResourceId,
			List<File> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		mInflater = LayoutInflater.from(context);
	}

	private LayoutInflater mInflater = null;

	public View getView(int position, View convertView, ViewGroup parent) {
		TextView vue = null;

		if (convertView != null)
			vue = (TextView) convertView;
		else
			vue = (TextView) mInflater.inflate(
					android.R.layout.simple_list_item_1, null);

		File item = getItem(position);

		// Color
		if (item.isDirectory()) {
			vue.setTextColor(Color.BLUE);
			Drawable myIcon = context.getResources().getDrawable(R.drawable.folder);
			vue.setCompoundDrawablesWithIntrinsicBounds(myIcon, null,
					null, null );
		} else{
			vue.setTextColor(Color.BLACK);
			Drawable myIcon = context.getResources().getDrawable(R.drawable.file);
			vue.setCompoundDrawablesWithIntrinsicBounds(myIcon, null,
					null, null );
		}
		vue.setText(item.getName());
		return vue;
	}

	public void sort() {
		super.sort(new FileComparator());
	}

}
