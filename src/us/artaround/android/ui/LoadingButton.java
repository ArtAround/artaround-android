package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class LoadingButton extends LinearLayout {

	private ImageButton btn;
	private ImageView loading;
	private View parent;
	private Animation rotate;
	private LayoutInflater inflater;

	public LoadingButton(Context context, AttributeSet attrs) {
		super(context, attrs);

		rotate = Utils.getLoadingAni(context);
		inflater = LayoutInflater.from(context);

		initUi(context, attrs);
	}

	private void initUi(Context context, AttributeSet attrs) {
		btn = (ImageButton) inflater.inflate(R.layout.icon_btn, null);
		parent = inflater.inflate(R.layout.icon_loading, null);
		loading = (ImageView) parent.findViewById(R.id.loading);

		addView(btn);
		addView(parent);

		setClickable(true);
	}

	public void setImageResource(int resId) {
		btn.setImageResource(resId);
	}

	public void showLoading(boolean isLoading) {
		btn.setVisibility(isLoading ? View.GONE : View.VISIBLE);
		parent.setVisibility(isLoading ? View.VISIBLE : View.GONE);

		if (isLoading) {
			loading.startAnimation(rotate);
		}
		else {
			loading.clearAnimation();
		}
	}

	@Override
	public void setOnClickListener(OnClickListener newListener) {
		btn.setOnClickListener(newListener);
	}
}
