package com.pasdam.universalsearch.wikipedia;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.pasdam.universalsearch.ResultItem;

public class Wikipedia extends Service {
	
	private static final String DOMAIN = "wikipedia.org";

	@Override
	public IBinder onBind(Intent arg0) {
		return addBinder;
	}
	
	private final com.pasdam.universalsearch.Service.Stub addBinder = 
			new com.pasdam.universalsearch.Service.Stub() {

				@Override
				public String[] getSuggest(String text, int maxNumberOfSuggests)
						throws RemoteException {
					try {
						String lang = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
								.getString(Wikipedia.this.getString(R.string.PREFS_LANG),
										Wikipedia.this.getString(R.string.PREFS_LANG_DEFAULT));
						StringTokenizer tokenizer = new StringTokenizer(EntityUtils.toString(
								new DefaultHttpClient()
									.execute(
											new HttpGet(
													"http://" + lang + "." + DOMAIN + "/w/api.php?action=opensearch&search=" 
													+ text + "&limit=" + maxNumberOfSuggests
											)
									).getEntity()), ",");
						tokenizer.nextToken();
						String[] suggests = new String[tokenizer.countTokens()];
						int i = 0;
						while (tokenizer.hasMoreTokens()) {
							suggests[i++] = tokenizer.nextToken()
								.replaceAll("\"", "")
								.replaceAll("\\]", "")
								.replaceAll("\\[", ""); // TODO one regex
						}
						return suggests;
					} catch (Exception e) {e.printStackTrace();}
					return new String[]{};
				}

				@Override
				public ResultItem[] search(String textToSearch,
						int maxNumberOfResults, int offset)
						throws RemoteException {
					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						
						String lang = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
								.getString(Wikipedia.this.getString(R.string.PREFS_LANG), 
										Wikipedia.this.getString(R.string.PREFS_LANG_DEFAULT));
						
						InputSource inputSrc = new InputSource();
						inputSrc.setCharacterStream(new StringReader(EntityUtils.toString(
								new DefaultHttpClient()
								.execute(
										new HttpGet("http://" + lang + "." + DOMAIN
												+ "/w/api.php?action=query&list=search&srprop=snippet&format=xml&srsearch="
												+ textToSearch + "&srlimit=" + maxNumberOfResults
												+ "&sroffset=" + offset))
											.getEntity())));
						
						Document doc = db.parse(inputSrc);
						
						NodeList nodeList = doc.getElementsByTagName("p");
						
						NamedNodeMap attributes;
						ResultItem[] resutls = new ResultItem[nodeList.getLength()];
						for (int i = 0; i < nodeList.getLength(); i++) {
							attributes = nodeList.item(i).getAttributes();
							resutls[i] = new ResultItem();
							resutls[i].title = attributes.getNamedItem("title").getNodeValue();
							resutls[i].snippet = attributes.getNamedItem("snippet").getNodeValue();
							resutls[i].uri = URI.create("http://" + lang + "." + DOMAIN + "/wiki/" + resutls[i].title.replaceAll(" ", "_"));
						}
						return resutls;
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (DOMException e) {
						e.printStackTrace();
					} catch (FactoryConfigurationError e) {
						e.printStackTrace();
					} catch (ParserConfigurationException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (SAXException e) {
						e.printStackTrace();
					}
					return null;
				}

				@Override
				public void launchPreferences() throws RemoteException {
					Intent intent = new Intent(Wikipedia.this, Preferences.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}

				@Override
				public boolean hasPreferences() throws RemoteException {
					return true;
				}
    };
}
