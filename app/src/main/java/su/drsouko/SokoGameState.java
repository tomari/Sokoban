package su.drsouko;

import java.io.Serializable;
import android.content.Context;

public class SokoGameState implements Serializable {
	private static final long serialVersionUID = -2030232080033441582L;
	private int chrx=1,chry=2;
	private int lastChrX=0, lastChrY=0;
	public int steps=0;
	public int roomWidth, roomHeight;
	public float scale=1.f;
	private int viewOffsU=0, viewOffsV=0;
	private int lastViewOffsU, lastViewOffsV;
	public float animProgress=1.f;
	public String stagesTitle;
	public String stagesCopyright;
	public String stageTitle;
	public String stagesFilename;
	public int stage=1;
	public char room[][];
	public boolean editMode=false;
	public static final int MAX_STAGE=256;
	public int loaded_stages=0;
	private int targets;
	private boolean last_parcel=false;
	public SokoGameState(String path) {
		stagesFilename=path;
		steps=0;
	}
	public boolean moveXY(int deltax, int deltay) {
		if(isFinished()) { return false; }
		int dstx=chrx+deltax;
		int dsty=chry+deltay;
		int chrxbuf=chrx;
		int chrybuf=chry;
		char dstChr;
		try {
			dstChr=room[dsty][dstx];
		} catch (ArrayIndexOutOfBoundsException e) {
			dstChr='#';
		}
		boolean res;
		if(dstChr=='.' || dstChr=='x') {
			chrx=dstx;
			chry=dsty;
			steps++;
			last_parcel=false;
			res=true;
		} else if(dstChr=='o' || dstChr=='0') {
			int dst2x=chrx+2*deltax;
			int dst2y=chry+2*deltay;
			char dst2Chr;
			try {
				dst2Chr=room[dst2y][dst2x];
			} catch (ArrayIndexOutOfBoundsException e) {
				dst2Chr='#';
			}
			if(dst2Chr=='.') {
				room[dst2y][dst2x]='o';
			} else if(dst2Chr=='x') {
				room[dst2y][dst2x]='0';
				targets--;
			} else {
				return false;
			}
			if(dstChr=='0') {
				room[dsty][dstx]='x';
				targets++;
			} else {
				room[dsty][dstx]='.';
			}
			chrx=dstx;
			chry=dsty;
			steps++;
			last_parcel=true;
			res=true;
		} else {
			return false;
		}
		lastChrX=chrxbuf;
		lastChrY=chrybuf;
		return res;
	}
	public boolean isFinished() {
		return targets==0;
	}
	public int chrX() { return chrx; }
	public int chrY() { return chry; }
	public void setChrXY(int x, int y) { chrx=x; chry=y; }
	public int lastChrX() { return lastChrX; }
	public int lastChrY() { return lastChrY; }
	public int viewOffsU() { return viewOffsU; }
	public int viewOffsV() { return viewOffsV; }
	public int lastViewOffsU() { return lastViewOffsU; }
	public int lastViewOffsV() { return lastViewOffsV; }
	public boolean isLastMoveParcel() { return last_parcel; }
	public void setViewOffs(int u,int v) {
		lastViewOffsU=viewOffsU;
		lastViewOffsV=viewOffsV;
		viewOffsU=u;
		viewOffsV=v;
	}
	public void setChrXY(int x, int y, boolean setLast) {
		setChrXY(x,y);
		if(setLast) {
			lastChrX=chrx;
			lastChrY=chry;
		}
	}
	public boolean loadStage(Context ctx, int stageno, boolean dry_run) {
		SokoStageLoader loader=new SokoStageLoader(ctx,stagesFilename);
		boolean res=loader.loadStage(stageno);
		loaded_stages=loader.maxLoadedStage();
		if(dry_run) {
			return res;
		}
		if(res) {
			roomWidth=loader.roomWidth();
			roomHeight=loader.roomHeight();
			room=loader.room();
			lastChrX=chrx=loader.chrx();
			lastChrY=chry=loader.chry();
			stagesTitle=loader.stagesTitle();
			stageTitle=loader.stageTitle();
			stagesCopyright=loader.stagesCopyright();
			stage=stageno;
			targets=loader.numTargets();
		}
		return res;
	}
	public boolean extendRoom(int x, int y) {
		int newWidth=roomWidth+Math.abs(x);
		int newHeight=roomHeight+Math.abs(y);
		char[][] newroom;
		try {
			newroom=new char[newHeight][newWidth];
		} catch (OutOfMemoryError e) {
			return false;
		}
		int copyto_xstart,copyto_xend;
		if(x>=0) {
			copyto_xstart=0;
			copyto_xend=roomWidth;
		} else {
			copyto_xstart=-x;
			copyto_xend=newWidth;
		}
		int copyto_ystart,copyto_yend;
		if(y>=0) {
			copyto_ystart=0;
			copyto_yend=roomHeight;
		} else {
			copyto_ystart=-y;
			copyto_yend=newHeight;
		}
		for(int v=0; v<newHeight; v++) {
			for(int u=0; u<newWidth; u++) {
				if(copyto_xstart<=u && u<copyto_xend &&
						copyto_ystart<=v && v<copyto_yend) {
					newroom[v][u]=room[v-copyto_ystart][u-copyto_xstart];
				} else {
					newroom[v][u]=' ';
				}
			}
		}
		room=newroom;
		roomWidth=newWidth;
		roomHeight=newHeight;
		return true;
	}
	public boolean undoLastMove() {
		if(lastChrX==chrx && lastChrY==chry) {
			return false;
		} else {
			if(last_parcel) {
				int deltax=chrx-lastChrX;
				int deltay=chry-lastChrY;
				int parcelx=lastChrX+deltax*2;
				int parcely=lastChrY+deltay*2;
				char cp=room[parcely][parcelx];
				char nc;
				if(cp=='0') {
					targets++;
					nc='x';
				} else {
					nc='.';
				}
				room[parcely][parcelx]=nc;
				char cc=room[chry][chrx];
				char nc2;
				if(cc=='x') {
					nc2='0';
					targets--;
				} else {
					nc2='o';
				}
				room[chry][chrx]=nc2;
			}
			chrx=lastChrX;
			chry=lastChrY;
			steps--;
			return true;
		}
	}
}
