package chan.eddie.wifipositioning;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.SparseArray;
import android.view.MotionEvent;

public class FloorPlanView extends TouchImageView {
	
	// class for survey point
	public class PointObj {
		public int id;
		public int state;
		public float pX, pY;
		public RectF dim = new RectF();
		public Bitmap curImg;
		ArrayList<Bitmap> imgStateArr = new ArrayList<Bitmap>();

		public PointObj(int pid, float posX, float posY, int pstate, Bitmap imgState, Bitmap imgState1, Bitmap imgState2) {
			id = pid;
			state = pstate;
			pX = posX;
			pY = posY;
			dim.left = posX - imgState.getWidth()/2;
			dim.top = posY - imgState.getHeight()/2;
			dim.right = posX + imgState.getWidth()/2;
			dim.bottom = posY + imgState.getHeight()/2;
			curImg = imgState;				
			imgStateArr.add(imgState); // 0
			imgStateArr.add(imgState1); // 1
			imgStateArr.add(imgState2); // 2
			if (pstate >= 0 && pstate <= 2)
				curImg = imgStateArr.get(pstate);				
		}
	}
	
	// class for user track point
	public static class TrackObj {
		public static final int TRACK_WIDTH = 10;
		
		public static Bitmap img;
		public static Bitmap imgAdj;
		public static float pAdjX, pAdjY, halfAngle;
		public static RectF dimAdj = new RectF();
		
		public float pX, pY, rad, dir;
		public RectF dim = new RectF();
		
		public TrackObj(float posX, float posY, float radius, float angle) {
			pX = posX;
			pY = posY;
			rad = radius;
			dir = angle;
			pAdjX = posX;
			pAdjY = posX;
			if (img != null) {
				dim.left = posX - img.getWidth()/2;
				dim.top = posY - img.getHeight()/2;
				dim.right = posX + img.getWidth()/2;
				dim.bottom = posY + img.getHeight()/2;
			}
		}
		
		public TrackObj(float posX, float posY, float posAdjX, float posAdjY, float radius, float angle) {
			pX = posX;
			pY = posY;
			rad = radius;
			dir = angle;
			pAdjX = posAdjX;
			pAdjY = posAdjY;
			if (img != null) {
				dim.left = posX - img.getWidth()/2;
				dim.top = posY - img.getHeight()/2;
				dim.right = posX + img.getWidth()/2;
				dim.bottom = posY + img.getHeight()/2;
			}
			if (imgAdj != null) {
				dimAdj.left = posAdjX - imgAdj.getWidth()/2;
				dimAdj.top = posAdjY - imgAdj.getHeight()/2;
				dimAdj.right = posAdjX + imgAdj.getWidth()/2;
				dimAdj.bottom = posAdjY + imgAdj.getHeight()/2;
			}
		}

		public static float getTrackWidth() {
			if (img != null)
				return img.getWidth();
			return TRACK_WIDTH;
		}
		
		public static float getAdjTrackWidth() {
			if (imgAdj != null)
				return imgAdj.getWidth();
			return TRACK_WIDTH;
		}
	}
	
	private OnFloorPlanClickListener mCallback;

    // Container Activity must implement this interface
    public interface OnFloorPlanClickListener {
    	public void OnFloorPlanClick(int pointId, float posX, float posY);
    }
    
	public static final int GRID_WIDTH_INIT = 200;
	public static final int GRID_UNIT_INIT = 3;
	public static final int TRACK_SIZE = 5;
	public static final float TRACK_HALF_ANGEL = 60;
	public static final float TRACK_ARC_RADIUS = 100;
	
	public static final int kStrOffsetX = 3;
	public static final int kStrOffsetY = 10;
	
	protected Paint gridPaint, textPaint, trackPaint, trackAdjPaint, arcPaint, circlePaint;
	protected float[] m;
	protected int gridWidth, gridUnit, trackSize;
	protected float scaledGridWidth = 0, matrixWidth = 0, initScale = 0, trackRadius = TRACK_ARC_RADIUS, curDir;
	protected float imageScale = 0, gridOffsetX = 0, gridOffsetY = 0;
	protected boolean lockGridScale = false;
	protected boolean showGrid = true;
	protected boolean showPoint = true;
	protected boolean showTrack = false;
	protected boolean showTrustRegion = false;
	protected boolean showOrientationFilter = false;
	protected boolean showRotate = false;
	protected String gridUnitStr;
	protected SparseArray<PointObj> mapPoint;
	protected ArrayList<TrackObj> listTrack;
	protected RectF arcOval = new RectF();

	public FloorPlanView(Context context) {
		super(context);
		init();
	}
	
	public FloorPlanView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public void init() {
		TrackObj.img = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.bluedot)).getBitmap();
		TrackObj.imgAdj = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.orangedot)).getBitmap();
		TrackObj.halfAngle = TRACK_HALF_ANGEL;

		gridPaint = new Paint();
		gridPaint.setStyle(Style.STROKE);
		gridPaint.setColor(Color.BLUE); // blue color, no alpha
		gridPaint.setPathEffect(new DashPathEffect(new float[] {5,5},0));
		
		textPaint = new Paint();
		textPaint.setColor(Color.BLUE); // blue color, no alpha
		textPaint.setTextSize(10);
		
		trackPaint = new Paint();
		trackPaint.setStyle(Style.STROKE);
		trackPaint.setColor(0x508080ff); // purple color, alpha 0.3
		trackPaint.setStrokeWidth(TrackObj.getTrackWidth());
		
		circlePaint = new Paint();
		circlePaint.setStyle(Style.FILL);
		circlePaint.setColor(0x508080ff); // purple color, alpha 0.5
		
		trackAdjPaint = new Paint();
		trackAdjPaint.setStyle(Style.STROKE);
		trackAdjPaint.setColor(0x80ff8080); // orange color, alpha 0.5
		trackAdjPaint.setStrokeWidth(TrackObj.getAdjTrackWidth());
		
		arcPaint = new Paint();
		arcPaint.setStyle(Style.FILL);
		arcPaint.setColor(0x50ffff00); // yellow color, alpha 0.3

		m = new float[9];
		mapPoint = new SparseArray<PointObj>();
		listTrack = new ArrayList<TrackObj>();
		
		gridWidth = GRID_WIDTH_INIT;
		gridUnit = GRID_UNIT_INIT;
		trackSize = TRACK_SIZE;
	}
	
	public void setGrid(int width, int unit) {
		gridWidth = width;
		gridUnit = unit;
		
		// assign new matrixGridWidth, for lockGridScale only
		matrixWidth = gridWidth * initScale;
	}
	
	public void setLockGrid(boolean lock) {
		lockGridScale = lock;
	}
	
	public void setOnFloorPlanClickListener(OnFloorPlanClickListener listener) {
		mCallback = listener;
	}
	
	public float getSaveScale() {
		return saveScale;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		matrix.getValues(m);
		imageScale = m[Matrix.MSCALE_X]; // actual image scale factor, saveScale is for reference only
		gridOffsetX = -m[Matrix.MTRANS_X];
		gridOffsetY = -m[Matrix.MTRANS_Y];
		
		//Log.d("FloorPlanView", "onDraw width="+width+", height="+height+", saveScale="+saveScale+", redundantXSpace="+redundantXSpace+", redundantYSpace="+redundantYSpace);
		//Log.d("FloorPlanView", "onDraw right="+right+", bottom="+bottom+", origWidth="+origWidth+", origHeight="+origHeight+", bmWidth="+bmWidth+", bmHeight="+bmHeight);
		//Log.d("FloorPlanView", "m[0]"+m[0]+",m[1]"+m[1]+",m[2]"+m[2]+",m[3]"+m[3]+",m[4]"+m[4]+",m[5]"+m[5]+",m[6]"+m[6]+",m[7]"+m[7]+",m[8]"+m[8]);

		if (initScale == 0) {
			// only assign for first time, for lockGridScale only
			initScale = imageScale;
			matrixWidth = gridWidth * initScale;
			trackRadius = height * 2 / 3;
		}
		float scaledGridWidth = lockGridScale ? matrixWidth : gridWidth * imageScale;
		
		if (showGrid) {
			// draw vertical
			int howManyX = (int)Math.ceil((double)bmWidth / gridWidth);
			float drawOffsetX = scaledGridWidth - (gridOffsetX % scaledGridWidth);
			if (drawOffsetX >= scaledGridWidth)
				drawOffsetX = 0;
			int beginUnitX = gridOffsetX > 0 ? (int)FloatMath.ceil(gridOffsetX / scaledGridWidth) : 0;
			for (int i=0; i<howManyX; i++) {
				canvas.drawLine(drawOffsetX + i*scaledGridWidth, 0, drawOffsetX + i*scaledGridWidth, height, gridPaint);
				gridUnitStr = "" + gridUnit * (i + beginUnitX);
				canvas.drawText(gridUnitStr, kStrOffsetX + drawOffsetX + i*scaledGridWidth, kStrOffsetY, textPaint);
			}
			
			// draw horizontal
			int howManyY = (int)Math.ceil((double)bmHeight / gridWidth);
			float drawOffsetY = scaledGridWidth - (gridOffsetY % scaledGridWidth);
			if (drawOffsetY >= scaledGridWidth)
				drawOffsetY = 0;
			int beginUnitY = gridOffsetY > 0 ? (int)FloatMath.ceil(gridOffsetY / scaledGridWidth) : 0;;
			for (int i=0; i<howManyY; i++) {
				canvas.drawLine(0, drawOffsetY + i*scaledGridWidth, width, drawOffsetY + i*scaledGridWidth, gridPaint);
				gridUnitStr = "" + gridUnit * (i + beginUnitY);
				canvas.drawText(gridUnitStr, kStrOffsetX, kStrOffsetY + drawOffsetY + i*scaledGridWidth, textPaint);
			}
		} // if (showGrid)

		// draw survey point
		if (showPoint) {
			for (int i = 0; i < mapPoint.size(); i++) {
				PointObj obj = mapPoint.valueAt(i);
				float scaledImgLeft = obj.dim.left * imageScale - gridOffsetX;
				float scaledImgTop = obj.dim.top * imageScale - gridOffsetY;
				canvas.drawBitmap(obj.curImg, scaledImgLeft, scaledImgTop, textPaint);
			}
		}
		
		if (showTrustRegion && listTrack.size() > 0) {
			// draw trust region circle for current point
			TrackObj curObj = listTrack.get(listTrack.size()-1);
				if (curObj.rad > 0)
					canvas.drawCircle(curObj.pX * imageScale - gridOffsetX, curObj.pY * imageScale - gridOffsetY, curObj.rad * scaledGridWidth / gridUnit, circlePaint);
				if (listTrack.size() > 1) {
					// draw trust region for last point
					TrackObj lastObj = listTrack.get(listTrack.size()-2);
					canvas.drawCircle(lastObj.pX * imageScale - gridOffsetX, lastObj.pY * imageScale - gridOffsetY, lastObj.rad * scaledGridWidth / gridUnit, circlePaint);
				}
		}
		
		if (showRotate && listTrack.size() > 0) {
			// if rotation is enabled, show the orientation angle at current point
			// since the selected of the survey point is based on current orientation angle
			// when display in real time
			TrackObj curObj = listTrack.get(listTrack.size()-1);
			float ovalLeft = curObj.pX * imageScale - gridOffsetX - trackRadius;
			float ovalTop = curObj.pY * imageScale - gridOffsetY - trackRadius;
			float ovalRight = curObj.pX * imageScale - gridOffsetX	+ trackRadius;
			float ovalBottom = curObj.pY * imageScale - gridOffsetY + trackRadius;
			arcOval.set(ovalLeft, ovalTop, ovalRight, ovalBottom);
			float adjStartAngle = curDir - 90 - TrackObj.halfAngle; // degree 0 is at east direction
			canvas.drawArc(arcOval, adjStartAngle, TrackObj.halfAngle * 2, true, arcPaint);
		} else if (showOrientationFilter && listTrack.size() > 1) {
			// draw direction arc, the orientation filter is measured from the
			// last point, not the new estimated point (current point)
			TrackObj lastObj = listTrack.get(listTrack.size() - 2);
			float ovalLeft = lastObj.pX * imageScale - gridOffsetX - trackRadius;
			float ovalTop = lastObj.pY * imageScale - gridOffsetY - trackRadius;
			float ovalRight = lastObj.pX * imageScale - gridOffsetX	+ trackRadius;
			float ovalBottom = lastObj.pY * imageScale - gridOffsetY + trackRadius;
			arcOval.set(ovalLeft, ovalTop, ovalRight, ovalBottom);
			float adjStartAngle = lastObj.dir - 90 - TrackObj.halfAngle; // degree 0 is at east direction
			canvas.drawArc(arcOval, adjStartAngle, TrackObj.halfAngle * 2, true, arcPaint);
		}

		// draw user track point
		if (showTrack && listTrack.size() > 0) {
			TrackObj curObj = listTrack.get(listTrack.size()-1);
			// draw track line
			for (int i=listTrack.size()-1; i > 0; i--) {
				TrackObj objFrom = listTrack.get(i);
				TrackObj objTo = listTrack.get(i-1);
				canvas.drawLine(objFrom.pX * imageScale - gridOffsetX, objFrom.pY * imageScale - gridOffsetY,
						objTo.pX * imageScale - gridOffsetX, objTo.pY * imageScale - gridOffsetY, trackPaint);
			}
			// draw track point / image
			for (int i=listTrack.size()-1; i >= 0; i--) {
				TrackObj obj = listTrack.get(i);
				canvas.drawBitmap(TrackObj.img, obj.dim.left * imageScale - gridOffsetX, obj.dim.top * imageScale - gridOffsetY, textPaint);
			}
			// draw adjusted point track
			if (curObj.pX != TrackObj.pAdjX && curObj.pX != TrackObj.pAdjY) {
				canvas.drawLine(curObj.pX * imageScale - gridOffsetX, curObj.pY * imageScale - gridOffsetY,
						TrackObj.pAdjX * imageScale - gridOffsetX, TrackObj.pAdjY * imageScale - gridOffsetY, trackAdjPaint);
				canvas.drawBitmap(TrackObj.imgAdj, TrackObj.dimAdj.left * imageScale - gridOffsetX, TrackObj.dimAdj.top * imageScale - gridOffsetY, textPaint);
			}
		}
	}

	@Override
	void performCustomClick(MotionEvent event) {
		if (mCallback == null)
			return;

		float orgClickX = (gridOffsetX + event.getX()) / imageScale;
		float orgClickY = (gridOffsetY + event.getY()) / imageScale;
		for (int i = 0; i < mapPoint.size(); i++) {
			PointObj obj = mapPoint.valueAt(i);
			if (obj.dim.contains(orgClickX, orgClickY)) {
				mCallback.OnFloorPlanClick(mapPoint.keyAt(i), orgClickX, orgClickY);
				return;
			}
		}
		mCallback.OnFloorPlanClick(0, orgClickX, orgClickY);
	}
	
	public void addPoint(int pointId, float posX, float posY, int state, Bitmap imgState, Bitmap imgState1, Bitmap imgState2) {
		mapPoint.put(pointId, new PointObj(pointId, posX, posY, state, imgState, imgState1, imgState2));
	}
	
	public void setPointState(int pointId, int state) {
		PointObj obj = mapPoint.get(pointId);
		if (obj != null && state >= 0 && state <= 2) {
			obj.state = state;
			obj.curImg = obj.imgStateArr.get(state); 
		}
	}
	
	public PointObj getPointObj(int pointId) {
		return mapPoint.get(pointId);
	}
	
	public void removePoint(int pointId) {
		PointObj obj = mapPoint.get(pointId);
		if (obj != null)
			mapPoint.remove(pointId);
	}
	
	public void showGrid(boolean show) {
		showGrid = show;
	}
	
	public void showPoint(boolean show) {
		showPoint = show;
	}
	
	public void showTrack(boolean show) {
		showTrack = show;
	}
	
	public void setTrackSize(int n) {
		trackSize = n;
	}
	
	public void setTrackPointImg(Bitmap b) {
		TrackObj.img = b;
		trackPaint.setStrokeWidth(TrackObj.getTrackWidth());
	}
	
	public void showTrustRegion(boolean show) {
		showTrustRegion = show;
	}
	
	public void setTrackAdjustPointImg(Bitmap b) {
		TrackObj.imgAdj = b;
		trackAdjPaint.setStrokeWidth(TrackObj.getAdjTrackWidth());
	}
	
	public void showOrientationFilter(boolean show) {
		showOrientationFilter = show;
	}
	
	public void setOrientationFilterAngle(float angle) {
		TrackObj.halfAngle = angle/2;
	}
	
	public void showRotate(boolean show) {
		showRotate = show;
	}
	
	public void setCurDirection(float dir) {
		curDir = dir;
	}
	
	public void addTrack(float posX, float posY, float radius, float angle) {
		if (listTrack.size() > trackSize)
			listTrack.remove(0);
		listTrack.add(new TrackObj(posX, posY, radius, angle));
	}
	
	public void addTrack(float posX, float posY, float posAdjX, float posAdjY, float radius, float angle) {
		if (listTrack.size() > trackSize)
			listTrack.remove(0);
		listTrack.add(new TrackObj(posX, posY, posAdjX, posAdjY, radius, angle));
	}
}
