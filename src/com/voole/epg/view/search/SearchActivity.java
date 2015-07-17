package com.voole.epg.view.search;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.voole.epg.Config;
import com.voole.epg.R;
import com.voole.epg.base.BaseActivity;
import com.voole.epg.base.common.LogoView;
import com.voole.epg.corelib.model.auth.AuthManager;
import com.voole.epg.corelib.model.movies.Detail;
import com.voole.epg.corelib.model.movies.Film;
import com.voole.epg.corelib.model.movies.FilmAndPageInfo;
import com.voole.epg.corelib.model.movies.MovieManager;
import com.voole.epg.corelib.model.movies.SearchManager;
import com.voole.epg.corelib.model.movies.SearchManager.SearchType;
import com.voole.epg.corelib.model.utils.LogUtil;
import com.voole.epg.view.movies.detail.MovieDetailActivity;
import com.voole.epg.view.movies.movie.MovieViewListener;
import com.voole.epg.view.search.KeyboardView.KeyboardListener;
import com.voole.epg.view.search.SearchTitleBarView.SearchBarListener;

public class SearchActivity extends BaseActivity {
	private static final int DOWNLOAD_UI = 10001;
	private static final int SEARCH_SUCCESS = 10002;
	private static final int SEARCH_FAIL = 10003;
	private static final int GOTO_PAGE_FAIL = 10004;
	private static final int HOTFILM_SUCCESS = 10005;
	private static final int HOTFILM_FAIL = 10006;
	private static final int DETAIL_SUCCESS = 10007;
	private static final int DETAIL_FAIL = 10008;

	private LogoView logoView = null;
	private SearchView searchView = null;

	private List<Film> hotFilms = null;
	private FilmAndPageInfo searchResult = null;
	private String keyword = "";
	private View view;

	@Override
	protected void doHandleMessage(int what, Object obj) {
		switch (what) {
		case DOWNLOAD_UI:
			init();
			if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
				keyword = getIntent().getStringExtra(
						android.app.SearchManager.QUERY);
				LogUtil.d("search--action-->" + Intent.ACTION_SEARCH
						+ "---keyword--->" + keyword);
				searchMovie(keyword, 1, SearchType.Keyword, false);
			} else if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
				String path = getIntent().getData().toString();
				keyword = getIntent().getStringExtra(
						android.app.SearchManager.QUERY);
				LogUtil.d("search---action--->" + Intent.ACTION_VIEW
						+ "--path-->" + path + "--keyword->" + keyword);
				if (path != null && !path.equals("")) {
					getDetail(path);
				} else {
					getHotFilms();
				}
			} else if (!TextUtils.isEmpty(getIntent().getStringExtra("type"))
					&& !TextUtils
							.isEmpty(getIntent().getStringExtra("keyword"))) {
				String type = getIntent().getStringExtra("type");
				SearchType searchType = null;
				if ("jianpin".equals(type)) {
					searchType = SearchType.JianPin;
				} else if ("quanpin".equals(type)) {
					searchType = SearchType.QuanPin;
				} else if ("keyword".equals(type)) {
					searchType = SearchType.Keyword;
				}
				searchMovie(getIntent().getStringExtra("keyword"), 1,
						searchType, false);
			} else {
				getHotFilms();
			}
			break;
		case HOTFILM_SUCCESS:
			searchView.setHotViewData(hotFilms);
			searchView.setFocusedToA();
			break;
		case HOTFILM_FAIL:
			Toast.makeText(SearchActivity.this, "没有热播影片", Toast.LENGTH_LONG)
					.show();
			break;
		case SEARCH_SUCCESS:
			searchView.setSearchResultData(searchResult,
					searchResult.getPageIndex(), searchResult.getPageCount());
			break;
		case SEARCH_FAIL:
			searchView.showNoSearchResultToast(keyword);
			break;
		case GOTO_PAGE_FAIL:
			Toast.makeText(SearchActivity.this, "抱歉，翻页失败!", Toast.LENGTH_SHORT)
					.show();
			break;
		case DETAIL_SUCCESS:
			Film film = (Film) obj;
			gotoDetail(film);
			SearchActivity.this.finish();
			break;
		case DETAIL_FAIL:
			Toast.makeText(SearchActivity.this, "查找影片出错！", Toast.LENGTH_LONG)
					.show();
			getHotFilms();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.cs_search);
		// init();
		LogUtil.d("intent---action--->" + getIntent().getAction());
		downloadUI();
	}

	private void downloadUI() {
		showDialog();
		new Thread(new Runnable() {
			@Override
			public void run() {
				sendMessage(DOWNLOAD_UI);
			}
		}).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	private void init() {
		showDialog();
		view = View.inflate(getApplicationContext(), R.layout.cs_search, null);
		setContentView(view);
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
		mainLayout.setBackgroundResource(R.drawable.cs_recommend_bg);
		logoView = (LogoView) findViewById(R.id.logo);
		logoView.setChannelName("搜索", false);
		logoView.hideSearchLabel();
		searchView = (SearchView) findViewById(R.id.middle);
		addListener();
	}

	private void addListener() {
		searchView.setSearchBarListener(new SearchBarListener() {
			@Override
			public void onSelected(String string) {
				SearchActivity.this.keyword = string;
				searchMovie(keyword, 1, SearchType.Keyword, false);
			}

			@Override
			public void onSearch(String keyword) {
				// SearchActivity.this.keyword = keyword;
				// searchMovie(keyword, 1, SearchType.JianPin,false);
			}

			@Override
			public void onInput(String string) {
				/*
				 * List<String> list = new ArrayList<String>();
				 * list.add(string); list.add(string); list.add(string);
				 * list.add(string); list.add(string);
				 * searchView.showRelateList(list); list.clear(); list = null;
				 */
			}
		});

		searchView.setSearchResultListener(new MovieViewListener() {

			@Override
			public void onPlay(Film item) {

			}

			@Override
			public void onItemSelected(Film item, int index) {
				gotoDetail(item);
			}

			@Override
			public void onGotoPage(int pageNo) {
				searchMovie(keyword, pageNo, SearchType.JianPin, true);
			}
		});

		searchView.setHotViewListener(new MovieViewListener() {

			@Override
			public void onPlay(Film item) {

			}

			@Override
			public void onItemSelected(Film item, int index) {
				// gotoDetail(item);
				Intent intent = new Intent();
				// intent.putExtra("intentMid", item.getMid());
				intent.putExtra("film", item);
				intent.setClass(SearchActivity.this, MovieDetailActivity.class);
				startActivity(intent);
			}

			@Override
			public void onGotoPage(int pageNo) {

			}
		});

		searchView.setKeyboardViewListener(new KeyboardListener() {
			@Override
			public void onKey(String text) {
				searchView.addInputText(text);
			}

			@Override
			public void onDelete() {
				searchView.deleteInputText();
			}

			@Override
			public void onClear() {
				searchView.clearInputText();
				keyword = "";
			}

			@Override
			public void onSearch() {
				SearchActivity.this.keyword = searchView.getSearchKeyword();
				if (keyword != null && !"".equals(keyword)) {
					searchMovie(keyword, 1, SearchType.JianPin, false);
				} else {
					Toast.makeText(SearchActivity.this, "请输入关键词",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

	}

	private void getHotFilms() {
		showDialog();
		new Thread(new Runnable() {
			@Override
			public void run() {
				hotFilms = MovieManager.GetInstance().getTopViewMovies();
				if (hotFilms != null && hotFilms.size() > 0) {
					sendMessage(HOTFILM_SUCCESS);
				} else {
					sendMessage(HOTFILM_FAIL);
				}
			}
		}).start();
	}

	private void searchMovie(final String string, final int currentPage,
			final SearchType searchType, final boolean isGotoPage) {
		showDialog();
		new Thread(new Runnable() {
			@Override
			public void run() {
				searchResult = SearchManager.GetInstance().searchIndex(string,
						currentPage, 7, searchType, "");
				if (searchResult != null && searchResult.getFilmList() != null
						&& searchResult.getFilmList().size() > 0) {
					sendMessage(SEARCH_SUCCESS);
				} else {
					if (isGotoPage) {
						sendMessage(GOTO_PAGE_FAIL);
					} else {
						sendMessage(SEARCH_FAIL);
					}
				}
			}
		}).start();
	}

	private void getDetail(final String url) {
		showDialog();
		new Thread() {
			public void run() {
				if (!AuthManager.GetInstance().isAuthRunning()) {
					AuthManager.GetInstance().startAuth();
				}
				Detail dm = MovieManager.GetInstance().getDetailFromSrcUrl(url);
				Message message = Message.obtain();
				Film film = dm.getFilm();
				if (film != null) {
					message.what = DETAIL_SUCCESS;
					message.obj = film;
				} else {
					message.what = DETAIL_FAIL;
				}
				handler.sendMessage(message);
			};
		}.start();
	}

	private void gotoDetail(Film film) {
		Intent intent = new Intent();
		intent.putExtra("intentMid", film.getMid());
		// intent.putExtra("film", film);
		intent.setClass(SearchActivity.this, MovieDetailActivity.class);
		startActivity(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}
}
