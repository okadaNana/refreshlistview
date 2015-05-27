package com.owen.refreshlistview.adapter;

import java.util.List;

import com.owen.refreshlistview.R;
import com.owen.refreshlistview.bean.ApkEntity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {
	
	private List<ApkEntity> apk_list = null;
	private LayoutInflater layoutInflater = null;
	
	public MyAdapter(Context context, List<ApkEntity> apk_list) {
		this.apk_list = apk_list;
		layoutInflater = LayoutInflater.from(context);
	}
	
	public void onDataChange(List<ApkEntity> apk_list) {
		this.apk_list = apk_list;
		this.notifyDataSetChanged();
	}
	
	
	@Override
	public int getCount() {
		return apk_list.size();
	}

	@Override
	public Object getItem(int position) {
		return apk_list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ApkEntity entity = apk_list.get(position);
		ViewHolder viewHolder = null;
		
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = layoutInflater.inflate(R.layout.item_layout, null);
			
			viewHolder.tvName = (TextView) convertView.findViewById(R.id.item3_apkname);
			viewHolder.tvDesc = (TextView) convertView.findViewById(R.id.item3_apkdes);
			viewHolder.tvInfo = (TextView) convertView.findViewById(R.id.item3_apkinfo);
			
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		viewHolder.tvName.setText(entity.getName());
		viewHolder.tvDesc.setText(entity.getDes());
		viewHolder.tvInfo.setText(entity.getInfo());
		
		return convertView;
	}
	
	private static class ViewHolder {
		TextView tvName = null;
		TextView tvDesc = null;
		TextView tvInfo = null;
	}

}
