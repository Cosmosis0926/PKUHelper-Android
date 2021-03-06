package com.pkuhelper.lib.webconnection;

import android.util.Log;

import com.pkuhelper.lib.Constants;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WebConnection {


	/**
	 * 向服务器发送请求，并且接收到返回的数据
	 *
	 * @param url    请求地址
	 * @param params 请求参数；如果参数为空使用get请求，如果不为空使用post请求
	 * @return 一个 Parameters；name 存放 HTTP code（无法连接时为-1）；如果 code==200 那么 value
	 * 存放的是返回的网页内容
	 */
	public static Parameters connect(String url, ArrayList<Parameters> params) {
		return connect(url, params, 0);
	}
	/**
	 * 向服务器发送请求，并且接收到返回的数据
	 *
	 * @param url    请求地址
	 * @param params 请求参数；如果参数为空使用get请求，如果不为空使用post请求
	 * @param encodingType 编码类型；-1表示Unknown, 0表示utf-8，1表示gbk；默认为0
	 * @return 一个 Parameters；name 存放 HTTP code（无法连接时为-1）；如果 code==200 那么 value
	 * 存放的是返回的网页内容
	 */
	public static Parameters connect(String url, ArrayList<Parameters> params, int encodingType) {
		if (params == null || params.size() == 0) {
			return connectWithGet(url, encodingType);
		}
		try {
			url = url.trim();
			boolean useProxy = whetherToUseProxy(url);
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 4000);
			HttpConnectionParams.setSoTimeout(httpParams, 13000);
//			DefaultHttpClient httpClient=new DefaultHttpClient(httpParams);
			DefaultHttpClient httpClient = HTTPSClient.getHttpClient(httpParams);
			HttpPost httpPost = new HttpPost(url);

			if (useProxy) {

				httpClient.getCredentialsProvider().setCredentials(
						new AuthScope("proxy.pku.edu.cn", 8080),
						new UsernamePasswordCredentials(Constants.username, Constants.password));
				httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
						new HttpHost("proxy.pku.edu.cn", 8080));
			}

			Log.w("URL", url);
			List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
			if (params != null) {
				for (Parameters paraItem : params) {
					String string = paraItem.value;
					if (string == null || "".equals(string)) continue;
					paramsList.add(new BasicNameValuePair(paraItem.name, paraItem.value));
					if (string.length() >= 300)
						string = string.substring(0, 299);
					Log.w(paraItem.name, string);
				}
			}
			Cookies.addCookie(httpPost);
			httpPost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
			if (paramsList.size() != 0)
				httpPost.setEntity(new UrlEncodedFormEntity(paramsList, "utf-8"));

			addHeader(httpPost, url);
			HttpResponse httpResponse = httpClient.execute(httpPost);

			Parameters parameters = new Parameters("", "");
			int returncode = httpResponse.getStatusLine().getStatusCode();
			boolean isGbk = false;

			Header header=httpResponse.getFirstHeader("Content-type");
			if (header!=null) {
				String typeString = header.getValue().toLowerCase(Locale.getDefault());
				if (typeString.contains("gbk") || typeString.contains("gb2312"))
					isGbk = true;
				else if (typeString.contains("utf8") || typeString.contains("utf-8"))
					isGbk = false;
				else if (encodingType == 1) isGbk = true;
			}
			else if (encodingType == 1) isGbk = true;

			Cookies.setCookie(httpResponse, url);

			parameters.name = returncode + "";

			if (returncode == 200) {
				BufferedReader bf;
				if (!isGbk)
					bf = new BufferedReader(
							new InputStreamReader(httpResponse.getEntity().getContent()));
				else
					bf = new BufferedReader(
							new InputStreamReader(httpResponse.getEntity().getContent(), "gbk"));
				String string = "";
				String line = bf.readLine();
				while (line != null) {
					string = string + line + "\n";
					line = bf.readLine();
				}
				string = string.trim();
				parameters.value = string;
			}
			return parameters;
		} catch (Exception e) {
			e.printStackTrace();
			return new Parameters("-1", "");
		}
	}

	private static Parameters connectWithGet(String url, int encodingType) {
		try {
			url = url.trim();
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 4000);
			HttpConnectionParams.setSoTimeout(httpParams, 13000);
			DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
			HttpGet httpGet = new HttpGet(url);
			Cookies.addCookie(httpGet);
			addHeader(httpGet, url);
			HttpResponse httpResponse = httpClient.execute(httpGet);

			Parameters parameters = new Parameters("", "");
			int returncode = httpResponse.getStatusLine().getStatusCode();

			boolean isGbk = false;

			Header header=httpResponse.getFirstHeader("Content-type");
			if (header!=null) {
				String typeString = header.getValue().toLowerCase(Locale.getDefault());
				if (typeString.contains("gbk") || typeString.contains("gb2312"))
					isGbk = true;
				else if (typeString.contains("utf8") || typeString.contains("utf-8"))
					isGbk = false;
				else if (encodingType == 1) isGbk = true;
			}
			else if (encodingType == 1) isGbk = true;

			Cookies.setCookie(httpResponse, url);

			parameters.name = returncode + "";

			if (returncode == 200) {
				BufferedReader bf;
				if (!isGbk)
					bf = new BufferedReader(
							new InputStreamReader(httpResponse.getEntity().getContent()));
				else
					bf = new BufferedReader(
							new InputStreamReader(httpResponse.getEntity().getContent(), "gbk"));
				String string = "";
				String line = bf.readLine();
				while (line != null) {
					string = string + line + "\n";
					line = bf.readLine();
				}
				parameters.value = string;
			}
			return parameters;
		} catch (Exception e) {
			return new Parameters("-1", "");
		}
	}

	/**
	 * Return a inputstream for binary request.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static InputStream connect(String url) throws IOException {
		url = url.trim();
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 4000);
		HttpConnectionParams.setSoTimeout(httpParams, 13000);
		HttpClient httpClient = new DefaultHttpClient(httpParams);
		Log.w("URL", url);
		HttpGet httpGet = new HttpGet(url);
		Cookies.addCookie(httpGet);
		addHeader(httpGet, url);
		HttpResponse httpResponse = httpClient.execute(httpGet);
		Cookies.setCookie(httpResponse, url);
		return httpResponse.getEntity().getContent();
	}

	public static boolean checkIfConnectedToNoFree() {
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress("www.sublimetext.com", 80), 1000);
			boolean connected = socket.isConnected();
			socket.close();
			return connected;
		} catch (Exception e) {
			return false;
		}
	}

	public static int checkIfInSchool() {
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 1000);
			HttpConnectionParams.setSoTimeout(httpParams, 2000);
			HttpClient httpClient = new DefaultHttpClient(httpParams);

			HttpGet httpGet = new HttpGet(Constants.domain + "/services/pkuhelper/isIPInPKU.php");
			HttpResponse httpResponse = httpClient.execute(httpGet);
			InputStream inputStream = httpResponse.getEntity().getContent();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String line = bufferedReader.readLine();
			return Integer.parseInt(line);
		} catch (IOException e) {
			return -1;
		}
	}

	/**
	 * 什么时候使用校内代理
	 *
	 * @param url
	 * @return
	 */
	private static boolean whetherToUseProxy(String url) {
		// 未登录：不用
		//if (!Constants.isValidLogin()) return false;
		// 已经是校内：不用
		//if (Constants.inSchool) return false;
		// 如果是树洞请求访问，使用
		//if (url.startsWith("http://pkuhole.sinaapp.com"))
		//	return true;

		// 默认不用
		return false;
	}

	private static void addHeader(HttpRequestBase httpRequestBase, String url) {
		if (url.startsWith("http://dean.pku.edu.cn/student/")) {
			httpRequestBase.addHeader("Referer", "http://dean.pku.edu.cn/student/");
		}
		if (url.startsWith("http://elective.pku.edu.cn/")) {
			httpRequestBase.addHeader("Referer", "http://elective.pku.edu.cn/elective2008/edu/pku/stu/elective/controller/electiveWork/showResults.do");
		}
		httpRequestBase.addHeader("Upgrade-Insecure-Requests","1");
		httpRequestBase.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36");
		httpRequestBase.addHeader("Platform", "Android");
		httpRequestBase.addHeader("Version", Constants.version);
		httpRequestBase.addHeader("User-token", Constants.user_token);
		/*
		else if (url.startsWith("http://dean.pku.edu.cn/student/authenticate.php")) {
			httpRequestBase.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36");
			httpRequestBase.addHeader("Referer","http://dean.pku.edu.cn/student/");
			httpRequestBase.addHeader("Origin","http://dean.pku.edu.cn");
		}
		*/
	}


}