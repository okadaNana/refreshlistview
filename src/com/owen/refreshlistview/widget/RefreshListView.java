package com.owen.refreshlistview.widget;

import java.sql.Date;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owen.refreshlistview.R;

/**
 * 带下拉刷新的自定义ListView
 * 
 * @author owen
 */
public class RefreshListView extends ListView implements OnScrollListener {

	private static final String TAG = "RefreshListView";
	
	/**
	 * 顶部布局
	 */
	private View headerView = null;
	
	/**
	 * 顶部布局的高度
	 */
	private int headerViewHeight = 0;

	/**
	 * ListView当前屏幕可见的第一个item的位置 
	 */
	private int firstVisibleItem = 0;
	
	/**
	 * ListView当前滚动状态
	 */
	private int scrollState = 0;
	
	/**
	 * 标识是否是在ListView的首个item出现在屏幕最顶端时手指按下
	 */
	private boolean isRemark = false;
	
	/**
	 * 当前屏幕中，ListViwe显示的第一个item是首个item时，手指按下时的Y轴坐标
	 */
	private int startY = 0;
	
	/**
	 * 当前的操作状态
	 */
	private int currentState = 0;
	
	/**
	 * 当前的操作状态为——正常状态
	 */
	private static final int NORMAL = 0;
	
	/**
	 * 当前的操作状态为——提示下拉可以刷新的状态
	 */
	private static final int PULL = 1;
	
	/**
	 * 当前的操作状态为——松开可以刷新的状态
	 */
	private static final int RELEASE = 2;
	
	/**
	 * 当前的操作状态为——正在刷新状态
	 */
	private static final int REFRESHING = 3;
	
	/**
	 * 数据刷新接口
	 */
	private onRefreshListener refreshListener = null;
	
	public RefreshListView(Context context) {
		super(context);
		
		initView(context);
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);

		initView(context);
	}

	public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		initView(context);
	}

	/**
	 * 初始化界面，添加顶部布局到ListView
	 */
	private void initView(Context context) {
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		headerView = layoutInflater.inflate(R.layout.header_layout, null);

		measureView(headerView);
		headerViewHeight = headerView.getMeasuredHeight();
		paddingTop(-headerViewHeight);

		this.addHeaderView(headerView); // 调用ListView自带的方法，添加顶部布局
		setOnScrollListener(this); // 设置滚动监听器
	}

	/**
	 * 通知view的父布局，view所占用的宽、高
	 */
	private void measureView(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params == null) {
			params = new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int width = ViewGroup.getChildMeasureSpec(0, 0, params.width);
		int height = 0;
		int tempHeight = params.height;
		if (tempHeight > 0) {
			height = MeasureSpec.makeMeasureSpec(tempHeight,
					MeasureSpec.EXACTLY);
		} else {
			height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}

		view.measure(width, height);
	}

	/**
	 * 设置顶部布局的上内边距
	 */
	private void paddingTop(int paddingTop) {
		headerView.setPadding(headerView.getPaddingLeft(),
				paddingTop,
				headerView.getPaddingRight(),
				headerView.getPaddingBottom());

		headerView.invalidate();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		this.scrollState = scrollState;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		this.firstVisibleItem = firstVisibleItem;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:  // 手指按下
			if (0 == firstVisibleItem) {
				isRemark = true;
				startY = (int) ev.getY();
			}
			break;
		case MotionEvent.ACTION_MOVE: // 手指滑动屏幕
			onMove(ev);
			break;
		case MotionEvent.ACTION_UP: // 手指抬起
			if (currentState == RELEASE) {  // 提示“松开可以刷新”，那么就刷新数据
				currentState = REFRESHING;
				
				refreshListViewByCurrentState();
				// 加载最新数据
				if (refreshListener != null) {
					refreshListener.onRefresh();
				}
			} else if (currentState == PULL) { // 提示“下拉可以刷新”，那么就复位
				currentState = NORMAL;
				isRemark = false;
				
				refreshListViewByCurrentState();
			}
			break;
		default:
			break;
		}

		return super.onTouchEvent(ev);
	}
	
	/**
	 * 在手指滑动屏幕的时候，进行的操作，比如状态、标志位的设置等等
	 */
	private void onMove(MotionEvent ev) {
		if (!isRemark) {
			/*
			 * 如果不是手指在ListView的首个item出现在顶部时按下，并滑动的操作，
			 * 说明用户不是在做下拉刷新的操作，那么就不需要做任何操作，方法直接放回
			 */
			return;
		}
		
		int currentY = (int) ev.getY();  // 当前手指移动到哪个位置
		int space = currentY - startY;   // 移动的距离是多少
		int paddingTop = space - headerViewHeight;
		
		switch (currentState) {
		case NORMAL:
			if (space > 0) {
				currentState = PULL;
				refreshListViewByCurrentState();
			}
			break;
		case PULL:
			paddingTop(paddingTop);
			
			if (space > (headerViewHeight + 30)
					&& scrollState == SCROLL_STATE_TOUCH_SCROLL) { // 移动距离大于一定高度，并且正在滚动
				currentState = RELEASE;
				refreshListViewByCurrentState();
			}
			
			break;
		case RELEASE:
			paddingTop(paddingTop);
			
			if (space < (headerViewHeight + 30)) {
				// 如果小于一定的高度
				currentState = PULL;
				refreshListViewByCurrentState();
			} else if (space <= 0) {
				currentState = NORMAL;
				isRemark = false;
				refreshListViewByCurrentState();
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * 根据当前状态，来改变界面显示
	 */
	private void refreshListViewByCurrentState() {
		TextView tip = (TextView) headerView.findViewById(R.id.tip);
		ImageView arrow = (ImageView) headerView.findViewById(R.id.arrow);
		ProgressBar progressBar = (ProgressBar) headerView.findViewById(R.id.progress);
		
		// 箭头从上到下旋转180度的动画
		RotateAnimation upToDownRotateAnim = new RotateAnimation(0, 180, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		upToDownRotateAnim.setDuration(500);
		upToDownRotateAnim.setFillAfter(true);
		
		// 箭头从下到上旋转180度的动画
		RotateAnimation downToUpRotateAnim = new RotateAnimation(180, 0, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		downToUpRotateAnim.setDuration(500);
		downToUpRotateAnim.setFillAfter(true);
		
		switch (currentState) {
		case NORMAL:
			arrow.clearAnimation();
			paddingTop(-headerViewHeight);
			break;
		case PULL:
			arrow.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			tip.setText("下拉可以刷新");
			arrow.clearAnimation();
			arrow.startAnimation(downToUpRotateAnim);
			break;
		case RELEASE:
			arrow.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			tip.setText("松开可以刷新");
			arrow.clearAnimation();
			arrow.startAnimation(upToDownRotateAnim);
			break;
		case REFRESHING:
			paddingTop(50);
			arrow.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			tip.setText("正在刷新");
			arrow.clearAnimation();
			break;
		default:
			break;
		}
	}
	
	/**
	 * 数据刷新完成。复位ListView的状态，并刷新界面
	 */
	public void refreshComplete() {
		currentState = NORMAL;
		isRemark = false;
		refreshListViewByCurrentState();
		
		TextView lastUpdateTime = (TextView) headerView.findViewById(R.id.last_update_time);
		SimpleDateFormat format = new SimpleDateFormat("yyy年MM月dd日 hh:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		String time =  format.format(date);
		lastUpdateTime.setText(time);
	}
	
	
	/**
	 * 数据刷新接口
	 * 
	 * @author owen
	 */
	public interface onRefreshListener {
		public void onRefresh();
	}

	public void setRefreshListener(onRefreshListener refreshListener) {
		this.refreshListener = refreshListener;
	}

}
