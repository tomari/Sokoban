package su.drsouko;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class SokoView extends View {
	private SokoGameState gameState=null;
	private Drawable kabe,nimotsu,target,yuka;
	private Drawable pl_r1, pl_r2, pl_l1, pl_l2, pl_u1, pl_u2;
	private int chrDimX=0, chrDimY=0;
	private ScaleGestureDetector scaleDetector;
	private GestureDetector gestureDetector;
	private SokoTouchListener touchListener=null;
	private boolean scrollEnabled=true;
	public SokoView(Context context) {
		super(context);
		init(context);
	}
	public SokoView(Context context, AttributeSet attrs) {
		super(context,attrs);
		init(context);
	}
	public SokoView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	public void setGameState(SokoGameState gamestate) {
		this.gameState=gamestate;
	}
	private void init(Context ctx) {
		Resources r=getResources();
		kabe=r.getDrawable(R.drawable.kabe);
		nimotsu=r.getDrawable(R.drawable.nimotsu);
		target=r.getDrawable(R.drawable.target);
		yuka=r.getDrawable(R.drawable.yuka);
		pl_r1=r.getDrawable(R.drawable.player_right1);
		pl_r2=r.getDrawable(R.drawable.player_right2);
		pl_l1=r.getDrawable(R.drawable.player_left1);
		pl_l2=r.getDrawable(R.drawable.player_left2);
		pl_u1=r.getDrawable(R.drawable.player_up1);
		pl_u2=r.getDrawable(R.drawable.player_up2);
		chrDimX=kabe.getIntrinsicWidth();
		chrDimY=kabe.getIntrinsicHeight();
		scaleDetector=new ScaleGestureDetector(ctx,new ScaleListener());
		gestureDetector=new GestureDetector(ctx,new GestureListener(this));
		setFocusable(true);
	}
	public int getChrDimX() {
		return chrDimX;
	}
	public int getChrDimY() {
		return chrDimY;
	}
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int chrDimX=getChrDimX();
		int chrDimY=getChrDimY();
		if(gameState!=null && gameState.room!=null) {
			chrDimX=(int) (chrDimX*gameState.scale);
			chrDimY=(int) (chrDimY*gameState.scale);
			char[][] stage=gameState.room;
			float foffsX=gameState.viewOffsU(), foffsY=gameState.viewOffsV();
			float lastOffsX=gameState.lastViewOffsU(), lastOffsY=gameState.lastViewOffsV();
			float intp=gameState.animProgress;
			int offsX=(int) (lastOffsX+intp*(foffsX-lastOffsX));
			int offsY=(int) (lastOffsY+intp*(foffsY-lastOffsY));
			// clip
			int xstart=Math.max(0, -offsX/chrDimX);
			int xend=Math.min(stage[0].length, xstart+1+(int)Math.ceil((float)getWidth()/(float)chrDimX));
			int ystart=Math.max(0, -offsY/chrDimY);
			int yend=Math.min(stage.length, ystart+1+(int)Math.ceil((float)getHeight()/(float)chrDimY));
			for(int y=ystart; y<yend; y++) {
				for(int x=xstart; x<xend; x++) {
					char c=stage[y][x];
					Drawable chr[]={null,null};
					if(c=='#') {
						chr[0]=kabe;
					} else if(c=='.') {
						chr[0]=yuka;
					} else if(c=='x') {
						chr[0]=target;
					} else if(c=='o') {
						chr[0]=yuka;
						chr[1]=nimotsu;
					} else if(c=='0') {
						chr[0]=target;
						chr[1]=nimotsu;
					}
					int u=offsX+chrDimX*x;
					int v=offsY+chrDimY*y;
					int u1,v1;
					if(x==gameState.chrX() && y==gameState.chrY()) {
						chr[1]=selectPlayerDrawable();
						float lastx=gameState.lastChrX();
						float lasty=gameState.lastChrY();
						u1=offsX+(int) (((float)chrDimX)*(lastx+intp*((float)x-lastx)));
						v1=offsY+(int) (((float)chrDimY)*(lasty+intp*((float)y-lasty)));
					} else {
						u1=u;
						v1=v;
					}
					if(chr[0]!=null) {
						chr[0].setBounds(u,v,u+chrDimX,v+chrDimY);
						chr[0].draw(canvas);
					}
					if(chr[1]!=null) {
						chr[1].setBounds(u1,v1,u1+chrDimX,v1+chrDimY);
						chr[1].draw(canvas);
					}
				}
			}
		} else { // dummy
			kabe.setBounds(chrDimX,chrDimY,2*chrDimX,2*chrDimY);
			kabe.draw(canvas);
		}
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean scaleP=scaleDetector.onTouchEvent(event);
		boolean gestureP=gestureDetector.onTouchEvent(event);
		if(!scrollEnabled) {
			if(touchListener!=null && event.getAction()==MotionEvent.ACTION_MOVE) {
				float x=event.getX();
				float y=event.getY();
				calcCoordXY(x,y);
				touchListener.onSokoTouch(this,coordX,coordY);
				return true;
			}
		}
		return scaleP || gestureP;
	}
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		private float centerx,centery;
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			centerx=detector.getFocusX();
			centery=detector.getFocusY();
			return true;
		}
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float scalef=detector.getScaleFactor();
			float scale=scalef*gameState.scale;
			gameState.scale=Math.max(0.2f, Math.min(scale, 1.f));
			if(0.2f<scale && scale<1.f) {
				float zoomy=scalef*centery;
				float offsoffsy=zoomy-centery;
				float zoomx=scalef*centerx;
				float offsoffsx=zoomx-centerx;
				int newoffsU=(int) (scalef*gameState.viewOffsU()-offsoffsx);
				int newoffsV=(int) (scalef*gameState.viewOffsV()-offsoffsy);
				gameState.setViewOffs(newoffsU, newoffsV);
			}
			invalidate();
			return true;
		}
	}
	public interface SokoTouchListener {
		public boolean onSokoTouch(SokoView v, int x, int y);
		public boolean onSokoDoubleTouch(SokoView v, int x, int y);
	}
	public void setSokoTouchListener(SokoTouchListener theListener) {
		touchListener=theListener;
	}
	public SokoTouchListener sokoTouchListener() {
		return touchListener;
	}
	private int coordX, coordY;
	private boolean calcCoordXY(float clkx, float clky) {
		float x=clkx-gameState.viewOffsU();
		float y=clky-gameState.viewOffsV();
		float chrDimU=gameState.scale*getChrDimX();
		float chrDimV=gameState.scale*getChrDimY();
		coordX=(int)Math.floor(x/chrDimU);
		coordY=(int)Math.floor(y/chrDimV);
		return (0<=coordX && coordX<gameState.roomWidth && 
				0<=coordY && coordY<gameState.roomHeight);
	}
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		private SokoView view;
		public GestureListener(SokoView theView) {
			view=theView;
		}
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if(touchListener!=null && (calcCoordXY(e.getX(),e.getY()) || gameState.editMode)) {
				return touchListener.onSokoTouch(view, coordX, coordY);
			}
			return false;
		}
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if(touchListener!=null && (gameState.editMode || calcCoordXY(e.getX(),e.getY()))) {
				return touchListener.onSokoDoubleTouch(view,coordX,coordY);
			}
			return false;
		}
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if(scrollEnabled) {
				int offsX=gameState.viewOffsU();
				int offsY=gameState.viewOffsV();
				int offsX2=offsX-(int) distanceX;
				int offsY2=offsY-(int) distanceY;
				int newOffsU=Math.max(minOffsU(),Math.min(maxOffsU(), offsX2));
				int newOffsV=Math.max(minOffsV(), Math.min(maxOffsV(),offsY2));
				gameState.setViewOffs(newOffsU, newOffsV);
				invalidate();
				return true;
			} else {
				return false;
			}
		}
	}
	public int minOffsU() {
		int roomDimU=(int)(gameState.roomWidth*chrDimX*gameState.scale);
		int chrDimU=(int) (chrDimX*gameState.scale);
		return -roomDimU+chrDimU/2;
	}
	public int maxOffsU() {
		int vw=getWidth();
		int chrDimU=(int) (chrDimX*gameState.scale);
		return vw-chrDimU/2;
	}
	public int minOffsV() {
		int roomDimV=(int)(gameState.roomHeight*chrDimY*gameState.scale);
		int chrDimV=(int) (chrDimX*gameState.scale);
		return -roomDimV+chrDimV/2;
	}
	public int maxOffsV() {
		int vh=getHeight();
		int chrDimV=(int) (chrDimX*gameState.scale);
		return vh-chrDimV/2;
	}
	public boolean isScrollEnabled() { return scrollEnabled; }
	public void setScrollEnable(boolean enableScroll) { scrollEnabled=enableScroll; }
	public int numHorizontalChrs() {
		float chrDimU=chrDimX*gameState.scale;
		return (int) Math.ceil(getWidth()/chrDimU);
	}
	public int numVerticalChrs() {
		float chrDimV=chrDimY*gameState.scale;
		return (int) Math.ceil(getHeight()/chrDimV);
	}
	private Drawable selectPlayerDrawable() {
		float intp=gameState.animProgress;
		Drawable res;
		if(gameState.lastChrY()!=gameState.chrY()) {
			boolean half=intp<0.5f;
			boolean parity=((gameState.chrY()&1)>0)^half;
			res=parity?pl_u1:pl_u2;
		} else {
			boolean half=intp<0.5f;
			boolean parity=((gameState.chrX()&1)>0)^half;
			if(gameState.lastChrX()>gameState.chrX()) {
				res=parity?pl_l2:pl_l1;
			} else {
				res=parity?pl_r2:pl_r1;
			}
		}
		return res;
	}
}
