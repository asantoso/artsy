package com.neusou.artsy;

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
			obj.pages = source.readInt();
			obj.perPage = source.readInt();
			obj.total = source.readInt();
			return obj;
		}
	};
	
	
	public int page = 0;
	public int pages = 0;
	public int perPage = 10;
	public int total = 0;
	
	public int getPages() {
		return pages;
	}
	
	public int getTotal() {
		return total;
	}
	
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

	public void reset(){
		this.page = 0;
		this.perPage = 20;
		this.pages = 0;
		this.total = 0;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(page);
		dest.writeInt(pages);
		dest.writeInt(perPage);
		dest.writeInt(total);
	}
	
}