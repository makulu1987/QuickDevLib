package me.solidev.library.fragment;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import me.solidev.library.R;
import me.solidev.library.adapter.Item;
import me.solidev.library.adapter.MultiTypeAdapter;
import me.solidev.library.adapter.wrapper.HeaderAndFooterWrapper;
import me.solidev.library.recyclerview.RecycleViewDecoration;
import me.solidev.library.widget.StatusViewLayout;
import me.solidev.library.widget.pulltorefresh.PullToRefresh;

/**
 * Created by _SOLID
 * Date:2016/9/28
 * Time:15:02
 * Desc:列表基类，默认线性布局
 */

public abstract class AbListFragment<E extends Item> extends BaseFragment implements IList {

    private StatusViewLayout mStatusViewLayout;
    private PullToRefresh mPullToRefresh;
    private RecyclerView mRecyclerView;

    private HeaderAndFooterWrapper mHeaderAndFooterWrapper;

    private int mCurrentPageIndex;
    private List<E> mItems;
    private boolean mIsHaveMore;


    @Override
    protected int setLayoutResourceID() {
        return R.layout.lib_fragment_base_recyclerview;
    }

    @Override
    protected void init() {
        mCurrentPageIndex = getInitPageIndex();
        mItems = new ArrayList<>();
        mIsHaveMore = true;
        mHeaderAndFooterWrapper = new HeaderAndFooterWrapper(getAdapter());
    }

    @Override
    protected void setUpView() {
        mStatusViewLayout = $(R.id.status_view_layout);
        mPullToRefresh = $(R.id.ptr);
        mRecyclerView = $(R.id.recyclerview);
        mRecyclerView.setAdapter(mHeaderAndFooterWrapper);
        mRecyclerView.setLayoutManager(getLayoutManager());
        mRecyclerView.addItemDecoration(getItemDecoration());

        mPullToRefresh.setListener(new PullToRefresh.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }

            @Override
            public void onLoadMore() {
                loadMore();
            }
        });

        //noinspection deprecation
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {


            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (mRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                    LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int lastVisibleItemPosition = lm.findLastVisibleItemPosition();

                    if (mIsHaveMore && visibleItemCount > 0 && newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItemPosition == totalItemCount - 1) {
                        mPullToRefresh.loadMore();
                    }
                }
            }
        });
        mStatusViewLayout.setOnRetryListener(new View.OnClickListener() {//错误重试
            @Override
            public void onClick(View v) {
                mStatusViewLayout.showLoading();
                loadData(getInitPageIndex());
            }
        });
    }


    @Override
    protected void setUpData() {
        showLoading();
        loadData(getInitPageIndex());//初始加载首页数据
    }

    @Override
    public abstract void loadData(int pageIndex);

    @Override
    public void refreshData() {
        mIsHaveMore = true;
        mPullToRefresh.setPullUpEnable(true);
        mCurrentPageIndex = getInitPageIndex();
        mHeaderAndFooterWrapper.clearFootView();
        loadData(getInitPageIndex());
    }

    @Override
    public void loadMore() {
        loadData(++mCurrentPageIndex);
    }

    /**
     * 列表数据接收成功时调用（相关的实现类需要手动去调用此方法）
     *
     * @param pageIndex 当前请求的页数
     * @param items     返回的数据
     */
    protected void onDataSuccessReceived(int pageIndex, List<E> items) {
        showContent();
        if (pageIndex == getInitPageIndex() && items.size() <= 0) {//无数据
            showEmpty(getEmptyMsg());
        } else if (pageIndex == getInitPageIndex()) {//刷新
            mItems.clear();
            mItems.addAll(items);

        } else if (items != null && items.size() != 0) {//加载更多
            mItems.addAll(items);
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.scrollBy(0, 100);
                }
            }, 100);
        } else {//没有更多数据了
            mIsHaveMore = false;
            mPullToRefresh.setPullUpEnable(false);
            mHeaderAndFooterWrapper.addFootView(getNoMoreView());
            mRecyclerView.scrollToPosition(mHeaderAndFooterWrapper.getItemCount() - 1);
        }

        mHeaderAndFooterWrapper.notifyDataSetChanged();

    }


    protected List<E> getItems() {
        return mItems;
    }

    //region 根据具体的情况可选择性实现下面方法

    protected int getInitPageIndex() {
        return 1;
    }

    protected MultiTypeAdapter getAdapter() {
        return new MultiTypeAdapter(mItems);
    }

    @NonNull
    protected LinearLayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @NonNull
    protected String getEmptyMsg() {
        return "无数据";
    }

    @NonNull
    protected RecycleViewDecoration getItemDecoration() {
        return new RecycleViewDecoration(getContext(), RecyclerView.VERTICAL, 2);
    }

    private View getNoMoreView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.lib_layout_footer_view, null);
        return view;
    }

    /**
     * 添加headerView，建议在onDataSuccessReceived方法之前调用
     *
     * @param view headView
     */
    protected void addHeaderView(View view) {
        mHeaderAndFooterWrapper.addHeaderView(view);
    }

    //endregion

    //region 数据加载状态的处理
    @Override
    public void showError(Exception e) {
        mStatusViewLayout.showError(e.getMessage());
        mPullToRefresh.onFinishLoading();
    }

    @Override
    public void showEmpty(String msg) {
        mStatusViewLayout.showEmpty(msg);
        mPullToRefresh.onFinishLoading();
    }

    @Override
    public void showLoading() {
        mStatusViewLayout.showLoading();
    }

    @Override
    public void showContent() {
        mStatusViewLayout.showContent();
        mPullToRefresh.onFinishLoading();
    }
    //endregion
}
