package com.neusou.vivacious;

import android.os.Parcel;
import android.os.Parcelable;

public class Paging implements Parcelable{

	public static final Parcelable.Creator<Paging> CREATOR = new Creator<Paging>() {
		
		@Override
		public Paging[] newArray(int size) {
			return null;
		}
		
		@Override
		public Paging createFromParcel(Parcel source) {
			Paging obj = new Paging();
			obj.page = source.readInt();
			obj.perPage = source.readInt();
			return obj;
		}
	};
	
	
	int page = 0;
	int perPage = 20;
	
	public int getPerPage() {
		return perPage;
	}

	public void setPerPage(int perPage) {
		this.perPage = perPage;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPage(){
		return page;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(page);
		dest.writeInt(perPage);
	}
	
}